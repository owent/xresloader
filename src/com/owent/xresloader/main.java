/**
 * 
 */
package com.owent.xresloader;

/**
 * @author owentou
 *
 */
public class main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 
		System.out.println("Hello world!");

		int ret = ProgramOptions.getInstance().init(args);
		if (ret < 0)
			System.exit(ret);
		
	}

}
