package com.owent.xresloader;


import java.io.FileDescriptor;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.*;
import org.ini4j.spi.OptionsBuilder;

public class ProgramOptions {

    public class RenameRule {
        public Pattern match;
        public String replace;
    }

    /**
     * 单例
     */
    private static ProgramOptions instance = null;
    private static Options options = null;

    public FileType outType;

    public Protocol protocol;
    public String protocolFile = "";
    public String outputDirectory = ".";
    public String dataSourceDirectory = ".";
    public String dataSourceFile = "";
    public FileType dataSourceType;
    public String[] dataSourceMetas = null;
    public RenameRule renameRule = null;
    public boolean enableFormular = true;
    public boolean enbleEmptyList = false;
    public int prettyIndent = 0;
    public boolean enableStdin = false;

    public String constPrint = "";
    public boolean luaGlobal = false;
    public String xmlRootName = "root";

    private ProgramOptions() {
        dataSourceMetas = new String[]{};
        outType = FileType.BIN;
        protocol = Protocol.PROTOBUF;

        outputDirectory = System.getProperty("user.dir");
        dataSourceDirectory = outputDirectory;
        dataSourceType = FileType.EXCEL;
    }

    public static ProgramOptions getInstance() {
        if (instance == null) {
            instance = new ProgramOptions();
            instance.reset();
        }

        return instance;
    }

    public void reset() {
        outType = FileType.BIN;
        protocol = Protocol.PROTOBUF;
        protocolFile = "";
        outputDirectory = "";
        dataSourceDirectory = "";
        dataSourceFile = "";
        dataSourceType = FileType.EXCEL;
        dataSourceMetas = null;
        renameRule = null;
        enableFormular = true;
        enbleEmptyList = false;
        prettyIndent = 0;
        enableStdin = false;
        constPrint = "";
        luaGlobal = false;
        xmlRootName = "root";
    }

    private static Options get_options_group() {
        if (null != options) {
            return options;
        }

        // create the Options
        options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");

        options.addOption(Option.builder("t")
                .longOpt("output-type")
                .desc("output type(bin, lua, msgpack, json, xml)")
                .hasArg()
                .argName("TYPE")
                .build()
        );

        options.addOption(Option.builder("p")
                .longOpt("proto")
                .desc("protocol(protobuf)")
                .hasArg()
                .argName("PROTOCOL")
                .build()
        );

        options.addOption(Option.builder("f")
                .longOpt("proto-file")
                .desc("protocol description file")
                .hasArg()
                .argName("FILE NAME")
                .build()
        );

        options.addOption(Option.builder("o")
                .longOpt("output-dir")
                .desc("output directory")
                .hasArg()
                .argName("DIRECTORY PATH")
                .build()
        );

        options.addOption(Option.builder("d")
                .longOpt("data-src-dir")
                .desc("data source directory(where to find excel specified by meta)")
                .hasArg()
                .argName("DIRECTORY PATH")
                .build()
        );

        options.addOption(Option.builder("s")
                .longOpt("src-file")
                .desc("data source file(.xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json)")
                .hasArg()
                .argName("META FILE PATH")
                .build()
        );

        options.addOption(Option.builder("m")
                .longOpt("src-meta")
                .desc("data description meta")
                .hasArg()
                .argName("META NAME")
                .build()
        );

        options.addOption("v", "version", false, "print version and exit");

        options.addOption(Option.builder("n")
                .longOpt("rename")
                .desc("rename output file name(regex), sample: /(?i)\\.bin$/\\.lua/")
                .hasArg()
                .argName("RENAME PATTERN")
                .build()
        );

        options.addOption(Option.builder()
                .longOpt("pretty")
                .desc("set pretty output and set ident length when output type supported.(disable pretty output by set to 0)")
                .hasArg()
                .argName("IDENT LENGTH")
                .build()
        );

        options.addOption(Option.builder("c")
                .longOpt("const-print")
                .desc("print all const data to file")
                .hasArg()
                .argName("OUTPUT FILE PATH")
                .build()
        );

        options.addOption(Option.builder()
                .longOpt("xml-root")
                .desc("set xml root node name.(default: root)")
                .hasArg()
                .argName("ROOT NAME")
                .build()
        );

        options.addOption(null, "disable-excel-formular", false, "disable formular in excel. will be faster when convert data.");
        options.addOption(null, "enable-excel-formular", false, "[default] enable formular in excel. will be slower when convert data.");
        options.addOption(null, "disable-empty-list", false, "[default] remove empty elements in a list or repeated field.");
        options.addOption(null, "enable-empty-list", false, "keep empty elements in a list or repeated field with default value.");
        options.addOption(null, "stdin", false, "enable read from stdin and convert more files.");
        options.addOption(null, "lua-global", false, "add data to _G if in lua mode when print const data");

        return options;
    }

    public int init(String[] args) {
        reset();

        // create parser
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            // parse the command line arguments
            cmd = parser.parse( get_options_group(), args );
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println(String.format("[ERROR] parsing failed.  reason: \"%s\" failed", exp.getMessage()));

            String script = System.getProperty("java.class.path");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Usage: java -client -jar " + script + " [options...]", options);
            return -1;
        }

        if (cmd.hasOption('h')) {
            String script = System.getProperty("java.class.path");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(String.format("java -client -jar \"%s\" [options...]", script), get_options_group());
            return 1;
        }

