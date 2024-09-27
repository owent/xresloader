package org.xresloader.core;

import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// Import log4j classes.

public class ProgramOptions {

    private static Logger logger = null;
    static private String endl = null;

    public class RenameRule {
        public Pattern match;
        public String replace;
    }

    /**
     * 单例
     */
    private static ThreadLocal<ProgramOptions> instance = null;
    private static Options options = null;
    private static String version = null;
    private static Properties properties = null;
    private static String dataSourceMappingFile = "";
    private static HashMap<String, String> dataSourceMappingResult = new HashMap<String, String>();
    private static String dataSourceMappingHashSeed = "xresloader";

    private String defaultDataVersion = null;
    private String dataVersion = null;

    public FileType outType;

    public Protocol protocol;
    public String[] protocolFile = null;
    public boolean protocolIgnoreUnknownDependency = false;
    public String outputDirectory = ".";
    public String[] dataSourceDirectory = null;
    public String dataSourceFile = "";
    public FileType dataSourceType;
    public String[] dataSourceMetas = null;
    public String dataSourceMetaDelimiter = "\\|";
    public int dataSourceLruCacheRows = 300000;
    public boolean enableStringMacro = false;
    public RenameRule renameRule = null;
    public boolean requireMappingAllFields = false;
    public boolean enableAliasMapping = true;
    public boolean enableFormular = false;
    public ListStripRule stripListRule = ListStripRule.STRIP_EMPTY_ALL;
    public int prettyIndent = 0;
    public boolean enableStdin = false;
    public String[] customValidatorRules = null;
    public boolean enableDataValidator = true;
    public String[] ignoreFieldTags = null;

    public String protoDumpFile = "";
    public ProtoDumpType protoDumpType = ProtoDumpType.NONE;
    public boolean luaGlobal = false;
    public String luaModule = null;
    public String xmlRootName = "root";
    public String javascriptExport = null;
    public String javascriptGlobalVar = "";
    public int tolerateContinueEmptyRows = 100000;
    public DataSourceMappingType dataSourceMappingType = DataSourceMappingType.NONE;

    private ProgramOptions() {
        dataSourceMetas = new String[] {};
        outType = FileType.BIN;
        protocol = Protocol.PROTOBUF;

        outputDirectory = System.getProperty("user.dir");
        dataSourceDirectory = new String[] { outputDirectory };
        dataSourceType = FileType.BIN;
        requireMappingAllFields = false;
        enableAliasMapping = true;
    }

    public static ProgramOptions getInstance() {
        if (instance == null) {
            instance = new ThreadLocal<>();
        }

        if (null == instance.get()) {
            instance.set(new ProgramOptions());
            instance.get().reset();
        }

        return instance.get();
    }

    public void reset() {
        outType = FileType.BIN;
        protocol = Protocol.PROTOBUF;
        protocolFile = null;
        protocolIgnoreUnknownDependency = false;
        outputDirectory = "";
        dataSourceDirectory = null;
        dataSourceFile = "";
        dataSourceType = FileType.BIN;
        dataSourceMetas = null;
        dataSourceMetaDelimiter = "\\|";
        dataSourceLruCacheRows = 300000;
        enableStringMacro = false;
        renameRule = null;
        requireMappingAllFields = false;
        enableAliasMapping = true;
        enableFormular = false;
        stripListRule = ListStripRule.STRIP_EMPTY_ALL;
        prettyIndent = 0;
        enableStdin = false;
        customValidatorRules = null;
        enableDataValidator = true;
        ignoreFieldTags = null;

        protoDumpFile = "";
        protoDumpType = ProtoDumpType.NONE;
        luaGlobal = false;
        luaModule = null;
        xmlRootName = "root";
        javascriptExport = null;
        javascriptGlobalVar = "";
        tolerateContinueEmptyRows = 100000;

        dataSourceMappingType = DataSourceMappingType.NONE;

        dataVersion = null;
    }

