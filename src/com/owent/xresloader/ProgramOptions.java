package com.owent.xresloader;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.FileDescriptor;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ProgramOptions {

    public class RenameRule {
        public Pattern match;
        public String replace;
    }

    /**
     * 单例
     */
    private static ProgramOptions instance = null;

    public FileType outType;

    public Protocol protocol;
    public String protocolFile = "";
    public String outputDirectory = "";
    public String dataSourceDirectory = "";
    public String dataSourceFile = "";
    public FileType dataSourceType;
    public List<String> dataSourceMetas = null;
    public RenameRule renameRule = null;
    public boolean enableFormular = true;
    public boolean enbleEmptyList = false;
    public int prettyIndent = 0;

    private ProgramOptions() {
        dataSourceMetas = new LinkedList<String>();
        outType = FileType.BIN;
        protocol = Protocol.PROTOBUF;

        outputDirectory = System.getProperty("user.dir");
        dataSourceDirectory = outputDirectory;
        dataSourceType = FileType.EXCEL;
    }

    public static ProgramOptions getInstance() {
        if (instance == null) {
            instance = new ProgramOptions();
        }
        return instance;
    }

    public int init(String[] args) {
        StringBuffer sb = new StringBuffer();
        String script = System.getProperty("user.scripts");

        LongOpt[] long_opts = new LongOpt[]{
                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
                new LongOpt("output-type", LongOpt.REQUIRED_ARGUMENT, sb, 't'),
                new LongOpt("proto", LongOpt.REQUIRED_ARGUMENT, sb, 'p'),
                new LongOpt("proto-file", LongOpt.REQUIRED_ARGUMENT, sb, 'f'),
                new LongOpt("output-dir", LongOpt.REQUIRED_ARGUMENT, sb, 'o'),
                new LongOpt("data-src-dir", LongOpt.REQUIRED_ARGUMENT, sb, 'd'),
                new LongOpt("src-file", LongOpt.REQUIRED_ARGUMENT, sb, 's'),
                new LongOpt("src-meta", LongOpt.REQUIRED_ARGUMENT, sb, 'm'),
                new LongOpt("version", LongOpt.NO_ARGUMENT, sb, 'v'),
                new LongOpt("rename", LongOpt.REQUIRED_ARGUMENT, sb, 'n'),
                new LongOpt("pretty", LongOpt.REQUIRED_ARGUMENT, sb, 0),
                new LongOpt("disable-excel-formular", LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt("enable-excel-formular", LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt("disable-empty-list", LongOpt.NO_ARGUMENT, null, 0),
                new LongOpt("enable-empty-list", LongOpt.NO_ARGUMENT, null, 0)
        };

        Getopt g = new Getopt("", args, "ht:p:f:o:d:s:m:vn:", long_opts);
        g.setOpterr(false);

        int c;
        while ((c = g.getopt()) != -1) {

            char cc = (char) c;
            if (0 == c) {
                cc = (char) long_opts[g.getLongind()].getVal();
            }
            switch (cc) {
                case 'h': {
                    System.out.println("Usage: java -jar " + script + " [options...]");
                    System.out.println("-h, --help                              show this help message and exit");
                    System.out.println("-t, --output-type <type>                output type(bin, lua, msgpack, json, xml)");
                    System.out.println("-p, --proto <protocol type name>        protocol(protobuf)");
                    System.out.println("-f, --proto-file <file name>            protocol description file");
                    System.out.println("-o, --output-dir <output directory>     output directory");
                    System.out.println("-d, --data-src-dir <source directory>   data source directory(where to find excel specified by meta) ");
                    System.out.println("-s, --src-file <meta file name>         data source file(.xls, .xlsx, .cvs, .xlsm, .ods, .ini, .cfg, .conf, .json)");
                    System.out.println("-m, --src-meta <meta name>              data description meta");
                    System.out.println("-v, --version                           print version and exit");
                    System.out.println("-n, --rename <pattern>                  rename output file name(regex), sample: /(?i)\\.bin$/\\.lua/");
                    System.out.println("--pretty <ident length>                 set pretty output and set ident length when output type supported.(disable pretty output by set to 0)");
                    System.out.println("Control options:");
                    System.out.println("--disable-excel-formular                disable formular in excel. will be faster when convert data.");
                    System.out.println("--enable-excel-formular                 [default] enable formular in excel. will be slower when convert data.");
                    System.out.println("--disable-empty-list                    [default] remove empty elements in a list or repeated field.");
                    System.out.println("--enable-empty-list                     keep empty elements in a list or repeated field with default value.");
                    System.exit(0);
                    break;
                }
                case 't': {
                    String val = g.getOptarg();
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
                        System.err.println("[ERROR] invalid output type " + sb.toString());
                        return -1;
                    }
                    break;
                }

                case 'p': {
                    String val = g.getOptarg();
                    if (val.equalsIgnoreCase("protobuf")) {
                        protocol = Protocol.PROTOBUF;

                        //} else if (val.equalsIgnoreCase("capnproto")){
                        //} else if (val.equalsIgnoreCase("flatbuffer")){
                    } else {
                        System.err.println("[ERROR] invalid protocol type " + sb.toString());
                        return -2;
                    }

                    break;
                }

                case 'f': {
                    protocolFile = g.getOptarg();
                    break;
                }

                case 'o': {
                    outputDirectory = g.getOptarg();
                    break;
                }

                case 'd': {
                    dataSourceDirectory = g.getOptarg();
                    break;
                }

                case 's': {
                    dataSourceFile = g.getOptarg();
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
//                } else if (null != name_suffix && name_suffix.equalsIgnoreCase("lua")) {
//                    dataSourceType = FileType.LUA;
//                } else if (null != name_suffix && name_suffix.equalsIgnoreCase("xml")) {
//                    dataSourceType = FileType.XML;
                    }

                    break;
                }

                case 'm': {
                    dataSourceMetas.add(g.getOptarg());
                    break;
                }

                case 'v': {
                    System.out.println(getVersion());
                    System.exit(0);
                    break;
                }

                case 'n': {
                    String rule_string = g.getOptarg();
                    rule_string = rule_string.trim();
                    if (rule_string.isEmpty()) {
                        System.err.println(String.format("[ERROR] Invalid rename rule %s", rule_string));
                        break;
                    }

                    String[] groups = rule_string.split(rule_string.substring(0, 1));
                    int start_index = 0;
                    for (; start_index < groups.length; ++ start_index) {
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

                    break;
                }

                default: {
                    int lindex = g.getLongind();
                    if (lindex >= 0 && lindex < long_opts.length) {
                        String long_opt_name = long_opts[lindex].getName();
                        if (long_opt_name.equals("pretty")) {
                            if(null != g.getOptarg()) {
                                prettyIndent = Integer.parseInt(g.getOptarg());
                            } else {
                                prettyIndent = 0;
                            }
                        } else if (long_opt_name.equals("disable-excel-formular")) {
                            enableFormular = false;
                        } else if (long_opt_name.equals("enable-excel-formular")) {
                            enableFormular = true;
                        } else if (long_opt_name.equals("disable-empty-list")) {
                            enbleEmptyList = false;
                        } else if (long_opt_name.equals("enable-empty-list")) {
                            enbleEmptyList = true;
                        } else {
                            System.out.println(String.format("[WARN] Unknown option %s", long_opt_name));
                        }
                    } else {
                        System.out.println(String.format("[WARN] Unknown option %d", g.getOptarg()));
                    }

                    break;
                }
            }
        }

        return 0;
    }

    public String getVersion() {
        return "1.0.1.0";
    }


    public enum FileType {BIN, LUA, MSGPACK, JSON, XML, INI, EXCEL}

    public enum Protocol {PROTOBUF, CAPNPROTO, FLATBUFFER}
}
