/**
 * 
 */
package com.owent.xresloader;

import com.owent.xresloader.data.src.DataSrcExcel;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;

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

        // 读入数据表 & 协议编译
        for(String sn : ProgramOptions.getInstance().dataSourceMetas) {
            // 1. 描述信息
            if (false == SchemeConf.getInstance().getScheme().load_scheme(sn)) {
                System.err.println("[ERROR] convert scheme \"" + sn + "\" failed");
                continue;
            } else {
                System.out.println("[INFO] convert scheme \"" + sn + "\" success");
            }

            // 2. 数据工作簿
            Class ds_clazz = DataSrcExcel.class;
            DataSrcImpl ds = DataSrcImpl.create(ds_clazz);
            if (null == ds) {
                System.err.println("[ERROR] create data source class \"" + ds_clazz.getName() + "\" failed");
                continue;
            }
            ret = ds.init();
            if (ret < 0) {
                System.err.println("[ERROR] init data source class \"" + ds_clazz.getName() + "\" failed");
                continue;
            }

            // 3. 协议描述文件
        }
	}

}