    private static synchronized Options get_options_group() {
        if (null != options) {
            return options;
        }

        // create the Options
        options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");

        options.addOption(Option.builder("t").longOpt("output-type")
                .desc("output type(bin, lua, msgpack, json, xml, javascript/js, ue-csv, ue-json)").hasArg()
                .argName("TYPE").build());

        options.addOption(
                Option.builder("p").longOpt("proto").desc("protocol(protobuf)").hasArg().argName("PROTOCOL").build());

        options.addOption(Option.builder("f").longOpt("proto-file").desc("protocol description file").hasArg()
                .argName("FILE NAME").build());

        options.addOption(null, "ignore-unknown-dependency", false,
                "ignore unknown dependency when initialize protocol files.");

        options.addOption(Option.builder("o").longOpt("output-dir").desc("output directory").hasArg()
                .argName("DIRECTORY PATH").build());

        options.addOption(Option.builder("d").longOpt("data-src-dir")
                .desc("data source directories(where to find excel files, can be used more than once.)").hasArg()
                .argName("DIRECTORY PATH")
                .build());

        options.addOption(Option.builder("s").longOpt("src-file")
                .desc("data source file(.xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json)").hasArg()
                .argName("META FILE PATH").build());

        options.addOption(Option.builder("m").longOpt("src-meta")
                .desc(String.format("%s\n\t%-32s=>%s\n\t%-32s=>%s\n\t%-32s=>%s\n\t%-32s=>%s",
                        "data description meta information of data source.", ".xls,/xlsx/.cvx/.xlsm/.dos",
                        "scheme sheet name", ".ini/.cfg/.conf", "scheme section name", ".json", "scheme key name",
                        "[NOTHING]", "KEY=VALUE1|VAL2|VAL3 pair of scheme configures"))
                .hasArg().argName("META NAME").build());

        options.addOption(Option.builder("l").longOpt("delimiter")
                .desc(String.format(
                        "regex delimiter for description meta when data source file is [NOTHING].(default: %s)",
                        getInstance().dataSourceMetaDelimiter))
                .hasArg().argName("DELTMITER").build());

        options.addOption(null, "enable-string-macro", false,
                "macro will also apply to string value.");
        options.addOption(null, "disable-string-macro", false,
                "[default] macro will not apply to string value.");

        options.addOption("v", "version", false, "print version and exit");

        options.addOption(Option.builder("n").longOpt("rename")
                .desc("rename output file name(regex), sample: /(?i)\\.bin$/\\.lua/").hasArg().argName("RENAME PATTERN")
                .build());
        options.addOption(null, "require-mapping-all", false,
                "require all fields in protocol message to be mapped from data source");
        options.addOption(null, "enable-alias-mapping", false, "allow to use alias when mapping fields");
        options.addOption(null, "disable-alias-mapping", false, "do not use alias when mapping fields");

        options.addOption(Option.builder("a").longOpt("data-version").desc("set data version").hasArg()
                .argName("DATA VERSION").build());

        options.addOption(Option.builder().longOpt("pretty").desc(
                "set pretty output and set ident length when output type supported.(disable pretty output by set to 0)")
                .hasArg().argName("INDENT LENGTH").build());

        options.addOption(Option.builder("c").longOpt("const-print").desc("print all const data to file").hasArg()
                .argName("OUTPUT FILE PATH").build());

        options.addOption(Option.builder("i").longOpt("option-print").desc("print all option data to file").hasArg()
                .argName("OUTPUT FILE PATH").build());

        options.addOption(Option.builder("r").longOpt("descriptor-print").desc("print all descriptor data to file")
                .hasArg().argName("OUTPUT FILE PATH").build());

        options.addOption(Option.builder().longOpt("xml-root").desc("set xml root node name.(default: root)").hasArg()
                .argName("ROOT NAME").build());

        options.addOption(Option.builder().longOpt("javascript-export")
                .desc("set javascript export mode(nodejs, amd or global)").hasArg().argName("EXPORT MODE").build());

        options.addOption(Option.builder().longOpt("javascript-global")
                .desc("set javascript export namespace of window or global").hasArg().argName("NAME").build());

        options.addOption(null, "disable-excel-formular", false,
                "disable formular in excel. will be faster when convert data.");
        options.addOption(null, "enable-excel-formular", false,
                "[default] enable formular in excel. will be slower when convert data.");
        options.addOption(null, "disable-empty-list", false,
                "[deprecated] remove empty elements in a list or repeated field,please use --list-strip-all-empty.");
        options.addOption(null, "enable-empty-list", false,
                "[deprecated] keep empty elements in a list or repeated field with default value,please use --list-keep-empty.");
        options.addOption(null, "list-strip-all-empty", false,
                "[default] remove all empty elements in a list or repeated field.");
        options.addOption(null, "list-keep-empty", false,
                "keep empty elements in a list or repeated field with default value.");
        options.addOption(null, "list-strip-empty-tail", false,
                "remove all tail empty elements in a list or repeated field.");
        options.addOption(null, "stdin", false, "enable read from stdin and convert more files.");
        options.addOption(null, "lua-global", false, "add data to _G if in lua mode when print const data");
        options.addOption(null, "lua-module", true, "module(MODULE_NAME, package.seeall) if in lua mode");
        options.addOption(Option.builder().longOpt("validator-rules")
                .desc("set file to load custom validator").hasArg().argName("FILE PATH").build());
        options.addOption(null, "disable-data-validator", false,
                "disable data validator, so it will not show warnings when data checking failed.");
        options.addOption(Option.builder().longOpt("ignore-field-tags")
                .desc("ignore data and only dump the default value of specify field tags.").hasArg().argName("TAG")
                .build());
        options.addOption(Option.builder().longOpt("data-source-lru-cache-rows")
                .desc("set row number for LRU cache").hasArg().argName("NUMBER").build());
        options.addOption(Option.builder().longOpt("tolerate-max-empty-rows")
                .desc("set max continue empty rows").hasArg().argName("NUMBER").build());

        options.addOption(Option.builder().longOpt("data-source-mapping-file")
                .desc("set where to store data source mapping result").hasArg().argName("FILE PATH").build());

        options.addOption(Option.builder().longOpt("data-source-mapping-mode")
                .desc("set where to store data source mapping mode(none, md5, sha1, sha256)").hasArg()
                .argName("ALGORITHM").build());

        options.addOption(Option.builder().longOpt("data-source-mapping-seed")
                .desc("set data source mapping seed").hasArg()
                .argName("SEED").build());

        return options;
    }

