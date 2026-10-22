package org.xresloader.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.engine.ExcelEngine.CustomDataRowIndex;
import org.xresloader.core.engine.ExcelEngine.CustomDataTableIndex;

@Timeout(30)
class ExcelTableCacheTest {
    @TempDir Path directory;
    private int previousRowBudget;

    @BeforeEach
    void saveOptions() {
        previousRowBudget = ProgramOptions.getInstance().dataSourceLruCacheRows;
    }

    @AfterEach
    void cleanup() {
        ProgramOptions.getInstance().dataSourceLruCacheRows = previousRowBudget;
    }

    private String file(String name) {
        return directory.resolve(name).toString();
    }

    private static CustomDataTableIndex table(String file, String sheet) {
        var table = new CustomDataTableIndex(file, sheet);
        var row = new CustomDataRowIndex(1, table);
        row.getColumns().add(sheet);
        table.addRow(row);
        return table;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Loader did not reach the expected synchronization point");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    // Inspect under the production lock so tests can wait for actual contenders, not scheduler timing.
    private static int pendingUsers(String file) throws Exception {
        var lockField = ExcelEngine.class.getDeclaredField("tableCacheLock");
        lockField.setAccessible(true);
        var indexLock = (ReentrantReadWriteLock) lockField.get(null);
        indexLock.readLock().lock();
        try {
            var loadsField = ExcelEngine.class.getDeclaredField("pendingTableLoads");
            loadsField.setAccessible(true);
            var pending = ((Map<?, ?>) loadsField.get(null)).get(file);
            if (pending == null) {
                return 0;
            }
            var usersField = pending.getClass().getDeclaredField("users");
            usersField.setAccessible(true);
            return usersField.getInt(pending);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    private static void awaitUsers(String file, int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (pendingUsers(file) != expected && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertEquals(expected, pendingUsers(file));
    }

    @Test
    void differentFilesBuildTogetherAndCachedSheetsRemainReadable() throws Exception {
        String first = file("first.xlsx");
        String second = file("second.xls");
        var warm = ExcelEngine.getOrBuildStreamTableIndex(first, "warm", () -> table(first, "warm"));
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(3);
        try {
            var builds = new ArrayList<Future<CustomDataTableIndex>>();
            for (String path : new String[] {first, second}) {
                builds.add(pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(path, "cold", () -> {
                    entered.countDown();
                    await(release);
                    return table(path, "cold");
                })));
            }
            await(entered); // Fails if a global lock covers either loader.
            assertSame(warm, pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(first, "warm",
                    () -> { throw new AssertionError("Cache hit invoked loader"); })).get(10, TimeUnit.SECONDS));
            release.countDown();
            for (var build : builds) {
                assertEquals("cold", build.get(10, TimeUnit.SECONDS).getRow(1).getCellValue(0));
            }
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(0, pendingUsers(first));
        assertEquals(0, pendingUsers(second));
    }

    @Test
    void evictionDoesNotReplaceALockWithWaitingBuilders() throws Exception {
        String path = file("shared.xlsx");
        var warm = ExcelEngine.getOrBuildStreamTableIndex(path, "warm", () -> table(path, "warm"));
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var loads = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var builds = new ArrayList<Future<CustomDataTableIndex>>();
            for (int i = 0; i < 8; ++i) {
                builds.add(pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(path, "cold", () -> {
                    loads.incrementAndGet();
                    entered.countDown();
                    await(release);
                    return table(path, "cold");
                })));
                if (i == 0) {
                    await(entered);
                    ProgramOptions.getInstance().dataSourceLruCacheRows = 3;
                    String other = file("eviction.xlsx");
                    ExcelEngine.getOrBuildStreamTableIndex(other, "other", () -> table(other, "other"));
                }
            }
            awaitUsers(path, 8);
            assertEquals(1, loads.get());
            release.countDown();
            var published = builds.get(0).get(10, TimeUnit.SECONDS);
            for (var build : builds) {
                assertSame(published, build.get(10, TimeUnit.SECONDS));
            }
            assertEquals(1, loads.get());
            assertEquals("warm", warm.getRow(1).getCellValue(0));
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(0, pendingUsers(path));
    }

    @Test
    void differentSheetsOfOneFileSerializeTheirBuilds() throws Exception {
        String path = file("shared.xlsx");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondLoads = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(path, "first", () -> {
                entered.countDown();
                await(release);
                return table(path, "first");
            }));
            await(entered);
            var second = pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(path, "second", () -> {
                secondLoads.incrementAndGet();
                return table(path, "second");
            }));
            awaitUsers(path, 2);
            assertEquals(0, secondLoads.get());
            release.countDown();
            assertEquals("first", first.get(10, TimeUnit.SECONDS).getRow(1).getCellValue(0));
            assertEquals("second", second.get(10, TimeUnit.SECONDS).getRow(1).getCellValue(0));
            assertEquals(1, secondLoads.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(0, pendingUsers(path));
    }

    @Test
    void failedAndNullLoadsReleaseTheirLockAndAllowWaitingRetries() throws Exception {
        String path = file("retry.xlsx");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> assertThrows(IllegalStateException.class,
                    () -> ExcelEngine.getOrBuildStreamTableIndex(path, "sheet", () -> {
                        entered.countDown();
                        await(release);
                        throw new IllegalStateException("failed before publication");
                    })));
            await(entered);
            var retry = pool.submit(() -> ExcelEngine.getOrBuildStreamTableIndex(path, "sheet", () -> null));
            awaitUsers(path, 2);
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            assertNull(retry.get(10, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(0, pendingUsers(path));
        assertEquals("sheet", ExcelEngine.getOrBuildStreamTableIndex(path, "sheet", () -> table(path, "sheet"))
                .getRow(1).getCellValue(0));
        assertEquals(0, pendingUsers(path));
    }

    @Test
    void fileAndSheetNamesCannotCollideAtASeparator() {
        // '|' is a valid file-name character on Unix and a valid sheet-name character.
        String second = file("book.xlsx");
        String first = second + "|part";
        var a = ExcelEngine.getOrBuildStreamTableIndex(first, "sheet", () -> table(first, "sheet"));
        var b = ExcelEngine.getOrBuildStreamTableIndex(second, "part|sheet", () -> table(second, "part|sheet"));
        assertNotSame(a, b);
        assertEquals(first, a.getFilePath());
        assertEquals(second, b.getFilePath());
    }

    @Test
    void oversizedTablesDoNotLeavePermanentLocksOrChangeTheRowBudget() throws Exception {
        String path = file("oversized.xlsx");
        ProgramOptions.getInstance().dataSourceLruCacheRows = 2;
        var first = ExcelEngine.getOrBuildStreamTableIndex(path, "sheet", () -> table(path, "sheet"));
        assertEquals(0, pendingUsers(path));
        var second = ExcelEngine.getOrBuildStreamTableIndex(path, "sheet", () -> table(path, "sheet"));
        assertNotSame(first, second);
        assertEquals("sheet", first.getRow(1).getCellValue(0));
        assertEquals(0, pendingUsers(path));
    }
}