        if (cmd.hasOption('v')) {
            System.out.println(getVersion());
            return 1;
        }

        if (cmd.hasOption("stdin")) {
            enableStdin = true;
        }

        if (cmd.hasOption("disable-empty-list")) {
            enbleEmptyList = false;
        } else if(cmd.hasOption("enable-empty-list")) {
            enbleEmptyList = true;
        }

        // target type
        if (cmd.hasOption('t')) {
            String val = cmd.getOptionValue('t');
            if (val.equalsIgnoreCase("bin")) {
                outType = FileType.BIN;
            } else if (val.equalsIgnoreCase("lua")) {
                outType = FileType.LUA;
            } else if (val.equalsIgnoreCase("msgpack")){
                outType = FileType.MSGPACK;
            } else if (val.equalsIgnoreCase("json")){
                outType = FileType.JSON;
            } else if (val.equalsIgnoreCase("xml")){
                outType = FileType.XML;
            } else {
                System.err.println(String.format("[ERROR] [ERROR] invalid output type ", val));
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
            } else if (val.equalsIgnoreCase("flatbuffer")){
                protocol = Protocol.FLATBUFFER;
            } else {
                System.err.println(String.format("[ERROR] [ERROR] invalid protocol type ", val));
                return -2;
            }
        }

        // protocol file
        protocolFile = cmd.getOptionValue('f', "");
        if (protocolFile.isEmpty()) {
            return 1;
        }

        luaGlobal = cmd.hasOption("lua-global");
        xmlRootName = cmd.getOptionValue("xml-root", xmlRootName);

        // output dir
        outputDirectory = cmd.getOptionValue('o', ".");
        // data sorce dir
        dataSourceDirectory = cmd.getOptionValue('d', ".");

        // pretty print
        prettyIndent = Integer.parseInt(cmd.getOptionValue("pretty", "0"));

        // const print
        if (cmd.hasOption('c')) {
            constPrint = cmd.getOptionValue('c');
            return 0;
        }

        // macro source file path
        if (cmd.hasOption('s')) {
            dataSourceFile = cmd.getOptionValue('s');
            int dot_index = dataSourceFile.lastIndexOf('.');

            String name_suffix = dot_index >= 0 && dot_index < dataSourceFile.length() - 1? dataSourceFile.substring(dot_index + 1) : null;
            if (null != name_suffix && (
                    name_suffix.equalsIgnoreCase("xls") ||
                            name_suffix.equalsIgnoreCase("xlsx") ||
                            name_suffix.equalsIgnoreCase("cvs") ||
                            name_suffix.equalsIgnoreCase("xlsm") ||
                            name_suffix.equalsIgnoreCase("ods")
            )) {
                dataSourceType = FileType.EXCEL;

            } else if (null != name_suffix && (
                    name_suffix.equalsIgnoreCase("ini") ||
                    name_suffix.equalsIgnoreCase("cfg") ||
                    name_suffix.equalsIgnoreCase("conf")
            )) {
                dataSourceType = FileType.INI;
            } else if (null != name_suffix && name_suffix.equalsIgnoreCase("json")) {
                dataSourceType = FileType.JSON;
//          } else if (null != name_suffix && name_suffix.equalsIgnoreCase("lua")) {
//              dataSourceType = FileType.LUA;
//          } else if (null != name_suffix && name_suffix.equalsIgnoreCase("xml")) {
//              dataSourceType = FileType.XML;
            }
        } else {
            return 1;
        }

        // macro names
        dataSourceMetas = cmd.getOptionValues('m');

        // rename rules
        if (cmd.hasOption('n')) {
            do {
                String rule_string = cmd.getOptionValue('n');
                rule_string = rule_string.trim();
                if (rule_string.isEmpty()) {
                    System.err.println(String.format("[ERROR] Invalid rename rule %s", rule_string));
                    break;
                }

                String[] groups = rule_string.split(rule_string.substring(0, 1));
                int start_index = 0;
                for (; start_index < groups.length; ++start_index) {
                    if (groups[start_index].isEmpty())
                        continue;
                    break;
                }

                if (groups.length < start_index + 2) {
                    System.err.println(String.format("[ERROR] Invalid rename rule %s", rule_string));
                    break;
                }

                Pattern match_rule = null;
                try {
                    match_rule = Pattern.compile(groups[start_index]);
                } catch (PatternSyntaxException e) {
                    System.err.println(String.format("[ERROR] Invalid rename regex rule %s", groups[start_index]));
                    break;
                }

                renameRule = new RenameRule();
                renameRule.match = match_rule;
                renameRule.replace = groups[start_index + 1];
            } while(false);
        }

        // special functions
        if (cmd.hasOption("disable-excel-formular")) {
            enableFormular = false;
        } else if(cmd.hasOption("enable-excel-formular")) {
            enableFormular = true;
        }

        if (cmd.hasOption("disable-empty-list")) {
            enbleEmptyList = false;
        } else if(cmd.hasOption("enable-empty-list")) {
            enbleEmptyList = true;
        }

        return 0;
    }

    public String getVersion() {
        return "1.0.4.0";
    }


    public enum FileType {BIN, LUA, MSGPACK, JSON, XML, INI, EXCEL}

    public enum Protocol {PROTOBUF, CAPNPROTO, FLATBUFFER}
}
