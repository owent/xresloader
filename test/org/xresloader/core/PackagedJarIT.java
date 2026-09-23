package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import javax.script.ScriptEngineManager;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PackagedJarIT {
    @TempDir
    Path temporaryDirectory;

    private Path packagedJar() {
        return Path.of(System.getProperty("xresloader.jar.path")).toAbsolutePath();
    }

    private String runJava(String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        command.addAll(List.of(arguments));
        Path output = Files.createTempFile(temporaryDirectory, "java-", ".log");
        Process process = new ProcessBuilder(command).directory(temporaryDirectory.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(60, TimeUnit.SECONDS), "Packaged JAR process timed out");
            String log = Files.readString(output, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), log);
            return log;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void executableJarPreservesServicesAndMultiReleaseImplementations() throws Exception {
        Path originalJar = packagedJar().resolveSibling("original-" + packagedJar().getFileName());
        try (JarFile jar = new JarFile(originalJar.toFile())) {
            assertNotNull(jar.getJarEntry("org/xresloader/core/Main.class"));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("org/apache/poi/")),
                    "Repeated packaging must recreate the unshaded input JAR");
        }
        try (JarFile jar = new JarFile(packagedJar().toFile())) {
            assertEquals("org.xresloader.core.Main", jar.getManifest().getMainAttributes().getValue("Main-Class"));
            assertEquals("true", jar.getManifest().getMainAttributes().getValue("Multi-Release"));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().equals("module-info.class")
                    || entry.getName().matches("META-INF/versions/[^/]+/module-info\\.class")));
            assertTrue(jar.stream().anyMatch(entry -> entry.getName().startsWith("META-INF/versions/")
                    && entry.getName().endsWith(".class")));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("edu/umd/cs/findbugs/")));
            var providers = jar.getJarEntry("META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider");
            assertNotNull(providers);
            try (var input = jar.getInputStream(providers)) {
                String contents = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(contents.contains("org.apache.poi.hssf.usermodel.HSSFWorkbookFactory"), contents);
                assertTrue(contents.contains("org.apache.poi.xssf.usermodel.XSSFWorkbookFactory"), contents);
            }
        }
        String help = runJava("-jar", packagedJar().toString(), "--help");
        assertTrue(help.contains("--output-type"), help);
        assertTrue(help.contains("--help"), help);
    }

    @Test
    public void excelAndJavascriptWorkWithOnlyThePackagedDependencies() throws Exception {
        Path testClasses = Path.of(PackagedJarIT.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String classpath = packagedJar() + File.pathSeparator + testClasses;
        String output = runJava("-cp", classpath, Smoke.class.getName());
        assertTrue(output.contains("Packaged Excel and JavaScript services OK"), output);
    }

    // The child JVM receives just the shaded JAR and these test classes, never Maven's dependency classpath.
    public static final class Smoke {
        public static void main(String[] arguments) throws Exception {
            roundTrip(new HSSFWorkbook());
            roundTrip(new XSSFWorkbook());
            var engine = new ScriptEngineManager().getEngineByName("javascript");
            if (engine == null || ((Number) engine.eval("21 * 2")).intValue() != 42) {
                throw new IllegalStateException("JavaScript engine discovery or execution failed");
            }
            System.out.println("Packaged Excel and JavaScript services OK");
        }

        private static void roundTrip(Workbook workbook) throws Exception {
            try (workbook; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (workbook instanceof HSSFWorkbook hssfWorkbook) {
                    hssfWorkbook.createInformationProperties();
                    hssfWorkbook.getSummaryInformation().getFirstSection().setCodepage(CodePageUtil.CP_UNICODE);
                    hssfWorkbook.getDocumentSummaryInformation().getFirstSection().setCodepage(CodePageUtil.CP_UNICODE);
                }
                workbook.createSheet("test").createRow(0).createCell(0).setCellValue("xresloader");
                workbook.write(output);
                try (Workbook loaded = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
                    if (!workbook.getClass().equals(loaded.getClass())
                            || !"xresloader".equals(loaded.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())) {
                        throw new IllegalStateException("Excel workbook round trip failed");
                    }
                }
            }
        }
    }
}
