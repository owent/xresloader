package org.xresloader.core.data.dst;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2019/04/08.
 */
public class DataDstUEJson extends DataDstUEBase {
    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "ue json";
    }

    private class UEBuildObject {
        JSONArray ja = null;
        ArrayList<Object> header = null;
    }

    @Override
    protected boolean isRecursiveEnabled() {
        return true;
    }

    @Override
    protected Object buildForUEOnInit() throws IOException {
        UEBuildObject ret = new UEBuildObject();
        ret.ja = new JSONArray();

        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected byte[] buildForUEOnFinal(Object buildObj) {
        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return ((UEBuildObject) buildObj).ja.toString(4).getBytes();

        return ((UEBuildObject) buildObj).ja.toString(4).getBytes(Charset.forName(encoding));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildForUEOnPrintHeader(Object buildObj, ArrayList<Object> rowData, UEDataRowRule rule) throws IOException {
        ((UEBuildObject) buildObj).header = rowData;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildForUEOnPrintRecord(Object buildObj, ArrayList<Object> rowData, UEDataRowRule rule) throws IOException {
        JSONObject jobj = new JSONObject();
        UEBuildObject bobj = ((UEBuildObject) buildObj);
        for (int i = 0; i < bobj.header.size() && i < rowData.size(); ++i) {
            jobj.put(bobj.header.get(i).toString(), rowData.get(i));
        }

        bobj.ja.put(jobj);
    }

    @Override
    public final DataDstWriterNode compile() {
        ProgramOptions.getLoger().error("UE-json can not be protocol description.");
        return null;
    }

    @SuppressWarnings("unchecked")
    private void writeConstData(JSONArray jo, Object data, String prefix, String valSeg) throws IOException {
        // null
        if (null == data) {
            JSONObject obj = new JSONObject();
            obj.put("Name", prefix);
            obj.put(valSeg, 0);
            jo.put(obj);
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Number) {
            JSONObject obj = new JSONObject();
            obj.put("Name", prefix);
            obj.put(valSeg, data);
            jo.put(obj);
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            JSONObject obj = new JSONObject();
            obj.put("Name", prefix);
            obj.put(valSeg, data);
            jo.put(obj);
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            JSONObject obj = new JSONObject();
            obj.put("Name", prefix);
            obj.put(valSeg, data);
            jo.put(obj);
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>) data;
            for (int i = 0; i < ls.size(); ++i) {
                if (prefix.isEmpty()) {
                    writeConstData(jo, ls.get(i), String.format("%d", i), valSeg);
                } else {
                    writeConstData(jo, ls.get(i), String.format("%s.%d", prefix, i), valSeg);
                }
            }
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>) data;
            for (Map.Entry<String, Object> item : mp.entrySet()) {
                if (prefix.isEmpty()) {
                    writeConstData(jo, item.getValue(), String.format("%s", item.getKey()), valSeg);
                } else {
                    writeConstData(jo, item.getValue(), String.format("%s.%s", prefix, item.getKey()), valSeg);
                }
            }
            return;
        }

        ProgramOptions.getLoger().error("rewrite %s as nil, should not called here.", data.toString());
    }

    /**
     * 和输出格式无关的常量转储功能
     * 
     * @param data 常量数据集
     * @return 常量代码
     */
    @Override
    public String dumpConstForUE(HashMap<String, Object> data, UEDataRowRule rule) throws IOException {
        JSONArray jo = new JSONArray();

        writeConstData(jo, data, "", getIdentName("Value"));

        return jo.toString(4);
    }
}