    public int init(String[] args) {
        reset();

        // create parser
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            // parse the command line arguments
            Options options = get_options_group();
            synchronized (options) {
                cmd = parser.parse(get_options_group(), args);
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            ProgramOptions.getLoger().error("Parsing failed.  reason: \"%s\" failed", exp.getMessage());

            String script = System.getProperty("java.class.path");
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(140);
            formatter.printHelp("Usage: java -client -jar " + script + " [options...]", options);
            System.out.println("");
            System.out.println("You can add -Dlog4j.configuration=log4j2.xml to use your own log configure.");
            return -1;
        }

        if (cmd.hasOption('h')) {
            String script = System.getProperty("java.class.path");
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(140);
            Options options = get_options_group();
            synchronized (options) {
                formatter.printHelp(String.format("java -client -jar \"%s\" [options...]", script),
                        options);
            }
            System.out.println("");
            System.out.println("You can add -Dlog4j.configuration=log4j2.xml to use your own log configure.");
            return 1;
        }

        if (cmd.hasOption("stdin")) {
            enableStdin = true;
        }

        if (cmd.hasOption('a')) {
            if (enableStdin) {
                defaultDataVersion = cmd.getOptionValue('a', "");
            } else {
                dataVersion = cmd.getOptionValue('a', "");
            }
        }

        if (cmd.hasOption('v')) {
            System.out.println(getVersion());
            return 1;
        }

        if (cmd.hasOption("disable-string-macro")) {
            enableStringMacro = false;
        } else if (cmd.hasOption("enable-string-macro")) {
            enableStringMacro = true;
        }

        if (cmd.hasOption("enable-empty-list") || cmd.hasOption("list-keep-empty")) {
            stripListRule = ListStripRule.KEEP_ALL;
        } else if (cmd.hasOption("list-strip-empty-tail")) {
            stripListRule = ListStripRule.STRIP_EMPTY_TAIL;
        } else {
            stripListRule = ListStripRule.STRIP_EMPTY_ALL;
        }

        // target type
        if (cmd.hasOption('t')) {
            String val = cmd.getOptionValue('t');
            if (val.equalsIgnoreCase("bin")) {
                outType = FileType.BIN;
            } else if (val.equalsIgnoreCase("lua")) {
                outType = FileType.LUA;
            } else if (val.equalsIgnoreCase("msgpack")) {
                outType = FileType.MSGPACK;
            } else if (val.equalsIgnoreCase("json")) {
                outType = FileType.JSON;
            } else if (val.equalsIgnoreCase("xml")) {
                outType = FileType.XML;
            } else if (val.equalsIgnoreCase("js") || val.equalsIgnoreCase("javascript")) {
                outType = FileType.JAVASCRIPT;
            } else if (val.equalsIgnoreCase("ue-csv")) {
                outType = FileType.UECSV;
            } else if (val.equalsIgnoreCase("ue-json")) {
                outType = FileType.UEJSON;
            } else {
                ProgramOptions.getLoger().error("Invalid output type ", val);
                return -1;
            }
        }

        // protocol type
        if (cmd.hasOption('p')) {
            String val = cmd.getOptionValue('p');
            if (val.equalsIgnoreCase("protobuf")) {
                protocol = Protocol.PROTOBUF;
            } else if (val.equalsIgnoreCase("capnproto")) {
                protocol = Protocol.CAPNPROTO;
            } else if (val.equalsIgnoreCase("flatbuffer")) {
                protocol = Protocol.FLATBUFFER;
            } else {
                ProgramOptions.getLoger().error("Invalid protocol type ", val);
                return -2;
            }
        }

        // protocol file
        protocolFile = cmd.getOptionValues('f');
        if (protocolFile == null) {
            return 1;
        }
        if (protocolFile.length == 0) {
            return 1;
        }

        protocolIgnoreUnknownDependency = cmd.hasOption("ignore-unknown-dependency");

        luaGlobal = cmd.hasOption("lua-global");
        luaModule = cmd.getOptionValue("lua-module", (String) null);
        xmlRootName = cmd.getOptionValue("xml-root", xmlRootName);
        javascriptExport = cmd.getOptionValue("javascript-export", javascriptExport);
        javascriptGlobalVar = cmd.getOptionValue("javascript-global", javascriptGlobalVar);

        // output dir
        outputDirectory = cmd.getOptionValue('o', ".");
        // data sorce dir
        dataSourceDirectory = cmd.getOptionValues('d');
        if (dataSourceDirectory == null || dataSourceDirectory.length == 0) {
            dataSourceDirectory = new String[] { "." };
        }
        if (cmd.hasOption("data-source-lru-cache-rows")) {
            dataSourceLruCacheRows = Integer.parseInt(cmd.getOptionValue("data-source-lru-cache-rows", "300000"));
        }
        if (cmd.hasOption("tolerate-max-empty-rows")) {
            tolerateContinueEmptyRows = Integer.parseInt(cmd.getOptionValue("tolerate-max-empty-rows", "100000"));
        }
        if (cmd.hasOption("data-source-mapping-file")) {
            String filePath = cmd.getOptionValue("data-source-mapping-file", "");
            if (!filePath.isEmpty()) {
                synchronized (dataSourceMappingFile) {
                    dataSourceMappingFile = filePath;
                }
            }
        }
        if (cmd.hasOption("data-source-mapping-mode")) {
            String mode = cmd.getOptionValue("data-source-mapping-mode", "none");
            if (mode.equalsIgnoreCase("md5")) {
                dataSourceMappingType = DataSourceMappingType.MD5;
            } else if (mode.equalsIgnoreCase("sha1")) {
                dataSourceMappingType = DataSourceMappingType.SHA1;
            } else if (mode.equalsIgnoreCase("sha256")) {
                dataSourceMappingType = DataSourceMappingType.SHA256;
            } else if (mode.equalsIgnoreCase("none") || mode.isEmpty()) {
                dataSourceMappingType = DataSourceMappingType.NONE;
            } else {
                ProgramOptions.getLoger().warn("Unknown --data-source-mapping-mode %s.", mode);
                dataSourceMappingType = DataSourceMappingType.NONE;
            }
        }
        if (cmd.hasOption("data-source-mapping-seed")) {
            synchronized (dataSourceMappingHashSeed) {
                dataSourceMappingHashSeed = cmd.getOptionValue("data-source-mapping-seed", "xresloader");
            }
        }

        // pretty print
        prettyIndent = Integer.parseInt(cmd.getOptionValue("pretty", "0"));

        // const print
        if (cmd.hasOption('c')) {
            protoDumpFile = cmd.getOptionValue('c');
            protoDumpType = ProtoDumpType.CONST;
            return 0;
        }

        // option print
        if (cmd.hasOption('r')) {
            protoDumpFile = cmd.getOptionValue('r');
            protoDumpType = ProtoDumpType.DESCRIPTOR;
        } else if (cmd.hasOption('i')) {
            protoDumpFile = cmd.getOptionValue('i');
            protoDumpType = ProtoDumpType.OPTIONS;
            return 0;
        }

        // macro source file path
        if (cmd.hasOption('s')) {
            dataSourceFile = cmd.getOptionValue('s');
            int dot_index = dataSourceFile.lastIndexOf('.');

            String name_suffix = dot_index >= 0 && dot_index < dataSourceFile.length() - 1
                    ? dataSourceFile.substring(dot_index + 1)
                    : null;
            if (null != name_suffix && (name_suffix.equalsIgnoreCase("xls") || name_suffix.equalsIgnoreCase("xlsx")
                    || name_suffix.equalsIgnoreCase("cvs") || name_suffix.equalsIgnoreCase("xlsm")
                    || name_suffix.equalsIgnoreCase("ods"))) {
                dataSourceType = FileType.EXCEL;

            } else if (null != name_suffix && (name_suffix.equalsIgnoreCase("ini")
                    || name_suffix.equalsIgnoreCase("cfg") || name_suffix.equalsIgnoreCase("conf"))) {
                dataSourceType = FileType.INI;
            } else if (null != name_suffix && name_suffix.equalsIgnoreCase("json")) {
                dataSourceType = FileType.JSON;
                // } else if (null != name_suffix && name_suffix.equalsIgnoreCase("lua")) {
                // dataSourceType = FileType.LUA;
                // } else if (null != name_suffix && name_suffix.equalsIgnoreCase("xml")) {
                // dataSourceType = FileType.XML;
            }
        }

        // macro names
        dataSourceMetas = cmd.getOptionValues('m');

        if (cmd.hasOption('l')) {
            dataSourceMetaDelimiter = cmd.getOptionValue('l');
        }

        // rename rules
        if (cmd.hasOption('n')) {
            do {
                String rule_string = cmd.getOptionValue('n');
                rule_string = rule_string.trim();
                if (rule_string.isEmpty()) {
                    ProgramOptions.getLoger().error("Invalid rename rule %s", rule_string);
                    break;
                }

                if (rule_string.length() >= 2 && (rule_string.charAt(0) == '"' || rule_string.charAt(0) == '\'')
                        && rule_string.charAt(0) == rule_string.charAt(rule_string.length() - 1)) {
                    rule_string = rule_string.substring(1, rule_string.length() - 1);
                }

                String[] groups = rule_string.split(rule_string.substring(0, 1));
                int start_index = 0;
                for (; start_index < groups.length; ++start_index) {
                    if (groups[start_index].isEmpty())
                        continue;
                    break;
                }

                if (groups.length < start_index + 2) {
                    ProgramOptions.getLoger().error("Invalid rename rule %s", rule_string);
                    break;
                }

                Pattern match_rule = null;
                try {
                    match_rule = Pattern.compile(groups[start_index]);
                } catch (PatternSyntaxException e) {
                    ProgramOptions.getLoger().error("Invalid rename regex rule %s", groups[start_index]);
                    break;
                }

                renameRule = new RenameRule();
                renameRule.match = match_rule;
                renameRule.replace = groups[start_index + 1];
            } while (false);
        }

        if (cmd.hasOption("require-mapping-all")) {
            requireMappingAllFields = true;
        }

        if (cmd.hasOption("enable-alias-mapping")) {
            enableAliasMapping = true;
        }

        if (cmd.hasOption("disable-alias-mapping")) {
            enableAliasMapping = false;
        }

        // special functions
        if (cmd.hasOption("disable-excel-formular")) {
            enableFormular = false;
        } else if (cmd.hasOption("enable-excel-formular")) {
            enableFormular = true;
        }

        // custom validator rule file
        customValidatorRules = cmd.getOptionValues("validator-rules");

        if (cmd.hasOption("disable-data-validator")) {
            enableDataValidator = false;
        } else {
            enableDataValidator = true;
        }

        // ignore field tags
        ignoreFieldTags = cmd.getOptionValues("ignore-field-tags");

        return 0;
    }

