package org.xresloader.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.xresloader.core.data.dst.DataDstImpl;
import org.xresloader.core.data.dst.DataDstJavascript;
import org.xresloader.core.data.dst.DataDstJson;
import org.xresloader.core.data.dst.DataDstLua;
import org.xresloader.core.data.dst.DataDstMsgPack;
import org.xresloader.core.data.dst.DataDstPb;
import org.xresloader.core.data.dst.DataDstUECsv;
import org.xresloader.core.data.dst.DataDstUEJson;
import org.xresloader.core.data.dst.DataDstXml;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.err.InitializeException;
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
            default -> ProgramOptions.getLoger().error("Output type \"%s\" invalid",
                    ProgramOptions.getInstance().outType.toString());
        }

        return outputDescriptor;
    }

    private static int printProtoData() {
        DataDstImpl protocolDescriptor = null;
        switch (ProgramOptions.getInstance().protocol) {
            case PROTOBUF -> protocolDescriptor = new DataDstPb();
            default -> ProgramOptions.getLoger().error("Protocol type \"%s\" invalid",
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
            ProgramOptions.getLoger().error("Protocol description \"%s\" initialize and build %s values failed",
                    ProgramOptions.getInstance().protocol.toString(), dumpName);
            return 1;
        }

        String outputFilePath = SchemeConf.getInstance().getOutputFileAbsPath();
        File outputFile = new File(outputFilePath);
        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        try (OutputStream outputStream = new FileOutputStream(outputFilePath, false)) {
            byte[] data = outputDescriptor.dumpConst(dumpData);
            if (data != null) {
                outputStream.write(data);
            } else {
                ProgramOptions.getLoger().error("Write %s data to file \"%s\" failed, output type invalid.", dumpName,
                        ProgramOptions.getInstance().protoDumpFile);
                return 1;
            }
        } catch (IOException | ConvException exception) {
            ProgramOptions.getLoger().error("Write data to file \"%s\" failed%s  > %s",
                    ProgramOptions.getInstance().protoDumpFile, lineSeparator, exception.getMessage());
            return 1;
        }

        ProgramOptions.getLoger().info("Write %s data to \"%s\" success.(charset: %s)", dumpName,
                ProgramOptions.getInstance().protoDumpFile, SchemeConf.getInstance().getKey().getEncoding());

        return 0;
    }

    private static int processArgumentGroup(String[] arguments) throws InitializeException {
        int initializationResult = ProgramOptions.getInstance().init(arguments);
        if (initializationResult < 0) {
            return 1;
        } else if (initializationResult > 0) {
            return 0;
        }

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

            if (!SchemeConf.getInstance().getScheme().load_scheme(sourceName)) {
                sourceName = String.join(" ", ProgramOptions.getInstance().dataSourceMetas);
                ProgramOptions.getLoger().error("Convert from \"%s\" failed", sourceName);
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
                ProgramOptions.getLoger().error("Create data source class \"%s\" failed", dataSourceClass.getName());
                ++failedCount;
                continue;
            }

            int dataSourceInitResult = dataSource.init();
            if (dataSourceInitResult < 0) {
                ProgramOptions.getLoger().error("Initialize data source class \"%s\" failed",
                        dataSourceClass.getName());
                ++failedCount;
                continue;
            }

            DataDstImpl protocolDescriptor = null;
            switch (ProgramOptions.getInstance().protocol) {
                case PROTOBUF -> protocolDescriptor = new DataDstPb();
                default -> {
                    ProgramOptions.getLoger().error("Protocol type \"%s\" invalid",
                            ProgramOptions.getInstance().protocol.toString());
                    ++failedCount;
                }
            }

            if (protocolDescriptor == null) {
                continue;
            }

            ProgramOptions.getLoger().trace("Convert from \"%s\" to \"%s\" started (protocol=%s) ...", sourceName,
                    SchemeConf.getInstance().getOutputFile(), SchemeConf.getInstance().getProtoName());

            if (!protocolDescriptor.init()) {
                ProgramOptions.getLoger().error("Protocol description \"%s\" initialize failed: %s ",
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

            try (OutputStream outputStream = new FileOutputStream(outputFilePath, false)) {
                byte[] data = outputDescriptor.build(protocolDescriptor);
                if (data != null) {
                    outputStream.write(data);
                }
            } catch (ConvException exception) {
                String currentFileName = dataSource.getCurrentFileName();
                String currentTableName = dataSource.getCurrentTableName();

                if (!currentFileName.isEmpty() && !currentTableName.isEmpty() && dataSource.hasCurrentDataGrid()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > %s",
                            lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName, currentTableName,
                            dataSource.getLastRowNum() + 1, dataSource.getLastColumnNum() + 1,
                            ExcelEngine.getColumnName(dataSource.getLastColumnNum() + 1), lineSeparator,
                            exception.getMessage());
                } else if (!currentFileName.isEmpty() && !currentTableName.isEmpty()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s, Table: %s%s  > %s", lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName, currentTableName,
                            lineSeparator, exception.getMessage());
                } else if (!currentFileName.isEmpty()) {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > File: %s%s  > %s", lineSeparator,
                            String.join(" ", arguments), lineSeparator, currentFileName,
                            lineSeparator, exception.getMessage());
                } else {
                    ProgramOptions.getLoger().error(
                            "Convert data failed.%s  > %s%s  > %s", lineSeparator,
                            String.join(" ", arguments),
                            lineSeparator, exception.getMessage());
                }

                ++failedCount;
                continue;
            } catch (IOException exception) {
                ProgramOptions.getLoger().error("Write data to file \"%s\" failed%s  > %s",
                        SchemeConf.getInstance().getOutputFile(), lineSeparator, exception.getMessage());
                ++failedCount;
                continue;
            }

            ProgramOptions.getLoger().info("Convert from \"%s\" to \"%s\" success.(charset: %s, %d excel row(s))",
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

    public static void main(String[] arguments) {
        lineSeparator = ProgramOptions.getEndl();

        ExcelEngine.setMaxByteArraySize(Integer.MAX_VALUE);
        ZipSecureFile.setMinInflateRatio(0);

        int exitCode = 1;
        try {
            exitCode = processArgumentGroup(arguments);
        } catch (InitializeException exception) {
            ProgramOptions.getLoger().error("Initlize failed.%s%s  > %s", exception.getMessage(), lineSeparator,
                    String.join(" ", arguments));
        } catch (Exception exception) {
            ProgramOptions.getLoger().error("%s", exception.getMessage());
            for (StackTraceElement frame : exception.getStackTrace()) {
                ProgramOptions.getLoger().error("\t%s", frame.toString());
            }

            ProgramOptions.getLoger().error("Panic!, it's probably a BUG, please report to %s, current version: %s",
                    ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
        }

        if (ProgramOptions.getInstance().enableStdin) {
            String[] stdinArguments;
            Scanner stdinScanner = new Scanner(System.in);
            while ((stdinArguments = readArgsFromStdin(stdinScanner)) != null) {
                if (stdinArguments.length == 0) {
                    continue;
                }

                try {
                    exitCode += processArgumentGroup(stdinArguments);
                } catch (InitializeException exception) {
                    ++exitCode;
                    ProgramOptions.getLoger().error("Initlize failed.%s%s  > %s", exception.getMessage(),
                            lineSeparator, String.join(" ", stdinArguments));
                } catch (Exception exception) {
                    ++exitCode;
                    ProgramOptions.getLoger().error("%s", exception.getMessage());
                    for (StackTraceElement frame : exception.getStackTrace()) {
                        ProgramOptions.getLoger().error("\t%s", frame.toString());
                    }

                    ProgramOptions.getLoger().error(
                            "Panic!, it's probably a BUG, please report to %s, current version: %s",
                            ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
                }
            }
        }

        exitCode += ProgramOptions.dumpDataSourceMapping();
        if (exitCode > 255) {
            exitCode = 255;
        }
        System.exit(exitCode);
    }

}
