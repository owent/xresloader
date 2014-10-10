package com.owent.xresloader;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.LinkedList;
import java.util.List;

public class ProgramOptions {

	public enum FileType { BIN, LUA, JSON, XML, INI, EXCEL};
	public enum Protocol { PROTOBUF, CAPNPROTO, FLATBUFFER};
	
	public FileType outType;
	public Protocol protocol;
	public String protocolFile = ""; 
	public String outputDirectory = "";
	public String dataSourceDirectory = "";
	public String dataSourceFile = "";
    public FileType dataSourceType;
	public List<String> dataSourceMetas = null;
	
	/**
	 * 单例
	 */
	private static ProgramOptions instance = null;
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
		StringBuffer sb =  new StringBuffer();
		String script = System.getProperty("user.scripts");

        LongOpt[] long_opts = new LongOpt[]{
                new LongOpt("help", 		LongOpt.NO_ARGUMENT, null, 'h'),
                new LongOpt("output-type", 	LongOpt.REQUIRED_ARGUMENT, sb, 't'),
                new LongOpt("proto", 		LongOpt.REQUIRED_ARGUMENT, sb, 'p'),
                new LongOpt("proto-file", 	LongOpt.REQUIRED_ARGUMENT, sb, 'f'),
                new LongOpt("output-dir", 	LongOpt.REQUIRED_ARGUMENT, sb, 'o'),
                new LongOpt("data-src-dir", LongOpt.REQUIRED_ARGUMENT, sb, 'd'),
                new LongOpt("src-file", 	LongOpt.REQUIRED_ARGUMENT, sb, 's'),
                new LongOpt("src-meta", 	LongOpt.REQUIRED_ARGUMENT, sb, 'm')
        };

		Getopt g = new Getopt("", args, "ht:p:f:o:d:s:m:", long_opts);
		g.setOpterr(false);
		
		int c;
		while ((c = g.getopt()) != -1) {

			char cc = (char) c;
            if (0 == c) {
                cc = (char)long_opts[g.getLongind()].getVal();
            }
		    switch (cc) {
		    case 'h': {
                System.out.println("Usage: java -jar " + script + " [options]");
                System.out.println("-h, --help              help");
                System.out.println("-t, --output-type       output type(bin)");
                System.out.println("-p, --proto             protocol(protobuf)");
                System.out.println("-f, --proto-file        protocol description file");
                System.out.println("-o, --output-dir        output directory");
                System.out.println("-d, --data-src-dir      data source directory");
                System.out.println("-s, --src-file          data source file");
                System.out.println("-m, --src-meta          data description meta");
                System.exit(0);
                break;
            }
            case 't': {
                String val = g.getOptarg();
                if (val.equalsIgnoreCase("bin")) {
                    outType = FileType.BIN;
                } else if (val.equalsIgnoreCase("lua")){
                    outType = FileType.LUA;
                //} else if (val.equalsIgnoreCase("json")){
                //} else if (val.equalsIgnoreCase("xml")){
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
                String[] suffixs = dataSourceFile.split(".");

                String name_suffix = suffixs.length > 0? suffixs[suffixs.length - 1]: null;
                if (null != name_suffix && (
                        name_suffix.equalsIgnoreCase("xls") ||
                        name_suffix.equalsIgnoreCase("xlsx") ||
                        name_suffix.equalsIgnoreCase("cvs") ||
                        name_suffix.equalsIgnoreCase("xlsm") ||
                        name_suffix.equalsIgnoreCase("ods")
                )) {
                    dataSourceType = FileType.EXCEL;

//                } else if (null != name_suffix && (
//                        name_suffix.equalsIgnoreCase("ini") ||
//                        name_suffix.equalsIgnoreCase("cfg") ||
//                        name_suffix.equalsIgnoreCase("conf")
//                )) {
//                    dataSourceType = FileType.INI;
//                } else if (null != name_suffix && name_suffix.equalsIgnoreCase("json")) {
//                    dataSourceType = FileType.JSON;
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

		    default:
		    	System.out.println("[WARN] Unknown option " + g.getOptarg());
                break;
		    }
		}
		
		return 0;
	}

    public String getVersion() {
        return "1.0.0.0";
    }
}
