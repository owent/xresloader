package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xresloader.core.data.src.DataSrcExcel;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.dst.DataDstWriterNode;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.InputFileCache;
import org.xresloader.core.engine.SharedCache;
import org.xresloader.core.scheme.SchemeConf;

class ParallelStateTest {
    @TempDir Path directory;

    @AfterEach
    void cleanup() {
        ProgramOptions.clearThreadState();
        SchemeConf.clearThreadState();
        DataSrcImpl.clearThreadState();
    }

    @Test
    void parallelismRequiresPositiveIntAndResetsBetweenTasks() {
        var options = ProgramOptions.getInstance();
        for (String invalid : List.of("0", "-1", "four", "1.5", "2147483648")) {
            assertTrue(options.init(new String[] {"--parallelism", invalid}) < 0, invalid);
        }
        assertEquals(1, options.init(new String[] {"--stdin", "--parallelism", "4"}));
        assertEquals(4, options.parallelism);
        assertFalse(options.automaticParallelism);
        assertEquals(1, options.init(new String[] {"--stdin"}));
        assertTrue(options.automaticParallelism);
        assertEquals(ProgramOptions.autoParallelism(Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory()), options.parallelism);
        options.dataSourceMappingType = ProgramOptions.DataSourceMappingType.SHA256;
        String expected = options.getDataSourceMapping("input", false);
        options.dataSourceMappingType = null;
        assertEquals(expected, options.getDataSourceMapping("input", false));
    }

    @Test
    void automaticParallelismIsDefaultAndExplicitValuesOverrideIt() {
        long mib = 1024L * 1024;
        assertEquals(1, ProgramOptions.autoParallelism(1, 2048 * mib));
        assertEquals(2, ProgramOptions.autoParallelism(16, 512 * mib));
        assertEquals(3, ProgramOptions.autoParallelism(5, 2048 * mib));
        assertEquals(8, ProgramOptions.autoParallelism(16, 2048 * mib));
        assertEquals(8, ProgramOptions.autoParallelism(128, 32768 * mib));
        assertEquals(1, ProgramOptions.autoParallelism(16, 128 * mib));
        var options = ProgramOptions.getInstance();
        assertEquals(1, options.init(new String[] {"--stdin", "--parallelism", "auto"}));
        assertTrue(options.automaticParallelism);
        assertTrue(options.parallelism >= 1 && options.parallelism <= 8);
        int automatic = options.parallelism;
        options.init(new String[] {"--stdin", "--parallelism", "1"});
        assertFalse(options.automaticParallelism);
        assertEquals(1, options.parallelism);
        options.init(new String[] {"--stdin"});
        assertTrue(options.automaticParallelism);
        assertEquals(automatic, options.parallelism);
    }

