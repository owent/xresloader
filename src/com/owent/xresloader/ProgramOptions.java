package com.owent.xresloader;

import java.util.LinkedList;
import java.util.List;

import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;

import gnu.getopt.*;

public class ProgramOptions {

	public enum OutputType { BIN, LUA, JSON, XML};
	public enum Protocol { PROTOBUF, CAPNPROTO, FLATBUFFER};
	
	public OutputType outType;
	public Protocol protocol;
	public String protocolFile = ""; 
	public String outputDirectory = "";
	public String dataSourceDirectory = "";
	public String dataSourceFile = "";
	public List<String> dataSourceMetas = null;
	
	/**
	 * 单例
	 */
	private static ProgramOptions instance = null;
	private ProgramOptions() {
		dataSourceMetas = new LinkedList<String>();
		outType = OutputType.BIN;
		protocol = Protocol.PROTOBUF;
		
		outputDirectory = System.getProperty("user.dir");
		dataSourceDirectory = outputDirectory;
	}

	public static ProgramOptions getInstance() {
		if (instance == null) {
			instance = new ProgramOptions();
		}
		return instance;
	}

	
	public int init(String[] args) {
		String[] argv = new String[args.length - 1];
		for (int i = 1 ; i < args.length - 1; ++ i)
			argv[i - 1] = args[i];
		
		StringBuffer sb =  new StringBuffer();
		
		Getopt g = new Getopt("", argv, "ht:p:f:o:d:s:m", //
			new LongOpt[]{
				new LongOpt("help", 		LongOpt.NO_ARGUMENT, null, 'h'),
				new LongOpt("output-type", 	LongOpt.REQUIRED_ARGUMENT, sb, 't'),
				new LongOpt("proto", 		LongOpt.REQUIRED_ARGUMENT, sb, 'p'),
				new LongOpt("proto-file", 	LongOpt.REQUIRED_ARGUMENT, sb, 'f'),
				new LongOpt("output-dir", 	LongOpt.REQUIRED_ARGUMENT, sb, 'o'),
				new LongOpt("data-src-dir", LongOpt.REQUIRED_ARGUMENT, sb, 'd'),
				new LongOpt("src-file", 	LongOpt.REQUIRED_ARGUMENT, sb, 's'),
				new LongOpt("src-meta", 	LongOpt.REQUIRED_ARGUMENT, sb, 'm')
			}
		);
		g.setOpterr(false);
		
		int c;
		while ((c = g.getopt()) != -1) {
			char cc = (char) c;
		    switch (c) {
		    case 'h':
		    case 1:
		    	System.out.println("Usage: java -jar " + args[0] + " [options]");
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
		    	
		    default:
		    	System.out.println("[WARN] Unknown option " + g.toString());
		    }
		}
		
		return c;
	}
}
