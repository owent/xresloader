package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

class ParallelConversionIT {
    @TempDir Path directory;

    private void fixtures() throws Exception {
        for (String suffix : List.of("xls", "xlsx")) {
            try (Workbook workbook = suffix.equals("xls") ? new HSSFWorkbook() : new XSSFWorkbook()) {
                var sheet = workbook.createSheet("data");
                var header = sheet.createRow(0);
                header.createCell(0).setCellValue("value");
                header.createCell(1).setCellValue("label");
                var row = sheet.createRow(1);
                row.createCell(0).setCellFormula("1+1");
                row.createCell(1).setCellValue("original");
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
                try (var output = Files.newOutputStream(directory.resolve("input." + suffix))) {
                    workbook.write(output);
                }
            }
        }
        for (Type type : List.of(Type.TYPE_INT32, Type.TYPE_STRING)) {
            var message = DescriptorProtos.DescriptorProto.newBuilder().setName("Record")
                    .addField(DescriptorProtos.FieldDescriptorProto.newBuilder().setName("value").setNumber(1)
                            .setType(type).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL))
                    .addField(DescriptorProtos.FieldDescriptorProto.newBuilder().setName("label").setNumber(2)
                            .setType(Type.TYPE_STRING).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL));
            // Same .proto and message names, different descriptors: global bound state would contaminate tasks.
            var file = DescriptorProtos.FileDescriptorProto.newBuilder().setName("record.proto")
                    .setSyntax("proto2").addMessageType(message);
            Files.write(directory.resolve(type.name() + ".pb"),
                    DescriptorProtos.FileDescriptorSet.newBuilder().addFile(file).build().toByteArray());
        }
        Files.writeString(directory.resolve("callback.js"), """
                var Probe = Java.type('org.xresloader.core.ParallelConversionIT$Probe');
                var calls = 0;
                function initDataSource() { Probe.awaitWorkers(); }
                function currentMessageCallback(message, descriptor) {
                    message.put('label', String(gSchemeConf.getOutputFile()) + ':' + (++calls));
                    return true;
                }
                """, StandardCharsets.UTF_8);
    }

    private String task(int index, String output, boolean callback) {
        String type = index % 2 == 0 ? "TYPE_INT32" : "TYPE_STRING";
        String extension = index % 4 < 2 ? "xls" : "xlsx";
        return "-t json -f " + type + ".pb -m DataSource=input." + extension + "|data|2,1"
                + " -m ProtoName=Record -m KeyRow=1 -m OutputFile=" + output
                + (callback ? " -m CallbackScript=callback.js" : "")
                + (index % 3 == 0 ? " --enable-excel-formular" : " --disable-excel-formular")
                + (index % 2 == 0 ? " --data-version task-" + index : "")
                + " --data-source-mapping-mode sha256 --data-source-mapping-seed seed-" + index
                + " --data-source-mapping-file mapping-" + index % 2 + ".txt";
    }

    private void runBatch(int parallelism, String commands, int expectedExit, boolean barrier) throws Exception {
        Path jar = Path.of(System.getProperty("xresloader.jar.path")).toAbsolutePath();
        Path testClasses = Path.of(ParallelConversionIT.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(), "-Xms32m", "-Xmx512m",
                "-Dxresloader.version=test", "-Dxresloader.test.barrier=" + barrier,
                "-cp", jar + File.pathSeparator + testClasses, Probe.class.getName(),
                "--stdin", "--data-version", "batch"));
        if (parallelism > 0) {
            command.addAll(List.of("--parallelism", Integer.toString(parallelism)));
        } else {
            // With four visible CPUs and a 512 MiB heap, the automatic default must run two workers.
            command.addAll(1, List.of("-XX:ActiveProcessorCount=4", "-Dxresloader.test.barrier.parties=2"));
        }
        Path log = directory.resolve("run-" + parallelism + ".log");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            try (var input = process.getOutputStream()) {
                input.write(commands.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(90, TimeUnit.SECONDS), "Conversion timed out: " + log);
            assertEquals(expectedExit, process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void omittedParallelismRunsAutomaticWorkersAndMatchesSerialBytes() throws Exception {
        fixtures();
        var tasks = new ArrayList<String>();
        for (int i = 0; i < 4; ++i) {
            tasks.add(task(i, "default-" + i + ".json", true));
        }
        String commands = String.join("\n", tasks) + "\n";
        runBatch(1, commands, 0, false);
        var before = new ArrayList<byte[]>();
        for (int i = 0; i < 4; ++i) {
            before.add(Files.readAllBytes(directory.resolve("default-" + i + ".json")));
        }
        // Each pair of callbacks must run concurrently without an explicit --parallelism option.
        runBatch(0, commands, 0, true);
        assertTrue(Files.readString(directory.resolve("run-0.log")).contains("Automatic stdin parallelism: 2"));
        for (int i = 0; i < 4; ++i) {
            assertArrayEquals(before.get(i), Files.readAllBytes(directory.resolve("default-" + i + ".json")));
        }
    }

    @Test
    void concurrentCallbacksProtocolsFormulasAndMappingMatchSerialBytes() throws Exception {
        fixtures();
        var tasks = new ArrayList<String>();
        for (int i = 0; i < 8; ++i) {
            tasks.add(task(i, "out-" + i + ".json", true));
        }
        String commands = String.join("\n", tasks) + "\n";
        runBatch(1, commands, 0, false);
        Map<Path, byte[]> before = new HashMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".txt")).toList()) {
                before.put(file, Files.readAllBytes(file));
                Files.delete(file);
            }
        }
        // Four callbacks must reach a barrier together, twice. Serial execution cannot pass this test.
        runBatch(4, commands, 0, true);
        for (var entry : before.entrySet()) {
            assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), entry.getKey().toString());
        }
        for (int i = 0; i < 8; ++i) {
            JSONArray output = new JSONArray(Files.readString(directory.resolve("out-" + i + ".json")));
            assertEquals(i % 2 == 0 ? "task-" + i : "batch", output.getJSONObject(0).getString("data_ver"));
            var row = output.getJSONObject(1).getJSONArray("Record").getJSONObject(0);
            assertEquals("out-" + i + ".json:1", row.getString("label"));
            if (i % 2 == 0) {
                assertInstanceOf(Number.class, row.get("value"));
                assertEquals(2, row.getInt("value"));
            } else {
                assertEquals("2", row.getString("value"));
            }
        }
        String firstMapping = Files.readString(directory.resolve("mapping-0.txt"));
        String secondMapping = Files.readString(directory.resolve("mapping-1.txt"));
        assertNotEquals(firstMapping, secondMapping);
        assertEquals(8, firstMapping.lines().count());
        assertEquals(8, secondMapping.lines().count());
    }

    @Test
    void failuresDoNotSkipLaterTasksAndDuplicateOutputsKeepInputOrder() throws Exception {
        fixtures();
        String commands = task(0, "same.json", true) + "\n"
                + "--not-an-option\n"
                + "-t json -f missing.pb -c missing.json\n"
                + task(1, "same.json", false) + "\n"
                + task(2, "last.json", false) + "\n";
        runBatch(4, commands, 2, false);
        var overwritten = new JSONArray(Files.readString(directory.resolve("same.json")));
        assertEquals("batch", overwritten.getJSONObject(0).getString("data_ver"));
        var row = overwritten.getJSONObject(1).getJSONArray("Record").getJSONObject(0);
        assertEquals("2", row.getString("value"));
        assertEquals("original", row.getString("label"), "Callback must not leak to the next task");
        assertTrue(Files.exists(directory.resolve("last.json")));
        // Also exercise reuse on a single worker and compare the last-writer result.
        byte[] expected = Files.readAllBytes(directory.resolve("same.json"));
        runBatch(1, commands, 2, false);
        assertArrayEquals(expected, Files.readAllBytes(directory.resolve("same.json")));
    }

    @Test
    void dataDirectoriesMacrosAndSameNamedCustomValidatorsStayIsolated() throws Exception {
        fixtures();
        for (int index = 0; index < 2; ++index) {
            Path inputDirectory = Files.createDirectory(directory.resolve("source-" + index));
            int value = index == 0 ? 2 : 7;
            Files.writeString(inputDirectory.resolve("values.txt"), value + "\n");
            Files.writeString(inputDirectory.resolve("rules.yaml"),
                    "validator:\n  - name: allowed\n    mode: and\n    rules:\n"
                    + "      - 'InValues(" + value + ")'\n      - 'InText(\"values.txt\")'\n");
            try (var workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("data");
                var header = sheet.createRow(0);
                header.createCell(0).setCellValue("value@allowed");
                header.createCell(1).setCellValue("label");
                var row = sheet.createRow(1);
                row.createCell(0).setCellValue(value);
                row.createCell(1).setCellValue("NAME");
                var macro = workbook.createSheet("macro");
                macro.createRow(0).createCell(0).setCellValue("key");
                var macroRow = macro.createRow(1);
                macroRow.createCell(0).setCellValue("NAME");
                macroRow.createCell(1).setCellValue("source-" + index);
                try (var output = Files.newOutputStream(inputDirectory.resolve("data.xlsx"))) {
                    workbook.write(output);
                }
            }
        }
        var tasks = new ArrayList<String>();
        for (int i = 0; i < 12; ++i) {
            tasks.add("-t json -f TYPE_INT32.pb -d source-" + i % 2 + " --validator-rules rules.yaml"
                    + " -m DataSource=data.xlsx|data|2,1 -m MacroSource=data.xlsx|macro|2,1"
                    + " -m ProtoName=Record -m KeyRow=1 -m OutputFile=isolated-" + i + ".json"
                    + (i % 3 == 0 ? " --disable-string-macro" : " --enable-string-macro"));
        }
        runBatch(4, String.join("\n", tasks) + "\n", 0, false);
        for (int i = 0; i < 12; ++i) {
            var output = new JSONArray(Files.readString(directory.resolve("isolated-" + i + ".json")));
            var row = output.getJSONObject(1).getJSONArray("Record").getJSONObject(0);
            assertEquals(i % 2 == 0 ? 2 : 7, row.getInt("value"));
            assertEquals(i % 3 == 0 ? "NAME" : "source-" + i % 2, row.getString("label"));
        }
    }

    @Test
    void sharedProtocolFieldValidatorsRemainBoundToEachTasksRules() throws Exception {
        fixtures();
        var original = DescriptorProtos.FileDescriptorSet.parseFrom(Files.readAllBytes(directory.resolve("TYPE_INT32.pb")));
        var file = original.getFile(0).toBuilder();
        file.getMessageTypeBuilder(0).getFieldBuilder(0).setOptions(DescriptorProtos.FieldOptions.newBuilder()
                .setExtension(org.xresloader.Xresloader.validator, "allowed"));
        Files.write(directory.resolve("shared.pb"), DescriptorProtos.FileDescriptorSet.newBuilder().addFile(file)
                .build().toByteArray());
        for (int value : List.of(2, 7)) {
            Path source = Files.createDirectory(directory.resolve("bound-" + value));
            Files.writeString(source.resolve("rules.yaml"),
                    "validator:\n  - name: allowed\n    mode: and\n    rules:\n      - 'InValues(" + value + ")'\n");
            try (var workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("data");
                var header = sheet.createRow(0);
                header.createCell(0).setCellValue("value");
                header.createCell(1).setCellValue("label");
                var row = sheet.createRow(1);
                row.createCell(0).setCellValue(value);
                row.createCell(1).setCellValue("original");
                try (var output = Files.newOutputStream(source.resolve("data.xlsx"))) {
                    workbook.write(output);
                }
            }
        }
        var tasks = new ArrayList<String>();
        for (int i = 0; i < 12; ++i) {
            tasks.add("-t json -f shared.pb -d bound-" + (i % 2 == 0 ? 2 : 7) + " --validator-rules rules.yaml"
                    + " -m DataSource=data.xlsx|data|2,1 -m ProtoName=Record -m KeyRow=1"
                    + " -m CallbackScript=callback.js -m OutputFile=bound-" + i + ".json");
        }
        String commands = String.join("\n", tasks) + "\n";
        runBatch(1, commands, 0, false);
        var reference = new ArrayList<byte[]>();
        for (int i = 0; i < 12; ++i) {
            reference.add(Files.readAllBytes(directory.resolve("bound-" + i + ".json")));
        }
        // Repeat with cold JVM caches and different worker counts; four callbacks synchronize per wave.
        for (int workers : List.of(4, 8, 4)) {
            runBatch(workers, commands, 0, true);
            for (int i = 0; i < 12; ++i) {
                Path output = directory.resolve("bound-" + i + ".json");
                assertArrayEquals(reference.get(i), Files.readAllBytes(output));
                assertEquals(i % 2 == 0 ? 2 : 7, new JSONArray(Files.readString(output))
                        .getJSONObject(1).getJSONArray("Record").getJSONObject(0).getInt("value"));
            }
        }
    }

    public static class Probe {
        private static final CyclicBarrier barrier = new CyclicBarrier(Integer.getInteger("xresloader.test.barrier.parties", 4));
        public static void awaitWorkers() throws Exception {
            if (Boolean.getBoolean("xresloader.test.barrier")) {
                barrier.await(20, TimeUnit.SECONDS);
            }
        }
        public static void main(String[] arguments) {
            Main.main(arguments);
        }
    }
}
