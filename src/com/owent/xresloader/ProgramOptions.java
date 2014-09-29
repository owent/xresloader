package com.owent.xresloader;

import java.util.LinkedList;
import java.util.List;


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

}
