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
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.IdentifyDescriptor;
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
        return SchemeConf.getInstance().getUEOptions().enableRecursiveMode;
    }

    @Override
    protected Object buildForUEOnInit() throws IOException {
        UEBuildObject ret = new UEBuildObject();
        ret.ja = new JSONArray();

        return ret;
    }

    @Override
    protected byte[] buildForUEOnFinal(Object buildObj) {
        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return ((UEBuildObject) buildObj).ja.toString(4).getBytes();

        return ((UEBuildObject) buildObj).ja.toString(4).getBytes(Charset.forName(encoding));
    }

    @Override
    protected void buildForUEOnPrintHeader(Object buildObj, ArrayList<Object> rowData, UEDataRowRule rule,
            UECodeInfo codeInfo) throws IOException {
        ((UEBuildObject) buildObj).header = rowData;
    }

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
        if (null != dumpedFields && null != codeInfo.messageDesc) {
            for (HashMap.Entry<String, DataDstFieldDescriptor> varPair : codeInfo.messageDesc.fields.entrySet()) {
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
                if (l.getValue() instanceof Integer && r.getValue() instanceof Integer) {
                    return ((Integer) l.getValue()).compareTo((Integer) r.getValue());
                }

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
    public String dumpConstForUE(HashMap<String, Object> data, UEDataRowRule rule) throws IOException {
        JSONArray jo = new JSONArray();

        writeConstData(jo, data, "", getIdentName("Value"));

        return jo.toString(4);
    }

    @Override
    final protected Object pickValueField(Object buildObj, DataDstFieldNodeWrapper desc) throws ConvException {
        if (!isRecursiveEnabled()) {
            return pickValueFieldBaseStandardImpl(desc, 0);
        }

        if (null == desc || null == desc.referWriterNodes || desc.referWriterNodes.isEmpty()) {
            return null;
        }

        if (desc.GetWriterNode(0).getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
            return pickValueFieldJsonStandardImpl(desc);
        }

        if (desc.GetWriterNode(0).getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
            return pickValueFieldJsonPlainImpl(desc);
        }

        return null;
    }

    protected Object pickValueFieldJsonStandardImpl(DataDstFieldNodeWrapper descWrapper) throws ConvException {
        if (null == descWrapper || null == descWrapper.referWriterNodes || descWrapper.referWriterNodes.isEmpty()) {
            return null;
        }

        DataDstWriterNode desc = descWrapper.GetWriterNode(0);
        if (desc == null) {
            return null;
        }

        if (descWrapper.isList()) {
            JSONArray ret = new JSONArray();
            for (DataDstWriterNode child : descWrapper.referWriterNodes) {
                Object val = pickValueFieldJsonStandardImpl(child);
                if (val != null) {
                    ret.put(val);
                }
            }

            return ret;
        } else {
            return pickValueFieldJsonStandardImpl(desc);
        }
    }

    protected Object pickValueFieldJsonStandardImpl(DataDstWriterNode desc) throws ConvException {
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
                        Object v = pickValueFieldJsonStandardImpl(subNode);
                        if (v != null) {
                            res.put(v);
                        }
                    }

                    val = res;
                } else if (!child.getValue().nodes.isEmpty()) {
                    val = pickValueFieldJsonStandardImpl(child.getValue().nodes.get(0));
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

        return pickValueFieldBaseStandardImpl(desc);
    }

    protected Object pickValueFieldJsonPlainImpl(DataDstFieldNodeWrapper descWrapper) throws ConvException {
        if (null == descWrapper || null == descWrapper.referWriterNodes || descWrapper.referWriterNodes.isEmpty()) {
            return null;
        }

        DataDstWriterNode desc = descWrapper.GetWriterNode(0);
        if (desc == null) {
            return null;
        }

        Object ret = pickValueFieldJsonPlainField(desc.identify, desc.getFieldDescriptor(), true);
        if (ret == null) {
            ret = pickValueFieldJsonDefaultImpl(desc.getFieldDescriptor());
        }
        return ret;
    }

    private Object pickValueFieldJsonPlainField(IdentifyDescriptor ident, DataDstFieldDescriptor field,
            boolean isTopLevel) throws ConvException {
        if (null == ident) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        return pickValueFieldJsonPlainField(ident, field, isTopLevel, res.value);
    }

    private Object pickValueFieldJsonPlainField(IdentifyDescriptor ident, DataDstFieldDescriptor field,
            boolean isTopLevel, String input) throws ConvException {

        if ((isTopLevel && !field.isList()) && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }

        Object ret = null;
        if (field.isList()) {
            String[] groups = splitPlainGroups(input.trim(), getPlainFieldSeparator(field));
            switch (field.getType()) {
            case INT: {
                Long[] values = parsePlainDataLong(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (Long v : values) {
                    tmp.put(v.intValue());
                }
                ret = tmp;
                break;
            }

            case LONG: {
                Long[] values = parsePlainDataLong(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (Long v : values) {
                    tmp.put(v);
                }
                ret = tmp;
                break;
            }

            case FLOAT: {
                Double[] values = parsePlainDataDouble(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (Double v : values) {
                    tmp.put(v.floatValue());
                }
                ret = tmp;
                break;
            }

            case DOUBLE: {
                Double[] values = parsePlainDataDouble(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (Double v : values) {
                    tmp.put(v);
                }
                ret = tmp;
                break;
            }

            case BOOLEAN: {
                Boolean[] values = parsePlainDataBoolean(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (Boolean v : values) {
                    tmp.put(v);
                }
                ret = tmp;
                break;
            }

            case STRING:
            case BYTES: {
                String[] values = parsePlainDataString(groups, ident, isTopLevel ? null : field);
                JSONArray tmp = new JSONArray();
                for (String v : values) {
                    tmp.put(v);
                }
                ret = tmp;
                break;
            }

            case MESSAGE: {
                JSONArray tmp = new JSONArray();
                for (String v : groups) {
                    String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                    JSONObject msg = pickValueFieldJsonPlainField(subGroups, ident, field);
                    if (msg != null) {
                        tmp.put(msg);
                    }
                }
                ret = tmp;
                break;
            }

            default:
                break;
            }
        } else {
            switch (field.getType()) {
            case INT: {
                ret = parsePlainDataLong(input.trim(), ident, isTopLevel ? null : field).intValue();
                break;
            }

            case LONG: {
                ret = parsePlainDataLong(input.trim(), ident, isTopLevel ? null : field);
                break;
            }

            case FLOAT: {
                ret = parsePlainDataDouble(input.trim(), ident, isTopLevel ? null : field).floatValue();
                break;
            }

            case DOUBLE: {
                ret = parsePlainDataDouble(input.trim(), ident, isTopLevel ? null : field);
                break;
            }

            case BOOLEAN: {
                ret = parsePlainDataBoolean(input.trim(), ident, isTopLevel ? null : field);
                break;
            }

            case STRING:
            case BYTES: {
                ret = parsePlainDataString(input.trim(), ident, isTopLevel ? null : field);
                break;
            }

            case MESSAGE: {
                String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                ret = pickValueFieldJsonPlainField(groups, ident, field);
                if (ret == null) {
                    ret = pickValueFieldJsonDefaultImpl(field);
                }
                break;
            }

            default:
                break;
            }
        }

        return ret;
    }

    private JSONObject pickValueFieldJsonPlainField(String[] inputs, IdentifyDescriptor ident,
            DataDstFieldDescriptor field) throws ConvException {
        if (field.getTypeDescriptor() == null || ident == null || inputs == null || inputs.length == 0) {
            return null;
        }

        ArrayList<DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();
        if (children.size() != inputs.length) {
            throw new ConvException(
                    String.format("Try to convert %s to %s failed, field count not matched(expect %d, real %d).",
                            field.getTypeDescriptor().getFullName(), field.getTypeDescriptor().getFullName(),
                            children.size(), inputs.length));
        }

        JSONObject ret = new JSONObject();
        for (int i = 0; i < inputs.length; ++i) {
            Object fieldVal = pickValueFieldJsonPlainField(ident, children.get(i), false, inputs[i]);
            String varName = getIdentName(children.get(i).getName());
            ret.put(varName, fieldVal);
        }

        if (ret.isEmpty()) {
            return null;
        }

        return ret;
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
