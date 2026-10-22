package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xresloader.core.data.err.InitializeException;

/**
 * Created by owentou on 2015/04/29.
 */
public final class SchemeDataSourceJson extends SchemeDataSourceBase {

    private JSONObject currentObject = null;

    public int load() throws InitializeException {

        String filePath = ProgramOptions.getInstance().dataSourceFile;
        try {
            byte[] data = Files.readAllBytes(Path.of(filePath));
            int offset = data.length >= 3 && data[0] == (byte) 0xef && data[1] == (byte) 0xbb
                    && data[2] == (byte) 0xbf ? 3 : 0;
            currentObject = new JSONObject(new String(data, offset, data.length - offset, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            throw new InitializeException(String.format("open file %s failed", filePath));
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public boolean load_scheme(String sectionName) throws InitializeException {
        if (null == currentObject || false == (currentObject instanceof Map)) {
            ProgramOptions.getLogger().warn("Scheme file error");
            return false;
        }

        if (false == currentObject.has(sectionName)) {
            ProgramOptions.getLogger().warn("Scheme section %s not found", sectionName);
            return false;
        }

        Object schemeObj = currentObject.get(sectionName);
        if (false == (schemeObj instanceof JSONObject)) {
            ProgramOptions.getLogger().warn("Scheme section %s data invalid", sectionName);
            return false;
        }

        Map<Object, Object> schemeMap = (Map<Object, Object>) schemeObj;
        for (Map.Entry<Object, Object> item : schemeMap.entrySet()) {
            loadSegment(item.getKey().toString(), item.getValue());
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadSegment(String key, Object val) {
        ArrayList<String> datas = new ArrayList<String>();

        if (val instanceof List) {
            int index = 0;

            for (Object obj : (List<Object>) val) {
                if (obj instanceof List) {
                    loadSegment(key, obj);
                } else {
                    datas.add(obj.toString());
                    ++index;
                }
            }

            if (0 != index) {
                for (; index < 3; ++index) {
                    datas.add("");
                }
                setScheme(key, datas);
            }
        } else if (null != val) {
            datas.add(val.toString());
            datas.add("");
            datas.add("");
            setScheme(key, datas);
        }
    }
}
