package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;
import org.json.*;
import org.json.simple.parser.JSONParser;
import org.msgpack.util.json.JSON;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.nio.charset.Charset;

/**
 * Created by owentou on 2015/04/29.
 */
public final class SchemeDataSourceJson extends SchemeDataSourceBase {

    private Object current_object = null;

    public int load() {

        String file_path = ProgramOptions.getInstance().dataSourceFile;
        try {
            JSONParser current_file = new JSONParser();
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

            current_object = current_file.parse(new String(data, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(String.format("[ERROR] open file %s failed", file_path));
            return -21;
        }

        return 0;
    }

    public boolean load_scheme(String section_name) {
        if (null == current_object || false == (current_object instanceof Map)) {
            System.out.println(String.format("[WARN] scheme file error"));
            return false;
        }

        Map cmap = (Map)current_object;
        if (false == cmap.containsKey(section_name)) {
            System.out.println(String.format("[WARN] scheme section %s not found", section_name));
            return false;
        }

        Object scheme_obj = cmap.get(section_name);
        if (false == (scheme_obj instanceof Map)) {
            System.out.println(String.format("[WARN] scheme section %s data invalid", section_name));
            return false;
        }

        Map<Object, Object> scheme_map = (Map<Object, Object>)scheme_obj;
        for(Map.Entry item : scheme_map.entrySet()) {
            ArrayList<String> datas = new ArrayList<String>();

            if (item.getValue() instanceof List) {
                int index = 0;

                for(Object obj: (List)item.getValue()) {
                    datas.add(obj.toString());
                    ++ index;
                }

                for(; index < 3; ++ index) {
                    datas.add("");
                }
            } else if (null != item.getValue()) {
                datas.add(item.getValue().toString());
                datas.add("");
                datas.add("");
            }

            if (false == datas.isEmpty()) {
                set_scheme(item.getKey().toString(), datas);
            }
        }

        return true;
    }
}