    @Test
    void singletonInterfacesAndDefaultVersionAreIsolatedAcrossWorkers() throws Exception {
        ProgramOptions.getInstance().init(new String[] {"--stdin", "--data-version", "batch"});
        var barrier = new CyclicBarrier(4);
        var pool = Executors.newFixedThreadPool(4);
        try {
            var jobs = new ArrayList<Callable<String>>();
            for (int index = 0; index < 4; ++index) {
                int number = index;
                ProgramOptions options = ProgramOptions.nextTask();
                options.init(new String[] {"-f", "unused.pb", "-o", "dir-" + number,
                        "--data-source-mapping-mode", "sha256", "--data-source-mapping-seed", "seed-" + number});
                jobs.add(() -> {
                    ProgramOptions.install(options);
                    SchemeConf scheme = SchemeConf.getInstance();
                    scheme.setCallbackScriptPath("script-" + number);
                    var source = DataSrcImpl.create(DataSrcExcel.class);
                    barrier.await(10, TimeUnit.SECONDS);
                    assertSame(options, ProgramOptions.getInstance());
                    assertSame(source, DataSrcImpl.getOurInstance());
                    assertEquals("script-" + number, SchemeConf.getInstance().getCallbackScriptPath());
                    assertEquals("dir-" + number, ProgramOptions.getInstance().outputDirectory);
                    assertEquals("batch", options.getDataVersion());
                    return options.getDataSourceMapping("shared-input", false);
                });
            }
            var hashes = new java.util.HashSet<String>();
            for (var result : pool.invokeAll(jobs)) {
                hashes.add(result.get());
            }
            assertEquals(4, hashes.size(), "Mapping seed must belong to each task");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void sharedCachePublishesOnlyOneCompleteValueAndRetriesFailedLoads() throws Exception {
        var cache = new SharedCache<String, List<Integer>>();
        var loads = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var jobs = new ArrayList<Callable<List<Integer>>>();
            for (int i = 0; i < 32; ++i) {
                jobs.add(() -> cache.getOrLoad("input", () -> {
                    loads.incrementAndGet();
                    return List.of(1, 2, 3);
                }));
            }
            var results = pool.invokeAll(jobs);
            for (var result : results) {
                assertSame(results.get(0).get(), result.get());
                assertEquals(List.of(1, 2, 3), result.get());
            }
            assertEquals(1, loads.get());
            assertThrows(java.io.IOException.class,
                    () -> cache.getOrLoad("retry", () -> { throw new java.io.IOException("unavailable"); }));
            assertEquals(List.of(4), cache.getOrLoad("retry", () -> List.of(4)));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void cacheHitsActuallyHoldConcurrentReadLocks() throws Exception {
        class Key {
            volatile CountDownLatch readers;
            @Override public int hashCode() {
                CountDownLatch latch = readers;
                if (latch != null) {
                    latch.countDown();
                    try {
                        assertTrue(latch.await(10, TimeUnit.SECONDS), "Cache hits must not take an exclusive lock");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                }
                return 1;
            }
            @Override public boolean equals(Object other) { return this == other; }
        }
        Key key = new Key();
        var cache = new SharedCache<Key, String>();
        cache.getOrLoad(key, () -> "loaded");
        key.readers = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> cache.getOrLoad(key, () -> "wrong"));
            var second = pool.submit(() -> cache.getOrLoad(key, () -> "wrong"));
            assertEquals("loaded", first.get(15, TimeUnit.SECONDS));
            assertEquals("loaded", second.get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void sharedFileBytesHaveIndependentReadCursors() throws Exception {
        Path path = directory.resolve("input.txt");
        Files.writeString(path, "abc", StandardCharsets.UTF_8);
        try (var first = InputFileCache.open(path.toFile()); var second = InputFileCache.open(path.toFile())) {
            assertEquals('a', first.read());
            assertEquals('b', first.read());
            assertEquals('a', second.read());
            assertEquals("bc", new String(second.readAllBytes(), StandardCharsets.UTF_8));
        }
        Files.writeString(path, "updated", StandardCharsets.UTF_8);
        try (var input = InputFileCache.open(path.toFile())) {
            assertEquals("updated", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void primitiveDescriptorExtensionsAreTaskLocalAndCanBeReleased() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var barrier = new CyclicBarrier(2);
        try {
            var jobs = new ArrayList<Callable<Void>>();
            for (String name : List.of("first", "second")) {
                jobs.add(() -> {
                    var descriptor = DataDstWriterNode.getDefaultMessageDescriptor(DataDstWriterNode.JAVA_TYPE.STRING, null, null);
                    descriptor.mutableExtension().description = name;
                    barrier.await(10, TimeUnit.SECONDS);
                    assertEquals(name, descriptor.mutableExtension().description);
                    DataDstWriterNode.clearThreadState();
                    assertNull(DataDstWriterNode.getDefaultMessageDescriptor(DataDstWriterNode.JAVA_TYPE.STRING, null, null)
                            .mutableExtension().description);
                    return null;
                });
            }
            for (var result : pool.invokeAll(jobs)) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void streamTableCacheIsSharedAndEvictionDoesNotInvalidateActiveReaders() throws Exception {
        Path path = directory.resolve("sheets.xlsx");
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            for (String name : List.of("first", "second", "third")) {
                var sheet = workbook.createSheet(name);
                sheet.createRow(0).createCell(0).setCellValue(name);
                sheet.createRow(1).createCell(0).setCellValue(42);
            }
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        var pool = Executors.newFixedThreadPool(4);
        try {
            var jobs = new ArrayList<Callable<ExcelEngine.CustomDataTableIndex>>();
            for (int i = 0; i < 4; ++i) {
                jobs.add(() -> ExcelEngine.openStreamTableIndex(path.toFile(), "first"));
            }
            var results = pool.invokeAll(jobs);
            var first = results.get(0).get();
            for (var result : results) {
                assertSame(first, result.get(), "Cold input must be parsed and published once");
            }
            ProgramOptions.getInstance().dataSourceLruCacheRows = 3;
            for (String name : List.of("second", "third")) {
                assertEquals(name, ExcelEngine.openStreamTableIndex(path.toFile(), name).getRow(0).getCellValue(0));
                assertEquals("first", first.getRow(0).getCellValue(0));
                assertEquals("42", first.getRow(1).getCellValue(0));
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
