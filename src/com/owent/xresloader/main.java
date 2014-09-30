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
		int ret = ProgramOptions.getInstance().init(args);
		if (ret < 0)
			System.exit(ret);

        ret = SchemeConf.getInstance().initScheme();
        if (ret < 0)
            System.exit(ret);
	}

}
