package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2015/04/29.
 */
public final class SchemeDataSourceJson extends SchemeDataSourceBase {

    private JSONObject current_object = null;

    public int load() {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        try {
            FileInputStream fis = new FileInputStream(file_path);
            File fd = new File(file_path);

            byte[] utf8_bom = new byte[3];
            int file_length = (int)fd.length();

            fis.read(utf8_bom);
            if(utf8_bom[0] != (byte)0xef ||
                utf8_bom[1] != (byte)0xbb ||
                utf8_bom[2] != (byte)0xbf) {
                fis.close();

                fis = new FileInputStream(file_path);
            } else {
                file_length -= 3;
            }

            byte[] data = new byte[file_length];
            fis.read(data);

            current_object = new JSONObject(new String(data, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            ProgramOptions.getLoger().error("open file %s failed", file_path);
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String section_name) {
        if (null == current_object || false == (current_object instanceof Map)) {
            ProgramOptions.getLoger().warn("scheme file error");
            return false;
        }

        if (false == current_object.has(section_name)) {
            ProgramOptions.getLoger().warn("scheme section %s not found", section_name);
            return false;
        }

        Object scheme_obj = current_object.get(section_name);
        if (false == (scheme_obj instanceof JSONObject)) {
            ProgramOptions.getLoger().warn("scheme section %s data invalid", section_name);
            return false;
        }

        Map<Object, Object> scheme_map = (Map<Object, Object>)scheme_obj;
        for(Map.Entry item : scheme_map.entrySet()) {
            load_segment(item.getKey().toString(), item.getValue());
        }

        return true;
    }

    private void load_segment(String key, Object val) {
        ArrayList<String> datas = new ArrayList<String>();

        if (val instanceof List) {
            int index = 0;

            for(Object obj: (List)val) {
                if (obj instanceof List) {
                    load_segment(key, obj);
                } else {
                    datas.add(obj.toString());
                    ++index;
                }
            }

            if (0 != index) {
                for (; index < 3; ++index) {
                    datas.add("");
                }
                set_scheme(key, datas);
            }
        } else if (null != val) {
            datas.add(val.toString());
            datas.add("");
            datas.add("");
            set_scheme(key, datas);
        }
    }
}
