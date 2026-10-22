package org.xresloader.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.xresloader.core.data.dst.DataDstImpl;
import org.xresloader.core.data.dst.DataDstJavascript;
import org.xresloader.core.data.dst.DataDstJson;
import org.xresloader.core.data.dst.DataDstLua;
import org.xresloader.core.data.dst.DataDstMsgPack;
import org.xresloader.core.data.dst.DataDstPb;
import org.xresloader.core.data.dst.DataDstUEBase;
import org.xresloader.core.data.dst.DataDstUECsv;
import org.xresloader.core.data.dst.DataDstUEJson;
import org.xresloader.core.data.dst.DataDstXml;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.err.InitializeException;
import org.xresloader.core.data.et.DataETProcessor;
import org.xresloader.core.data.src.DataSrcExcel;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.scheme.SchemeConf;

/**
 * @author owentou
 */
public class Main {

    /**
     * Initialize stream protection and Log4j2 configuration BEFORE any Log4j2
     * classes are loaded.
     * This prevents Log4j2's ConsoleAppender lifecycle from closing stdout/stderr
     * and ensures
     * Log4j2 uses the correct configuration from the start (avoiding
     * DefaultConfiguration).
     */
    static {
        // FIRST: Protect stdout/stderr from being closed
        StreamProtector.init();

        // SECOND: Set log4j.configurationFile system property if not already set
        // This must happen BEFORE any Log4j2 class is loaded
        initializeLog4j2SystemProperty();
    }

    private static void initializeLog4j2SystemProperty() {
        try {
            // Check if user has specified a custom configuration
            String configFile = System.getProperty("log4j.configurationFile");
            if (configFile == null || configFile.isEmpty()) {
                configFile = System.getProperty("log4j2.configurationFile");
            }

            if (configFile == null || configFile.isEmpty()) {
                // No custom config specified, set the system property to our bundled default
                // Log4j2 will find log4j2.xml in classpath automatically, but we set the
                // property explicitly to ensure it's loaded before any logging occurs
                URL defaultConfig = Main.class.getClassLoader().getResource("log4j2.xml");
                if (defaultConfig != null) {
                    System.setProperty("log4j2.configurationFile", defaultConfig.toURI().toString());
                }
            }
            // If user already specified a config, Log4j2 will use it automatically
        } catch (Exception ignored) {
            // If this fails, Log4j2 will fall back to defaults
        }
    }

    private static String lineSeparator = "\n";

