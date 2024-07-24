package org.xresloader.core.data.dst;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.scheme.SchemeConf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

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
    }

    @Override
    protected Object buildForUEOnInit() throws IOException {
        UEBuildObject ret = new UEBuildObject();
        ret.ja = new JSONArray();

        return ret;
    }

    @Override
    protected byte[] buildForUEOnFinal(Object buildObj) throws ConvException {
        try {
            // 带编码的输出
            String encoding = SchemeConf.getInstance().getKey().getEncoding();
            if (null == encoding || encoding.isEmpty())
                return DataDstJson.stringify(((UEBuildObject) buildObj).ja, 4).toString().getBytes();
            return DataDstJson.stringify(((UEBuildObject) buildObj).ja, 4).toString()
                    .getBytes(Charset.forName(encoding));
        } catch (JSONException e) {
            setLastErrorMessage(
                    "stringify JSON object failed, %s", e.getMessage());
            throw new ConvException(getLastErrorMessage());
        }
    }

    @Override
    protected void buildForUEOnPrintHeader(Object buildObj, ArrayList<DataDstWriterNodeWrapper> rowData,
            UEDataRowRule rule,
            UECodeInfo codeInfo) throws IOException {
    }

    @Override
    protected void buildForUEOnPrintRecord(Object buildObj, HashMap<String, Object> rowData, UEDataRowRule rule,
            UECodeInfo codeInfo) throws IOException {
        JSONObject jobj = new JSONObject();
        HashSet<String> dumpedFields = new HashSet<String>();

        UEBuildObject bobj = ((UEBuildObject) buildObj);
        // 顺序约定:
        // 1. key
        // 2. value(包含oneof)
        // 3. 补全空字段(提取自
        // codeInfo.writerNodeWrapper.getTypeDescriptor().getSortedFields())

        // The key field of 0 is FName Name
        for (int i = 0; i < rule.keyFields.size(); ++i) {
            dumpField(jobj, dumpedFields, rule.keyFields.get(i), rowData);
        }

        for (int i = 0; i < rule.valueFields.size(); ++i) {
            dumpField(jobj, dumpedFields, rule.valueFields.get(i), rowData);
        }

        // 需要补全空字段, oneof
        if (null != codeInfo.writerNodeWrapper.getTypeDescriptor()) {
            for (DataDstFieldDescriptor field : codeInfo.writerNodeWrapper.getTypeDescriptor().getSortedFields()) {
                String varName = getIdentName(field.getName());

                if (dumpedFields.contains(varName)) {
                    continue;
                }
                dumpedFields.add(varName);
                dumpDefault(jobj, field);
            }
        }

        if (!jobj.isEmpty()) {
            bobj.ja.put(jobj);
        }
    }

    private JSONObject pickMessage(DataDstWriterNodeWrapper descWraper,
            HashMap<?, ?> dataSet) {

        // 仅需要数据结构，提取一个节点就行了，剩下的走dataSet
        ArrayList<DataDstWriterNodeWrapper> children = new ArrayList<>();
        children.ensureCapacity(descWraper.getChildren().size());
        descWraper.getChildren().values().forEach((ea) -> {
            if (!ea.isEmpty()) {
                children.add(ea.get(0));
            }
        });

        children.sort((l, r) -> {
            return l.compareTo(r);
        });

        JSONObject ret = new JSONObject();
        HashSet<String> dumpedFields = new HashSet<>();
        for (DataDstWriterNodeWrapper child : children) {
            dumpField(ret, dumpedFields, child, dataSet);
        }

        // 补全缺失字段
        for (DataDstWriterNodeWrapper child : children) {
            if (!dumpedFields.contains(child.getVarName())) {
                if (child.getReferOneof() != null) {
                    dumpDefault(ret, child.getReferOneof());
                } else {
                    dumpDefault(ret, child.getReferField());
                }
            }
        }

        return ret;
    }

    private Object pickField(DataDstWriterNodeWrapper descWraper, Object data) {
        if (data instanceof List<?>) {
            JSONArray ret = new JSONArray();
            for (Object val : (List<?>) data) {
                Object element = pickField(descWraper, val);
                if (element != null) {
                    ret.put(element);
                }
            }
            if (ret.isEmpty()) {
                return null;
            }

            return ret;
        } else if (data instanceof SpecialInnerHashMap<?, ?>) {
            JSONObject ret = new JSONObject();
            ArrayList<DataDstWriterNodeWrapper> valueDesc = descWraper.getMapValueField();
            if (valueDesc.isEmpty()) {
                return null;
            }

            for (Map.Entry<?, ?> subval : ((SpecialInnerHashMap<?, ?>) data).entrySet()) {
                Object element = pickField(valueDesc.get(0), subval.getValue());
                if (element != null) {
                    ret.put(subval.getKey().toString(), element);
                }
            }

            if (ret.isEmpty()) {
                return null;
            }

            return ret;
        } else if (data instanceof HashMap<?, ?>) {
            return pickMessage(descWraper, (HashMap<?, ?>) data);
        } else {
            return data;
        }
    }

    private void dumpField(JSONObject obj, HashSet<String> dumpedFields, DataDstWriterNodeWrapper descWraper,
            HashMap<?, ?> dataSet) {
        if (dumpedFields.contains(descWraper.getVarName())) {
            return;
        }

        Object val = pickJavaFieldValue(dataSet, descWraper);
        if (val == null) {
            return;
        }

        Object fieldData = pickField(descWraper, val);
        if (fieldData != null) {
            dumpedFields.add(descWraper.getVarName());
            obj.put(descWraper.getVarName(), fieldData);
        }
    }

    @Override
    public final DataDstWriterNode compile() {
        this.logErrorMessage("UE-json can not be protocol description.");
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
            ArrayList<Map.Entry<String, Object>> sorted_array = new ArrayList<Map.Entry<String, Object>>();
            sorted_array.ensureCapacity(mp.size());
            sorted_array.addAll(mp.entrySet());
            sorted_array.sort((l, r) -> {
                return l.getKey().compareTo(r.getKey());
            });

            for (Map.Entry<String, Object> item : sorted_array) {
                if (prefix.isEmpty()) {
                    writeConstData(jo, item.getValue(), String.format("%s", item.getKey()), valSeg);
                } else {
                    writeConstData(jo, item.getValue(), String.format("%s.%s", prefix, item.getKey()), valSeg);
                }
            }
            return;
        }

        JSONObject obj = new JSONObject();
        obj.put("Name", prefix);
        obj.put(valSeg, data.toString());
        jo.put(obj);
    }

    /**
     * 和输出格式无关的常量转储功能
     * 
     * @param data 常量数据集
     * @return 常量代码
     */
    @Override
    public String dumpConstForUE(HashMap<String, Object> data, UEDataRowRule rule) throws IOException, ConvException {
        JSONArray jo = new JSONArray();

        writeConstData(jo, data, "", getIdentName("Value"));

        try {
            return DataDstJson.stringify(jo, 4).toString();
        } catch (JSONException e) {
            setLastErrorMessage(
                    "stringify JSON object failed, %s", e.getMessage());
            throw new ConvException(getLastErrorMessage());
        }
    }

    private Object getDefault(DataDstFieldDescriptor fd) {
        switch (fd.getType()) {
            case INT: {
                return Integer.valueOf(0);
            }
            case LONG: {
                return Long.valueOf(0);
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
                HashSet<String> dumpedOneof = new HashSet<String>();
                JSONObject ret = new JSONObject();
                for (DataDstFieldDescriptor subField : fd.getTypeDescriptor().getSortedFields()) {
                    String varName = getIdentName(subField.getName());
                    // 需要dump一次oneof字段
                    if (null != subField.getReferOneof()) {
                        String oneofVarName = getIdentName(subField.getReferOneof().getName());
                        if (!dumpedOneof.contains(oneofVarName)) {
                            dumpedOneof.add(oneofVarName);
                            ret.put(oneofVarName, Integer.valueOf(0));
                        }
                    }

                    if (subField.isMap()) {
                        ret.put(varName, new JSONObject());
                    } else if (subField.isList()) {
                        ret.put(varName, new JSONArray());
                    } else {
                        dumpDefault(ret, subField);
                    }
                }

                return ret;
            }
            default:
                return null;
        }
    }

    protected Object dumpDefault(JSONObject builder, DataDstWriterNode.DataDstOneofDescriptor oneof) {
        String varName = getIdentName(oneof.getName());
        String val = "";
        builder.put(varName, val);
        return val;
    }

    /**
     * Dump default element and return the whole field
     * 
     * @param builder container
     * @param fd      element descriptor
     * @return All field data, JSONObect for map and JSONArray for list
     */
    protected Object dumpDefault(JSONObject builder, DataDstFieldDescriptor fd) {
        String varName = getIdentName(fd.getName());
        if (fd.isMap()) {
            Object val = builder.opt(varName);
            if (null == val) {
                val = new JSONObject();
                builder.put(varName, val);
            }
            return val;
        } else if (fd.isList()) {
            JSONArray old = (JSONArray) builder.opt(varName);
            if (null == old) {
                old = new JSONArray();
                builder.put(varName, old);
            }
            if (fd.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                Object val = getDefault(fd);
                if (val == null) {
                    return old;
                }
                old.put(val);
            }

            return old;
        } else {
            Object val = getDefault(fd);
            if (val == null) {
                return val;
            }
            builder.put(varName, val);
            return val;
        }
    }
}