    public static synchronized Properties getProperties() {
        if (properties != null) {
            return properties;
        }

        properties = new Properties();
        try {
            InputStream inCfg = getInstance().getClass().getClassLoader().getResourceAsStream("application.properties");
            properties.load(inCfg);
            inCfg.close();
        } catch (IOException e) {
            ProgramOptions.getLoger().error("Get properties file application.properties failed.\n%s", e.getMessage());
        }

        return properties;
    }

    public synchronized String getVersion() {
        if (version == null) {
            version = getProperties().getProperty("version", "Unknown");
        }
        return version;
    }

    public String getDataVersion() {
        if (dataVersion == null || dataVersion.isEmpty()) {
            dataVersion = getDefaultDataVersion();
        }
        return dataVersion;
    }

    public String getDefaultDataVersion() {
        if (defaultDataVersion == null || defaultDataVersion.isEmpty()) {
            defaultDataVersion = String.format("%s.%s", getVersion(),
                    new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()));
        }
        return defaultDataVersion;
    }

    public String getDataSourceMapping(String input, boolean pathMode) {
        if (DataSourceMappingType.NONE == dataSourceMappingType) {
            return input;
        }

        if (null == input) {
            return input;
        }

        if (input.isEmpty()) {
            return input;
        }

        if (pathMode) {
            input = input.replaceAll("\\\\", "/");
        }

        synchronized (dataSourceMappingResult) {
            if (dataSourceMappingResult.containsKey(input)) {
                return dataSourceMappingResult.get(input);
            }

            MessageDigest hasher = null;
            try {
                if (DataSourceMappingType.MD5 == dataSourceMappingType) {
                    hasher = MessageDigest.getInstance("MD5");
                } else if (DataSourceMappingType.SHA1 == dataSourceMappingType) {
                    hasher = MessageDigest.getInstance("SHA-1");
                } else {
                    hasher = MessageDigest.getInstance("SHA-256");
                }

                synchronized (dataSourceMappingHashSeed) {
                    if (dataSourceMappingHashSeed != null && !dataSourceMappingHashSeed.equals(input)) {
                        hasher.update(dataSourceMappingHashSeed.getBytes());
                    }
                }
                hasher.update(input.getBytes(Charset.forName("UTF-8")));
                String result = Hex.encodeHexString(hasher.digest());
                dataSourceMappingResult.put(result, input);
                return result;
            } catch (NoSuchAlgorithmException e) {
                ProgramOptions.getLoger().error("%s", e.getMessage());
                return input;
            }
        }
    }

    public enum FileType {
        BIN, LUA, MSGPACK, JSON, XML, INI, EXCEL, JAVASCRIPT, UECSV, UEJSON
    }

    public enum Protocol {
        PROTOBUF, CAPNPROTO, FLATBUFFER
    }

    public enum ProtoDumpType {
        NONE, CONST, OPTIONS, DESCRIPTOR
    }

    public enum ListStripRule {
        STRIP_EMPTY_ALL, STRIP_EMPTY_TAIL, KEEP_ALL,
    }

    public enum DataSourceMappingType {
        NONE, MD5, SHA1, SHA256
    }

    static public synchronized Logger getLoger() {
        if (null != logger) {
            return logger;
        }

        String name = getProperties().getProperty("name", "xresloader");

        try {
            logger = LogManager.getFormatterLogger(name);
        } catch (UnsupportedCharsetException e) {
            System.err.println(String.format(
                    "[WARN] Unknown console charset %s, we will try use UTF-8 for console output",
                    e.getCharsetName()));
        }

        return logger;
    }

    static public String getReportUrl() {
        return getProperties().getProperty("report", "https://github.com/xresloader/xresloader/issues");
    }

    static public String getHomeUrl() {
        return getProperties().getProperty("home", "https://xresloader.atframe.work/");
    }

    static public String getEndl() {
        if (endl == null) {
            endl = System.getProperty("line.separator", "\n");
        }

        return endl;
    }

    static public int dumpDataSourceMapping() {
        int ret = 0;
        OutputStream out = null;
        File file = null;
        synchronized (dataSourceMappingFile) {
            if (dataSourceMappingFile.isEmpty()) {
                return ret;
            }
            file = new File(dataSourceMappingFile);
        }

        Charset utf8 = Charset.forName("UTF-8");

        HashMap<String, String> dataSourceMappingOrigin = new HashMap<String, String>();
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                Scanner s = new Scanner(in, utf8);
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length < 2) {
                        continue;
                    }
                    if (parts[0].isEmpty()) {
                        continue;
                    }

                    dataSourceMappingOrigin.put(parts[0], parts[1]);
                }
                s.close();
                in.close();
            } catch (IOException e) {
                ProgramOptions.getLoger().error("Load data source mapping file %s failed.\n%s", dataSourceMappingFile,
                        e.getMessage());
                ret += 1;
            }
        }

        try {
            File dir = new File(file.getParent());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            out = new FileOutputStream(file);
        } catch (IOException e) {
            ProgramOptions.getLoger().error("Open data source mapping file %s failed.\n%s", dataSourceMappingFile,
                    e.getMessage());
            ret += 1;
            return ret;
        }

        ArrayList<String> keys = new ArrayList<String>();

        synchronized (dataSourceMappingResult) {
            for (String key : dataSourceMappingResult.keySet()) {
                if (null == key) {
                    continue;
                }
                if (key.isEmpty()) {
                    continue;
                }
                keys.add(key);
            }

            for (String key : dataSourceMappingOrigin.keySet()) {
                if (null == key) {
                    continue;
                }
                if (key.isEmpty()) {
                    continue;
                }
                if (!dataSourceMappingResult.containsKey(key)) {
                    keys.add(key);
                }
            }

            keys.sort((l, r) -> {
                return l.compareTo(r);
            });

            for (String key : keys) {
                try {
                    out.write(key.getBytes(utf8));
                    out.write(" ".getBytes(utf8));
                    if (dataSourceMappingResult.containsKey(key)) {
                        out.write(dataSourceMappingResult.get(key).getBytes(utf8));
                    } else {
                        out.write(dataSourceMappingOrigin.get(key).getBytes(utf8));
                    }
                    out.write(getEndl().getBytes());
                } catch (IOException e) {
                    ProgramOptions.getLoger().error("Write data source mapping %s failed.\n%s", key, e.getMessage());
                    ret += 1;
                }
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            ProgramOptions.getLoger().error("Close data source mapping file %s failed.\n%s",
                    file.getAbsoluteFile().getPath(),
                    e.getMessage());
            ret += 1;
        }
        return ret;
    }
}
