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
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
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
            UECodeInfo codeInfo, HashMap<String, Object> fieldDataByOneof) throws IOException {
        JSONObject jobj = new JSONObject();
        HashSet<String> dumpedFields = null;
        if (isRecursiveEnabled()) {
            dumpedFields = new HashSet<String>();
        }

        UEBuildObject bobj = ((UEBuildObject) buildObj);
        for (int i = 0; i < bobj.header.size() && i < rowData.size(); ++i) {
            if (null != rowData.get(i)) {
                jobj.put(bobj.header.get(i).toString(), rowData.get(i));
                if (null != dumpedFields) {
                    dumpedFields.add(bobj.header.get(i).toString());
                }
            }
        }

        // 需要补全空字段, oneof
        if (null != dumpedFields && null != codeInfo.writerNodeWrapper
                && null != codeInfo.writerNodeWrapper.getTypeDescriptor()) {
            for (DataDstFieldDescriptor field : codeInfo.writerNodeWrapper.getTypeDescriptor().getSortedFields()) {
                Object val = null;
                String varName = getIdentName(field.getName());

                // Write oneof
                if (field.getReferOneof() != null) {
                    String oneofVarName = getIdentName(field.getReferOneof().getName());
                    val = fieldDataByOneof.getOrDefault(varName, null);

                    if (!dumpedFields.contains(oneofVarName)) {
                        dumpedFields.add(oneofVarName);
                        if (val == null) {
                            jobj.put(oneofVarName, ""); // default for oneof case
                        } else {
                            jobj.put(oneofVarName, varName); // default for oneof case
                        }
                    }
                }

                if (dumpedFields.contains(varName)) {
                    continue;
                }
                dumpedFields.add(varName);

                if (val == null) {
                    val = pickValueFieldJsonDefaultImpl(field);
                }
                jobj.put(varName, val);
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
    final protected Object pickValueField(Object buildObj, ArrayList<DataDstWriterNodeWrapper> fieldSet,
            HashMap<String, Object> fieldDataByOneof) throws ConvException {
        return pickValueFieldJsonImpl(fieldSet);
    }

    protected Object pickValueFieldJsonImpl(ArrayList<DataDstWriterNodeWrapper> fieldSet) throws ConvException {
        DataDstWriterNode msgDesc = getFirstWriterNode(fieldSet);
        if (msgDesc == null) {
            return null;
        }

        if (msgDesc.getReferBrothers().isOneof()) {
            return pickValueFieldJsonPlainField(msgDesc.identify, msgDesc.getOneofDescriptor(),
                    msgDesc.identify != null);
        }

        DataDstFieldDescriptor field = getFieldDescriptor(fieldSet);
        if (field == null) {
            return null;
        }
        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
            if (isRecursiveEnabled() && field.isList()) {
                JSONArray ret = new JSONArray();
                for (int i = 0; i < fieldSet.size(); ++i) {
                    Object obj = pickValueFieldJsonStandardImpl(fieldSet.get(i));
                    if (obj != null) {
                        ret.put(obj);
                    }
                }
                return ret;
            } else {
                return pickValueFieldJsonStandardImpl(fieldSet.get(0));
            }
        }

        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
            return pickValueFieldJsonPlainImpl(field, fieldSet);
        }

        return null;
    }

    protected Object pickValueFieldJsonStandardImpl(DataDstWriterNodeWrapper descWrapper) throws ConvException {
        if (null == descWrapper || null == descWrapper.getReferNode()) {
            return null;
        }

        DataDstWriterNode desc = descWrapper.getReferNode();
        if (desc == null) {
            return null;
        }

        // oneof data
        if (descWrapper.getReferOneof() != null) {
            return pickValueFieldJsonPlainField(desc.identify, descWrapper.getReferOneof(), desc.identify != null);
        }

        if (desc.getType() == JAVA_TYPE.MESSAGE) {
            HashSet<String> dumpedFields = null;
            if (isRecursiveEnabled()) {
                dumpedFields = new HashSet<String>();
            }

            JSONObject ret = new JSONObject();
            HashMap<String, Object> fieldDataByOneof = new HashMap<String, Object>();

            if (descWrapper.getChildren() != null) {
                for (ArrayList<DataDstWriterNodeWrapper> child : descWrapper.getChildren()) {
                    if (child.isEmpty()) {
                        continue;
                    }

                    DataDstWriterNodeWrapper firstNode = child.get(0);
                    String varName = firstNode.getVarName();

                    // oneof node
                    if (firstNode.getReferOneof() != null) {
                        if (null != dumpedFields) {
                            dumpedFields.add(varName);
                        }

                        // 如果是生成的节点，case 直接填充固定值
                        if (firstNode.isGenerated()) {
                            if (firstNode.getReferField() != null) {
                                ret.put(varName, getIdentName(firstNode.getReferField().getName()));
                            } else {
                                ret.put(varName, "");
                            }
                            continue;
                        }

                        // oneof 设置 case,缓存field
                        Object res = pickValueFieldJsonImpl(child);
                        if (res instanceof OneofDataObject) {
                            String fieldVarName = getIdentName(((OneofDataObject) res).field.getName());
                            fieldDataByOneof.put(fieldVarName, ((OneofDataObject) res).value);
                            ret.put(varName, fieldVarName);
                        } else {
                            ret.put(varName, res);
                        }
                        continue;
                    }

                    // oneof一定先处理,如果有oneof引用且已经有数据缓存了直接用
                    if (firstNode.getReferOneofNode() != null) {
                        if (fieldDataByOneof.containsKey(varName)) {
                            ret.put(varName, fieldDataByOneof.get(varName));

                            if (null != dumpedFields) {
                                dumpedFields.add(varName);
                            }

                            continue;
                        }
                    }

                    Object val = pickValueFieldJsonImpl(child);
                    if (val != null) {
                        ret.put(varName, val);

                        if (null != dumpedFields) {
                            dumpedFields.add(varName);
                        }
                    }
                }
            }

            // 需要补全空字段
            if (null != dumpedFields) {
                for (DataDstFieldDescriptor subField : desc.getTypeDescriptor().getSortedFields()) {
                    String varName = getIdentName(subField.getName());
                    Object val = null;
                    // Write oneof
                    if (subField.getReferOneof() != null) {
                        String oneofVarName = getIdentName(subField.getReferOneof().getName());
                        val = fieldDataByOneof.getOrDefault(varName, null);

                        if (!dumpedFields.contains(oneofVarName)) {
                            dumpedFields.add(oneofVarName);
                            if (val == null) {
                                ret.put(oneofVarName, ""); // default for oneof case
                            } else {
                                ret.put(oneofVarName, varName); // default for oneof case
                            }
                        }
                    }

                    if (dumpedFields.contains(varName)) {
                        continue;
                    }

                    if (val == null) {
                        val = pickValueFieldJsonDefaultImpl(subField);
                    }
                    ret.put(varName, val);
                }
            }

            return ret;
        }

        return pickValueFieldBaseStandardImpl(desc);
    }

    protected Object pickValueFieldJsonPlainImpl(DataDstFieldDescriptor field,
            ArrayList<DataDstWriterNodeWrapper> fieldSet) throws ConvException {
        if (null == field) {
            return null;
        }

        if (fieldSet.isEmpty()) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        DataDstWriterNode desc = fieldSet.get(0).getReferNode();
        if (desc == null) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        Object ret = pickValueFieldJsonPlainField(desc.identify, desc.getFieldDescriptor(), true);
        if (ret == null) {
            ret = pickValueFieldJsonDefaultImpl(desc.getFieldDescriptor());
        }
        return ret;
    }

    private OneofDataObject pickValueFieldJsonPlainField(IdentifyDescriptor ident, DataDstOneofDescriptor field,
            boolean isTopLevel) throws ConvException {
        if (null == ident) {
            return null;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return null;
        }

        return pickValueFieldJsonPlainField(ident, field, isTopLevel, res.value);
    }

    private OneofDataObject pickValueFieldJsonPlainField(IdentifyDescriptor ident, DataDstOneofDescriptor field,
            boolean isTopLevel, String input) throws ConvException {
        if (field == null) {
            return null;
        }

        Object[] res = parsePlainDataOneof(input, ident, field);
        if (null == res) {
            return null;
        }

        if (res.length < 1) {
            return null;
        }

        DataDstFieldDescriptor sub_field = (DataDstFieldDescriptor) res[0];
        if (sub_field == null) {
            return null;
        }

        if (res.length == 1) {
            return new OneofDataObject(sub_field, pickValueFieldJsonDefaultImpl(sub_field));
        }

        // 非顶层，不用验证类型
        return new OneofDataObject(sub_field, pickValueFieldJsonPlainField(null, sub_field, false, (String) res[1]));
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
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (Long v : values) {
                            tmp.put(v.intValue());
                        }
                    }
                    ret = tmp;
                    break;
                }

                case LONG: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (Long v : values) {
                            tmp.put(v);
                        }
                    }
                    ret = tmp;
                    break;
                }

                case FLOAT: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (Double v : values) {
                            tmp.put(v.floatValue());
                        }
                    }
                    ret = tmp;
                    break;
                }

                case DOUBLE: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (Double v : values) {
                            tmp.put(v);
                        }
                    }
                    ret = tmp;
                    break;
                }

                case BOOLEAN: {
                    Boolean[] values = parsePlainDataBoolean(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (Boolean v : values) {
                            tmp.put(v);
                        }
                    }
                    ret = tmp;
                    break;
                }

                case STRING:
                case BYTES: {
                    String[] values = parsePlainDataString(groups, ident, field);
                    JSONArray tmp = new JSONArray();
                    if (null != values) {
                        for (String v : values) {
                            tmp.put(v);
                        }
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
                    ret = parsePlainDataLong(input.trim(), ident, field).intValue();
                    break;
                }

                case LONG: {
                    ret = parsePlainDataLong(input.trim(), ident, field);
                    break;
                }

                case FLOAT: {
                    ret = parsePlainDataDouble(input.trim(), ident, field).floatValue();
                    break;
                }

                case DOUBLE: {
                    ret = parsePlainDataDouble(input.trim(), ident, field);
                    break;
                }

                case BOOLEAN: {
                    ret = parsePlainDataBoolean(input.trim(), ident, field);
                    break;
                }

                case STRING:
                case BYTES: {
                    ret = parsePlainDataString(input.trim(), ident, field);
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
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        ArrayList<DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();

        HashSet<String> dumpedOneof = null;
        if (field.getTypeDescriptor().getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
        }

        JSONObject ret = new JSONObject();
        int usedInputIdx = 0;
        for (int i = 0; i < children.size(); ++i) {
            if (children.get(i).getReferOneof() != null) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            children.get(i).getName(), field.getTypeDescriptor().getFullName()));
                }

                String varName = getIdentName(children.get(i).getName());
                String oneofVarName = getIdentName(children.get(i).getReferOneof().getName());
                if (dumpedOneof.contains(oneofVarName)) {
                    // already dumped with other field, dump default
                    if (!ret.has(varName)) {
                        ret.put(varName, pickValueFieldJsonDefaultImpl(children.get(i)));
                    }

                    continue;
                }

                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getReferOneof().getName(), field.getTypeDescriptor().getFullName(),
                            usedInputIdx + 1, inputs.length));
                }

                Object res = pickValueFieldJsonPlainField(null, children.get(i).getReferOneof(), false,
                        inputs[usedInputIdx]);
                if (res != null && res instanceof OneofDataObject) {
                    OneofDataObject realRes = ((OneofDataObject) res);
                    String readVarName = getIdentName(realRes.field.getName());
                    ret.put(oneofVarName, readVarName);
                    if (realRes.value != null) {
                        ret.put(readVarName, realRes.value);
                    } else {
                        ret.put(readVarName, pickValueFieldJsonDefaultImpl(realRes.field));
                    }

                    dumpedOneof.add(oneofVarName);
                }

                if (!ret.has(varName)) {
                    ret.put(varName, pickValueFieldJsonDefaultImpl(children.get(i)));
                }

                ++usedInputIdx;
            } else {
                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getName(), field.getTypeDescriptor().getFullName(), usedInputIdx + 1,
                            inputs.length));
                }

                Object fieldVal = pickValueFieldJsonPlainField(ident, children.get(i), false, inputs[usedInputIdx]);
                String varName = getIdentName(children.get(i).getName());
                ret.put(varName, fieldVal);

                ++usedInputIdx;
            }
        }

        if (usedInputIdx != inputs.length) {
            DataSrcImpl current_source = DataSrcImpl.getOurInstance();
            if (null == current_source) {
                ProgramOptions.getLoger().warn("Try to convert %s need %d fields, but provide %d fields.",
                        field.getTypeDescriptor().getFullName(), usedInputIdx, inputs.length);
            } else {
                ProgramOptions.getLoger().warn(
                        "Try to convert %s need %d fields, but provide %d fields.%s  > File: %s, Table: %s, Row: %d, Column: %d",
                        field.getTypeDescriptor().getFullName(), usedInputIdx, inputs.length, ProgramOptions.getEndl(),
                        current_source.getCurrentFileName(), current_source.getCurrentTableName(),
                        current_source.getCurrentRowNum() + 1, current_source.getLastColomnNum() + 1);
            }
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
                HashSet<String> dumpedOneof = null;
                if (fd.getTypeDescriptor().getSortedOneofs().size() > 0) {
                    dumpedOneof = new HashSet<String>();
                }

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

                    ret.put(varName, pickValueFieldJsonDefaultImpl(subField));
                }

                return ret;
            }
            default:
                return null;
        }
    }
}
