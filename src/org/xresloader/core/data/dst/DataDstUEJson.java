package org.xresloader.core.data.dst;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstChildrenNode;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.JAVA_TYPE;
import org.xresloader.core.data.err.ConvException;
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
    protected void buildForUEOnPrintHeader(Object buildObj, ArrayList<Object> rowData, UEDataRowRule rule,
            UECodeInfo codeInfo) throws IOException {
        ((UEBuildObject) buildObj).header = rowData;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildForUEOnPrintRecord(Object buildObj, ArrayList<Object> rowData, UEDataRowRule rule,
            UECodeInfo codeInfo) throws IOException {
        JSONObject jobj = new JSONObject();
        HashSet<String> dumpedFields = null;
        if (isRecursiveEnabled()) {
            dumpedFields = new HashSet<String>();
        }

        UEBuildObject bobj = ((UEBuildObject) buildObj);
        for (int i = 0; i < bobj.header.size() && i < rowData.size(); ++i) {
            jobj.put(bobj.header.get(i).toString(), rowData.get(i));
            if (null != dumpedFields) {
                dumpedFields.add(bobj.header.get(i).toString());
            }
        }

        // 需要补全空字段
        if (null != dumpedFields && null != codeInfo.desc) {
            for (HashMap.Entry<String, DataDstFieldDescriptor> varPair : codeInfo.desc.getTypeDescriptor().fields
                    .entrySet()) {
                String varName = getIdentName(varPair.getKey());
                if (dumpedFields.contains(varName)) {
                    continue;
                }

                jobj.put(varName, pickValueFieldJsonDefaultImpl(varPair.getValue()));
            }
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

    @Override
    final protected Object pickValueField(DataDstWriterNodeWrapper desc) throws ConvException {
        if (!isRecursiveEnabled()) {
            return pickValueFieldBaseImpl(desc, 0);
        }

        return pickValueFieldJsonImpl(desc);
    }

    protected Object pickValueFieldJsonImpl(DataDstWriterNodeWrapper descWrapper) throws ConvException {
        if (null == descWrapper || null == descWrapper.descs || descWrapper.descs.isEmpty()) {
            return null;
        }

        DataDstWriterNode desc = descWrapper.GetWriterNode(0);
        if (desc == null) {
            return null;
        }

        if (descWrapper.isList) {
            JSONArray ret = new JSONArray();
            for (DataDstWriterNode child : descWrapper.descs) {
                Object val = pickValueFieldJsonImpl(child);
                if (val != null) {
                    ret.put(val);
                }
            }

            return ret;
        } else {
            return pickValueFieldJsonImpl(desc);
        }
    }

    protected Object pickValueFieldJsonImpl(DataDstWriterNode desc) throws ConvException {
        if (desc == null) {
            return null;
        }

        if (desc.getType() == JAVA_TYPE.MESSAGE) {
            HashSet<String> dumpedFields = null;
            if (isRecursiveEnabled()) {
                dumpedFields = new HashSet<String>();
            }

            JSONObject ret = new JSONObject();
            for (Entry<String, DataDstChildrenNode> child : desc.getChildren().entrySet()) {
                Object val = null;
                if (child.getValue().innerDesc.isList()) {
                    JSONArray res = new JSONArray();

                    for (DataDstWriterNode subNode : child.getValue().nodes) {
                        Object v = pickValueFieldJsonImpl(subNode);
                        if (v != null) {
                            res.put(v);
                        }
                    }

                    val = res;
                } else if (!child.getValue().nodes.isEmpty()) {
                    val = pickValueFieldJsonImpl(child.getValue().nodes.get(0));
                }

                if (val != null) {
                    String varName = getIdentName(child.getKey());
                    ret.put(varName, val);

                    if (null != dumpedFields) {
                        dumpedFields.add(varName);
                    }
                }
            }

            // 需要补全空字段
            if (null != dumpedFields) {
                for (HashMap.Entry<String, DataDstFieldDescriptor> varPair : desc.getTypeDescriptor().fields
                        .entrySet()) {
                    String varName = getIdentName(varPair.getKey());
                    if (dumpedFields.contains(varName)) {
                        continue;
                    }

                    ret.put(varName, pickValueFieldJsonDefaultImpl(varPair.getValue()));
                }
            }

            return ret;
        }

        return pickValueFieldBaseImpl(desc);
    }

    protected Object pickValueFieldJsonDefaultImpl(DataDstFieldDescriptor fd) {
        if (fd.isList()) {
            return new JSONArray();
        }

        switch (fd.getType()) {
        case INT:
        case LONG: {
            return 0;
        }
        case BOOLEAN: {
            return false;
        }
        case STRING:
        case BYTES: {
            return "";
        }
        case FLOAT:
        case DOUBLE: {
            return 0.0f;
        }
        case MESSAGE: {
            JSONObject ret = new JSONObject();
            for (HashMap.Entry<String, DataDstFieldDescriptor> varPair : fd.getTypeDescriptor().fields.entrySet()) {
                ret.put(getIdentName(varPair.getKey()), pickValueFieldJsonDefaultImpl(varPair.getValue()));
            }

            return ret;
        }
        default:
            return null;
        }
    }
}
