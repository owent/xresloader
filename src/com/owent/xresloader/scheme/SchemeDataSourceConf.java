package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;
import org.ini4j.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by owentou on 2015/04/29.
 */
public final class SchemeDataSourceConf extends SchemeDataSourceBase {

    private ConfigParser current_file = new ConfigParser();

    public int load() {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        try {
            FileInputStream fsi = new FileInputStream(file_path);
            byte[] utf8_bom = new byte[3];
            fsi.read(utf8_bom, 0, 3);
            if(utf8_bom[0] != (byte)0xef ||
                utf8_bom[1] != (byte)0xbb ||
                utf8_bom[2] != (byte)0xbf) {
                fsi.close();
                fsi = new FileInputStream(file_path);
            }

            current_file.read(fsi);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(String.format("[ERROR] open file %s failed", file_path));
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String section_name) {
        Map<String, ArrayList<String> > all_conf = new HashMap<String, ArrayList<String>>();
        List<Map.Entry<String,String>> datas = null;
        try {
            datas = current_file.items(section_name);
        } catch (ConfigParser.NoSectionException e) {
            System.out.println(String.format("[WARN] scheme section %s not found", section_name));
        } catch (ConfigParser.InterpolationMissingOptionException e) {
            System.out.println(String.format("[WARN] read scheme error,%s", e.getMessage()));
            e.printStackTrace();
        }

        if (null != datas) {
            for (Map.Entry<String, String> data : datas) {
                String key = "";
                Integer index = 0;
                if(data.getKey().endsWith(".0") || data.getKey().endsWith(".1") || data.getKey().endsWith(".2")) {
                    key = data.getKey().substring(0, data.getKey().length() - 2);
                    index = Integer.valueOf(data.getKey().substring(data.getKey().length() - 1));
                } else {
                    key = data.getKey();
                }

                ArrayList<String> data_value;
                if (!all_conf.containsKey(key)) {
                    data_value = new ArrayList<String>();
                    data_value.add("");
                    data_value.add("");
                    data_value.add("");
                    all_conf.put(key, data_value);
                } else {
                    data_value = all_conf.get(key);
                }

                data_value.set(index, data.getValue());
            }
        }

        // 数据项必须在这之后
        set_scheme(all_conf);

        return true;
    }
}