    private static DataDstImpl createOutputDescriptor(DataDstImpl protocolDescriptor) {
        DataDstImpl outputDescriptor = null;
        switch (ProgramOptions.getInstance().outType) {
            case BIN -> outputDescriptor = protocolDescriptor;
            case LUA -> {
                outputDescriptor = new DataDstLua();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case MSGPACK -> {
                outputDescriptor = new DataDstMsgPack();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case JSON -> {
                outputDescriptor = new DataDstJson();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case XML -> {
                outputDescriptor = new DataDstXml();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case JAVASCRIPT -> {
                outputDescriptor = new DataDstJavascript();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case UECSV -> {
                outputDescriptor = new DataDstUECsv();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            case UEJSON -> {
                outputDescriptor = new DataDstUEJson();
                outputDescriptor = outputDescriptor.init() ? outputDescriptor : null;
            }
            default -> ProgramOptions.getLogger().error("Output type \"%s\" invalid",
                    ProgramOptions.getInstance().outType.toString());
        }

        return outputDescriptor;
    }

    private static int printProtoData() {
        DataDstImpl protocolDescriptor = null;
        switch (ProgramOptions.getInstance().protocol) {
            case PROTOBUF -> protocolDescriptor = new DataDstPb();
            default -> ProgramOptions.getLogger().error("Protocol type \"%s\" invalid",
                    ProgramOptions.getInstance().protocol.toString());
        }

        if (protocolDescriptor == null) {
            return 1;
        }

        DataDstImpl outputDescriptor = createOutputDescriptor(protocolDescriptor);
        if (outputDescriptor == null) {
            return 1;
        }

        HashMap<String, Object> dumpData = null;
        String dumpName = null;

        switch (ProgramOptions.getInstance().protoDumpType) {
            case CONST -> {
                dumpData = protocolDescriptor.buildConst();
                dumpName = "const";
            }
            case DESCRIPTOR, OPTIONS -> {
                dumpData = protocolDescriptor.buildOptions(ProgramOptions.getInstance().protoDumpType);
                dumpName = "option";
            }
            default -> {
            }
        }

        if (dumpData == null) {
            ProgramOptions.getLogger().error("Protocol description \"%s\" initialize and build %s values failed",
                    ProgramOptions.getInstance().protocol.toString(), dumpName);
            return 1;
        }

        String outputFilePath = SchemeConf.getInstance().getOutputFileAbsPath();
        File outputFile = new File(outputFilePath);
        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        try {
            byte[] data = outputDescriptor.dumpConst(dumpData);
            TaskOrder.awaitOutputTurn();
            try (OutputStream outputStream = new FileOutputStream(outputFilePath, false)) {
                if (data != null) {
                    outputStream.write(data);
                } else {
                    ProgramOptions.getLogger().error("Write %s data to file \"%s\" failed, output type invalid.", dumpName,
                            ProgramOptions.getInstance().protoDumpFile);
                    return 1;
                }
            }
        } catch (IOException | ConvException exception) {
            ProgramOptions.getLogger().error("Write data to file \"%s\" failed%s  > %s",
                    ProgramOptions.getInstance().protoDumpFile, lineSeparator, exception.getMessage());
            return 1;
        }

        ProgramOptions.getLogger().info("Write %s data to \"%s\" success.(charset: %s)", dumpName,
                ProgramOptions.getInstance().protoDumpFile, SchemeConf.getInstance().getKey().getEncoding());

        return 0;
    }

    private static int processPreparedGroup(String[] arguments) throws InitializeException {
        SchemeConf.getInstance().reset();

        if (!ProgramOptions.getInstance().protoDumpFile.isEmpty()) {
            return printProtoData();
        }

        int schemeInitResult = SchemeConf.getInstance().initScheme();
        if (schemeInitResult < 0) {
            return 1;
        }

        if (ProgramOptions.getInstance().dataSourceMetas == null) {
            return 1;
        }

        int failedCount = 0;
        boolean continueNextScheme = true;
        for (int i = 0; continueNextScheme && i < ProgramOptions.getInstance().dataSourceMetas.length; ++i) {
            String sourceName = ProgramOptions.getInstance().dataSourceMetas[i];

            SchemeConf.getInstance().getMacroSource().clear();
            SchemeConf.getInstance().getDataSource().clear();

            if (!SchemeConf.getInstance().getScheme().loadScheme(sourceName)) {
                sourceName = String.join(" ", ProgramOptions.getInstance().dataSourceMetas);
                ProgramOptions.getLogger().error("Convert from \"%s\" failed", sourceName);
                ++failedCount;
                continue;
            }

            continueNextScheme = SchemeConf.getInstance().getScheme().isSupportMultipleScheme();

            StringBuilder descriptionBuilder = new StringBuilder();
            for (SchemeConf.DataInfo dataInfo : SchemeConf.getInstance().getDataSource()) {
                if (descriptionBuilder.length() > 0) {
                    descriptionBuilder.append(',');
                }

                if (!dataInfo.filePath.isEmpty()) {
                    descriptionBuilder.append(dataInfo.filePath);
                    descriptionBuilder.append('|');
                }

                descriptionBuilder.append(dataInfo.tableName);
            }
            sourceName = descriptionBuilder.toString();

            Class<?> dataSourceClass = DataSrcExcel.class;
            DataSrcImpl dataSource = DataSrcImpl.create(dataSourceClass);
            if (dataSource == null) {
                ProgramOptions.getLogger().error("Create data source class \"%s\" failed", dataSourceClass.getName());
                ++failedCount;
                continue;
            }

            int dataSourceInitResult = dataSource.init();
            if (dataSourceInitResult < 0) {
                ProgramOptions.getLogger().error("Initialize data source class \"%s\" failed",
                        dataSourceClass.getName());
                ++failedCount;
                continue;
            }

            DataDstImpl protocolDescriptor = null;
            switch (ProgramOptions.getInstance().protocol) {
                case PROTOBUF -> protocolDescriptor = new DataDstPb();
                default -> {
                    ProgramOptions.getLogger().error("Protocol type \"%s\" invalid",
                            ProgramOptions.getInstance().protocol.toString());
                    ++failedCount;
                }
            }

            if (protocolDescriptor == null) {
                continue;
            }

            ProgramOptions.getLogger().trace("Convert from \"%s\" to \"%s\" started (protocol=%s) ...", sourceName,
                    SchemeConf.getInstance().getOutputFile(), SchemeConf.getInstance().getProtoName());

            if (!protocolDescriptor.init()) {
                ProgramOptions.getLogger().error("Protocol description \"%s\" initialize failed: %s ",
                        ProgramOptions.getInstance().protocol.toString(),
                        protocolDescriptor.getLastErrorMessage());
                ++failedCount;
                continue;
            }

            DataDstImpl outputDescriptor = createOutputDescriptor(protocolDescriptor);
            if (outputDescriptor == null) {
                continue;
            }

            String outputFilePath = SchemeConf.getInstance().getOutputFileAbsPath();
            File outputFile = new File(outputFilePath);
            File outputDirectory = outputFile.getParentFile();
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            try {
                byte[] data = outputDescriptor.build(protocolDescriptor);
                TaskOrder.awaitOutputTurn();
                try (OutputStream outputStream = new FileOutputStream(outputFilePath, false)) {
                    if (data != null) {
                        outputStream.write(data);
                    }
                }
            } catch (ConvException exception) {
                String currentFileName = dataSource.getCurrentFileName();
                String currentTableName = dataSource.getCurrentTableName();

                if (!currentFileName.isEmpty() && !currentTableName.isEmpty() && dataSource.hasCurrentDataGrid()) {
                    ProgramOptions.getLogger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > %s",
                            lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName, currentTableName,
                            dataSource.getLastRowNum() + 1, dataSource.getLastColumnNum() + 1,
                            ExcelEngine.getColumnName(dataSource.getLastColumnNum() + 1), lineSeparator,
                            exception.getMessage());
                } else if (!currentFileName.isEmpty() && !currentTableName.isEmpty()) {
                    ProgramOptions.getLogger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s%s  > %s", lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName, currentTableName,
                            lineSeparator, exception.getMessage());
                } else if (!currentFileName.isEmpty()) {
                    ProgramOptions.getLogger().error(
                            "Convert data failed.%s  > %s%s  > File: %s%s  > %s", lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName,
                            lineSeparator, exception.getMessage());
                } else {
                    ProgramOptions.getLogger().error(
                            "Convert data failed.%s  > %s%s  > %s", lineSeparator,
                            String.join(" ", arguments),
                            lineSeparator, exception.getMessage());
                }

                ++failedCount;
                continue;
            } catch (IOException exception) {
                ProgramOptions.getLogger().error("Write data to file \"%s\" failed%s  > %s",
                        SchemeConf.getInstance().getOutputFile(), lineSeparator, exception.getMessage());
                ++failedCount;
                continue;
            }

            ProgramOptions.getLogger().info("Convert from \"%s\" to \"%s\" success.(charset: %s, %d excel row(s))",
                    sourceName,
                    SchemeConf.getInstance().getOutputFile(), SchemeConf.getInstance().getKey().getEncoding(),
                    dataSource.getRecordNumber());
        }

        return failedCount;
    }

    private static final Pattern INPUT_ARGUMENT_PATTERN = Pattern.compile("('[^']*')|(\"[^\"]*\")|(\\S+)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static String[] readArgsFromStdin(Scanner input) {
        if (!input.hasNextLine()) {
            return null;
        }

        Matcher matcher = INPUT_ARGUMENT_PATTERN.matcher(input.nextLine());
        LinkedList<String> tokens = new LinkedList<>();

        while (matcher.find()) {
            String token = matcher.group();
            if (token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"') {
                token = token.length() > 2 ? token.substring(1, token.length() - 1) : "";
            } else if (token.charAt(0) == '\'' && token.charAt(token.length() - 1) == '\'') {
                token = token.length() > 2 ? token.substring(1, token.length() - 1) : "";
            }

            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        String[] result = new String[tokens.size()];
        for (int index = 0; !tokens.isEmpty(); ++index, tokens.removeFirst()) {
            result[index] = tokens.getFirst();
        }

        return result;
    }

    private static void reportException(String[] arguments, Exception exception) {
        if (exception instanceof InitializeException) {
            ProgramOptions.getLogger().error("Initialize failed. %s%s  > %s", exception.getMessage(),
                    lineSeparator, String.join(" ", arguments));
            return;
        }
        ProgramOptions.getLogger().error("%s", exception.getMessage());
        for (StackTraceElement frame : exception.getStackTrace()) {
            ProgramOptions.getLogger().error("\t%s", frame.toString());
        }
        ProgramOptions.getLogger().error("Panic!, it's probably a BUG, please report to %s, current version: %s",
                ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
    }

    private static int runPreparedGroup(ProgramOptions options, String[] arguments, int initializationResult) {
        ProgramOptions.install(options);
        try {
            if (initializationResult != 0) {
                return initializationResult < 0 ? 1 : 0;
            }
            return processPreparedGroup(arguments);
        } catch (Exception exception) {
            reportException(arguments, exception);
            return 1;
        } finally {
            // A pool thread must never carry bindings, callbacks or validators into its next task.
            DataETProcessor.clearThreadState();
            DataDstPb.clearThreadState();
            DataDstUEBase.clearThreadState();
            DataSrcImpl.clearThreadState();
            SchemeConf.clearThreadState();
            ExcelEngine.clearThreadState();
        }
    }

    private static int initialize(String[] arguments) {
        try {
            return ProgramOptions.getInstance().init(arguments);
        } catch (Exception exception) {
            reportException(arguments, exception);
            return -1;
        }
    }

    private static int awaitTask(Future<Integer> task) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return task.get();
                } catch (InterruptedException exception) {
                    // Drain accepted tasks before exiting; no output may outlive the process.
                    interrupted = true;
                } catch (ExecutionException exception) {
                    ProgramOptions.getLogger().error("Conversion worker failed: %s", exception.getCause());
                    return 1;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] arguments) {
        lineSeparator = ProgramOptions.getEndl();
        ExcelEngine.setMaxByteArraySize(Integer.MAX_VALUE);
        ZipSecureFile.setMinInflateRatio(0);

        int initializationResult = initialize(arguments);
        ProgramOptions launchOptions = ProgramOptions.getInstance();
        int exitCode = runPreparedGroup(launchOptions, arguments, initializationResult);
        if (launchOptions.enableStdin) {
            // Materialize a single default version before handing any options to workers.
            launchOptions.getDefaultDataVersion();
            int parallelism = launchOptions.parallelism;
            if (launchOptions.automaticParallelism) {
                ProgramOptions.getLogger().info("Automatic stdin parallelism: %d (processors: %d, max heap: %d MiB)",
                        parallelism, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / (1024 * 1024));
            }
            ExecutorService workers = parallelism > 1 ? Executors.newFixedThreadPool(parallelism) : null;
            ArrayDeque<Future<Integer>> pending = new ArrayDeque<>();
            TaskOrder previous = null;
            try {
                Scanner input = new Scanner(System.in);
                String[] taskArguments;
                while ((taskArguments = readArgsFromStdin(input)) != null) {
                    if (taskArguments.length == 0) {
                        continue;
                    }
                    ProgramOptions taskOptions = ProgramOptions.nextTask();
                    int taskInitialization = initialize(taskArguments);
                    if (workers == null) {
                        exitCode = Math.min(255, exitCode + runPreparedGroup(taskOptions, taskArguments, taskInitialization));
                    } else {
                        TaskOrder order = new TaskOrder(previous);
                        previous = order;
                        String[] task = taskArguments;
                        pending.addLast(workers.submit(() -> {
                            order.enter();
                            try {
                                return runPreparedGroup(taskOptions, task, taskInitialization);
                            } finally {
                                order.finish();
                                ProgramOptions.clearThreadState();
                            }
                        }));
                        // Bound accepted work and completed output buffers, even for endless stdin.
                        if (pending.size() >= parallelism) {
                            exitCode = Math.min(255, exitCode + awaitTask(pending.removeFirst()));
                        }
                    }
                }
            } finally {
                if (workers != null) {
                    workers.shutdown();
                }
                while (!pending.isEmpty()) {
                    exitCode = Math.min(255, exitCode + awaitTask(pending.removeFirst()));
                }
            }
        }
        exitCode = Math.min(255, exitCode + ProgramOptions.dumpDataSourceMapping());
        System.exit(exitCode);
    }
}
