package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;
import org.ini4j.ConfigParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xresloader.core.data.err.InitializeException;

/**
 * Created by owentou on 2015/04/29.
 */
public final class SchemeDataSourceConf extends SchemeDataSourceBase {

    private ConfigParser currentFile = new ConfigParser();

    public int load() throws InitializeException {

        String filePath = ProgramOptions.getInstance().dataSourceFile;
        String errorMessage = null;
        try (FileInputStream input = new FileInputStream(filePath)) {
            byte[] utf8Bom = input.readNBytes(3);
            if (utf8Bom.length != 3 || utf8Bom[0] != (byte) 0xef || utf8Bom[1] != (byte) 0xbb
                    || utf8Bom[2] != (byte) 0xbf) {
                input.getChannel().position(0);
            }

            currentFile.read(input);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = String.format("open file %s failed", filePath);
        }

        if (errorMessage != null) {
            throw new InitializeException(errorMessage);
        }

        return 0;
    }

    @Deprecated
    public boolean load_scheme(String sectionName) throws InitializeException {
        Map<String, HashMap<String, ArrayList<String>>> allConf = new HashMap<String, HashMap<String, ArrayList<String>>>();
        List<Map.Entry<String, String>> datas = null;
        try {
            datas = currentFile.items(sectionName);
        } catch (ConfigParser.NoSectionException e) {
            ProgramOptions.getLogger().warn("Scheme section %s not found", sectionName);
        } catch (ConfigParser.InterpolationMissingOptionException e) {
            ProgramOptions.getLogger().warn("Read scheme error,%s", e.getMessage());
            e.printStackTrace();
        }

        if (null != datas) {
            for (Map.Entry<String, String> data : datas) {
                String[] keys = data.getKey().split("\\.");
                for (int i = 0; i < keys.length; ++i) {
                    keys[i] = keys[i].trim();
                }
                dumpScheme(allConf, keys, data.getValue());
            }
        }

        // 数据项必须在这之后
        for (Map.Entry<String, HashMap<String, ArrayList<String>>> element : allConf.entrySet()) {
            for (Map.Entry<String, ArrayList<String>> subDatas : element.getValue().entrySet()) {
                setScheme(element.getKey(), subDatas.getValue());
            }
        }

        return true;
    }

    private ArrayList<String> mutableConfigure(Map<String, HashMap<String, ArrayList<String>>> out, String[] keys) {
        ArrayList<String> ret = null;
        if (0 == keys.length) {
            return ret;
        }

        HashMap<String, ArrayList<String>> firstLayer = out.getOrDefault(keys[0], null);
        if (firstLayer == null) {
            firstLayer = new HashMap<String, ArrayList<String>>();
            out.put(keys[0], firstLayer);
        }

        String secondKey = "";
        if (keys.length > 2) {
            secondKey = keys[1];
            for (int i = 2; i < keys.length - 1; ++i) {
                secondKey += "." + keys[i];
            }
        }

        ret = firstLayer.getOrDefault(secondKey, null);
        if (null == ret) {
            ret = new ArrayList<String>();
            for (int i = 0; i < 3; ++i) {
                ret.add("");
            }
            firstLayer.put(secondKey, ret);
        }

        return ret;
    }

    private void dumpScheme(Map<String, HashMap<String, ArrayList<String>>> out, String[] keys, String val) {
        if (keys.length <= 0) {
            return;
        }

        ArrayList<String> opr = mutableConfigure(out, keys);
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
