package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;
import org.ini4j.ConfigParser;

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
            ProgramOptions.getLoger().error("open file %s failed", file_path);
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String section_name) {
        Map<String, HashMap<String, ArrayList<String>>> all_conf = new HashMap<String, HashMap<String, ArrayList<String>>>();
        List<Map.Entry<String,String>> datas = null;
        try {
            datas = current_file.items(section_name);
        } catch (ConfigParser.NoSectionException e) {
            ProgramOptions.getLoger().warn("scheme section %s not found", section_name);
        } catch (ConfigParser.InterpolationMissingOptionException e) {
            ProgramOptions.getLoger().warn("read scheme error,%s", e.getMessage());
            e.printStackTrace();
        }

        if (null != datas) {
            for (Map.Entry<String, String> data : datas) {
                String[] keys = data.getKey().split("\\.");
                for(int i = 0; i < keys.length; ++i) {
                    keys[i] = keys[i].trim();
                }
                dump_scheme(all_conf, keys, data.getValue());
            }
        }

        // 数据项必须在这之后
        for(Map.Entry<String, HashMap<String, ArrayList<String>>> element: all_conf.entrySet()) {
            for (Map.Entry<String, ArrayList<String>> sub_datas: element.getValue().entrySet()) {
                set_scheme(element.getKey(), sub_datas.getValue());
            }
        }

        return true;
    }

    private ArrayList<String> mutable_configure(Map<String, HashMap<String, ArrayList<String>>> out, String[] keys) {
        ArrayList<String> ret = null;
        if (0 == keys.length) {
            return ret;
        }

        HashMap<String, ArrayList<String>> first_layer = out.getOrDefault(keys[0], null);
        if(first_layer == null) {
            first_layer = new HashMap<String, ArrayList<String>>();
            out.put(keys[0], first_layer);
        }

        String second_key = "";
        if (keys.length > 2) {
            second_key = keys[1];
            for (int i = 2; i < keys.length - 1; ++ i) {
                second_key += "." + keys[i];
            }
        }

        ret = first_layer.getOrDefault(second_key, null);
        if (null == ret) {
            ret = new ArrayList<String>();
            for (int i = 0; i < 3; ++ i) {
                ret.add("");
            }
            first_layer.put(second_key, ret);
        }

        return ret;
    }

    private void dump_scheme(Map<String, HashMap<String, ArrayList<String>>> out, String[] keys, String val) {
        if (keys.length <= 0) {
            return;
        }

        ArrayList<String> opr = mutable_configure(out, keys);
        if (null == opr) {
            return;
        }

        if (keys.length > 1) {
            int index = Integer.parseInt(keys[keys.length - 1]);
            if (index < opr.size() && index >= 0) {
                opr.set(index, val);
            }
        } else {
            opr.set(0, val);
        }
    }
}
