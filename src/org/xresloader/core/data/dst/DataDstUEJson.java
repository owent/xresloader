package org.xresloader.core.data.dst;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.JAVA_TYPE;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Duration;

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
                    dumpDefault(jobj, field);
                }
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
        DataDstWriterNode msgDesc = getFirstWriterNode(fieldSet);
        if (msgDesc == null) {
            return null;
        }

        JSONObject builder = new JSONObject();
        if (msgDesc.getReferBrothers().isOneof()) {
            return dumpPlainOneofField(builder, getIdentName(msgDesc.getOneofDescriptor().getName()), msgDesc.identify,
                    msgDesc.getOneofDescriptor(), msgDesc, null);
        }

        dumpFieldJsonImpl(builder, fieldSet, fieldDataByOneof);
        for (Map.Entry<String, Object> pair : builder.toMap().entrySet()) {
            return pair.getValue();
        }

        if (builder.length() > 1) {
            throw new ConvException(
                    String.format("Pick data of %s with more than 1 value is invalid", msgDesc.getFullName()));
        }

        return null;
    }

    protected boolean dumpFieldJsonImpl(JSONObject builder, ArrayList<DataDstWriterNodeWrapper> fieldSet,
            HashMap<String, Object> fieldDataByOneof) throws ConvException {
        DataDstWriterNode msgDesc = getFirstWriterNode(fieldSet);
        if (msgDesc == null) {
            return false;
        }

        if (msgDesc.getReferBrothers().isOneof()) {
            return null != dumpPlainOneofField(builder, getIdentName(msgDesc.getOneofDescriptor().getName()),
                    msgDesc.identify, msgDesc.getOneofDescriptor(), msgDesc, null);
        }

        DataDstFieldDescriptor field = getFieldDescriptor(fieldSet);
        if (field == null) {
            return false;
        }

        String varName = getIdentName(field.getName());

        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
            if (isRecursiveEnabled() && field.isMap()) {
                JSONObject ret = null;
                Object oldValue = builder.opt(varName);
                if (oldValue != null && oldValue instanceof JSONObject) {
                    ret = (JSONObject) oldValue;
                } else {
                    ret = new JSONObject();
                    builder.put(varName, ret);
                }

                for (int i = 0; i < fieldSet.size(); ++i) {
                    // 不可填充原始的JSONObject，Map结构要特殊处理
                    Object tmp = pickValueFieldJsonStandardImpl(fieldSet.get(i));
                    if (tmp != null && tmp instanceof JSONObject) {
                        Object mapKey = null;
                        Object mapValue = null;
                        for (String key : ((JSONObject) tmp).keySet()) {
                            if (key.equalsIgnoreCase("key")) {
                                mapKey = ((JSONObject) tmp).opt(key);
                            } else if (key.equalsIgnoreCase("value")) {
                                mapValue = ((JSONObject) tmp).opt(key);
                            }
                        }

                        if (mapKey != null && mapValue != null) {
                            ret.put(mapKey.toString(), mapValue);
                        }
                    }
                }

                return !ret.isEmpty();
            } else if (isRecursiveEnabled() && field.isList()) {
                JSONArray ret;
                Object oldValue = builder.opt(varName);
                if (oldValue != null && oldValue instanceof JSONArray) {
                    ret = (JSONArray) oldValue;
                } else {
                    ret = new JSONArray();
                    builder.put(varName, ret);
                }

                ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
                for (int i = 0; i < fieldSet.size(); ++i) {
                    Object obj = pickValueFieldJsonStandardImpl(fieldSet.get(i));
                    if (null == obj
                            && stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                        obj = getDefault(field);
                    }

                    if (null != obj) {
                        int index = fieldSet.get(i).getReferNode().getListIndex();
                        if (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL
                                || stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                            while (ret.length() < index) {
                                ret.put(getDefault(field));
                            }
                        }

                        if (index >= 0 && ret.length() > index) {
                            ret.put(index, obj);
                        } else {
                            ret.put(obj);
                        }
                    }
                }

                return !ret.isEmpty();
            } else {
                Object val = pickValueFieldJsonStandardImpl(fieldSet.get(0));
                if (val == null) {
                    return false;
                }

                if (val instanceof OneofDataObject) {
                    String fieldVarName = getIdentName(((OneofDataObject) val).field.getName());
                    fieldDataByOneof.put(fieldVarName, ((OneofDataObject) val).value);
                    builder.put(varName, fieldVarName);
                } else {
                    builder.put(varName, val);
                }
                return true;
            }
        }

        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
            return dumpPlainFields(builder, field, fieldSet);
        }

        return false;
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
            return dumpPlainOneofField(new JSONObject(), getIdentName(descWrapper.getReferOneof().getName()),
                    desc.identify, descWrapper.getReferOneof(), desc, null);
        }

        if (desc.getType() == JAVA_TYPE.MESSAGE) {
            HashSet<String> dumpedFields = null;
            if (isRecursiveEnabled()) {
                dumpedFields = new HashSet<String>();
            }

            JSONObject ret = new JSONObject();
            HashMap<String, Object> fieldDataByOneof = new HashMap<String, Object>();
            boolean hasChildrenData = false;

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
                        if (dumpFieldJsonImpl(ret, child, fieldDataByOneof)) {
                            hasChildrenData = true;
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
                            hasChildrenData = true;

                            continue;
                        }
                    }

                    if (dumpFieldJsonImpl(ret, child, fieldDataByOneof)) {
                        if (null != dumpedFields) {
                            dumpedFields.add(varName);
                        }
                        hasChildrenData = true;
                    }
                }

                if (!hasChildrenData) {
                    return null;
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
                        dumpDefault(ret, subField);
                    }
                }
            }

            return ret;
        }

        return pickValueFieldBaseStandardImpl(desc);
    }

    protected boolean dumpPlainFields(JSONObject builder, DataDstFieldDescriptor field,
            ArrayList<DataDstWriterNodeWrapper> fieldSet) throws ConvException {
        if (null == field) {
            return false;
        }

        if (fieldSet.isEmpty()) {
            dumpDefault(builder, field);
            return false;
        }

        boolean ret = false;
        for (DataDstWriterNodeWrapper node : fieldSet) {
            DataDstWriterNode desc = node.getReferNode();
            if (desc != null) {
                if (dumpPlainMessageField(builder, desc.identify, desc.getFieldDescriptor(), desc)) {
                    ret = true;
                }
            } else {
                dumpDefault(builder, field);
            }
        }
        return ret;
    }

    private OneofDataObject dumpPlainOneofField(JSONObject builder, String oneofVarName, IdentifyDescriptor ident,
            DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode, HashSet<String> dumpedOneof)
            throws ConvException {
        if (null == ident) {
            return null;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return null;
        }

        return dumpPlainOneofField(builder, oneofVarName, ident, field, maybeFromNode, res.value, dumpedOneof);
    }

    private OneofDataObject dumpPlainOneofField(JSONObject builder, String oneofVarName, IdentifyDescriptor ident,
            DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode, String input, HashSet<String> dumpedOneof)
            throws ConvException {
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

        String readVarName = getIdentName(sub_field.getName());
        builder.put(oneofVarName, readVarName);
        Object value;

        if (res.length == 1) {
            value = dumpDefault(builder, sub_field);
        } else {
            value = dumpPlainMessageField(builder, null, sub_field, null, (String) res[1]);
        }

        if (null != value) {
            builder.put(readVarName, value);
            if (null != dumpedOneof) {
                dumpedOneof.add(oneofVarName);
            }
        }

        return new OneofDataObject(sub_field, value);
    }

    private boolean dumpPlainMessageField(JSONObject builder, IdentifyDescriptor ident, DataDstFieldDescriptor field,
            DataDstWriterNode maybeFromNode) throws ConvException {
        if (null == ident) {
            dumpDefault(builder, field);
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            dumpDefault(builder, field);
            return false;
        }

        return null != dumpPlainMessageField(builder, ident, field, maybeFromNode, res.value);
    }

    /**
     * Dump message field element and return the whole filed
     * 
     * @param builder       container
     * @param ident         identify object
     * @param field         field descriptor
     * @param maybeFromNode maybe from writer node(There is a excel cell)
     * @param input         text data
     * @return All field data, JSONObect for map and JSONArray for list
     * @throws ConvException
     */
    private Object dumpPlainMessageField(JSONObject builder, IdentifyDescriptor ident, DataDstFieldDescriptor field,
            DataDstWriterNode maybeFromNode, String input) throws ConvException {
        if ((null != maybeFromNode && null != maybeFromNode.identify && !field.isList())
                && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return null;
        }

        String varName = getIdentName(field.getName());
        if (field.isList()) {
            Object val = builder.opt(varName);
            if (field.isMap()) {
                if (val == null && !(val instanceof JSONObject)) {
                    val = new JSONObject();
                    builder.put(varName, val);
                }
            } else {
                if (val == null && !(val instanceof JSONArray)) {
                    val = new JSONArray();
                    builder.put(varName, val);
                }
            }

            String[] groups;
            if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                groups = new String[] { input.trim() };
            } else {
                groups = splitPlainGroups(input.trim(), getPlainFieldSeparator(field));
            }

            Object parsedDatas = null;
            switch (field.getType()) {
                case INT: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (Long v : values) {
                            tmp.add(v.intValue());
                        }
                    }

                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case LONG: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (Long v : values) {
                            tmp.add(v);
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case FLOAT: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (Double v : values) {
                            tmp.add(v.floatValue());
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case DOUBLE: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (Double v : values) {
                            tmp.add(v);
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case BOOLEAN: {
                    Boolean[] values = parsePlainDataBoolean(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (Boolean v : values) {
                            tmp.add(v);
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case STRING:
                case BYTES: {
                    String[] values = parsePlainDataString(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (null != values) {
                        tmp.ensureCapacity(values.length);
                        for (String v : values) {
                            tmp.add(v);
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                case MESSAGE: {
                    if (field.isMap()) {
                        HashMap<Object, Object> tmp = new HashMap<Object, Object>();
                        for (String v : groups) {
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            // 和Standard模式一样，Map结构要特殊处理。但是Plain模式本身就是外部Merge的。所以不影响原有结构
                            JSONObject msg = new JSONObject();
                            dumpPlainMessageFields(msg, subGroups, ident, field.getTypeDescriptor());
                            if (!msg.isEmpty()) {
                                Object mapKey = null;
                                Object mapValue = null;
                                for (String key : ((JSONObject) msg).keySet()) {
                                    if (key.equalsIgnoreCase("key")) {
                                        mapKey = ((JSONObject) msg).opt(key);
                                    } else if (key.equalsIgnoreCase("value")) {
                                        mapValue = ((JSONObject) msg).opt(key);
                                    }
                                }

                                if (mapKey != null && mapValue != null) {
                                    tmp.put(mapKey.toString(), mapValue);
                                }
                            }
                        }
                        if (!tmp.isEmpty()) {
                            parsedDatas = tmp;
                        }
                    } else {
                        ArrayList<Object> tmp = new ArrayList<Object>();
                        tmp.ensureCapacity(groups.length);
                        for (String v : groups) {
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            JSONObject msg = new JSONObject();
                            dumpPlainMessageFields(msg, subGroups, ident, field.getTypeDescriptor());
                            if (!msg.isEmpty()) {
                                tmp.add(msg);
                            }
                        }
                        if (!tmp.isEmpty()) {
                            parsedDatas = tmp;
                        }
                    }
                    break;
                }

                default:
                    break;
            }

            if (field.isMap() && parsedDatas != null && parsedDatas instanceof HashMap<?, ?>) {
                HashMap<?, ?> parsedMap = (HashMap<?, ?>) parsedDatas;
                JSONObject valMap = (JSONObject) val;
                for (HashMap.Entry<?, ?> pair : parsedMap.entrySet()) {
                    valMap.put(pair.getKey().toString(), pair.getValue());
                }

                return val;
            } else if (parsedDatas != null && parsedDatas instanceof ArrayList<?>) {
                ArrayList<?> parsedArray = (ArrayList<?>) parsedDatas;
                JSONArray valArray = (JSONArray) val;
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (parsedArray.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s[%d] failed, too many elements(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), parsedArray.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
                    if (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL
                            || stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                        while (valArray.length() < index) {
                            valArray.put(getDefault(field));
                        }
                    }

                    if (index >= 0 && valArray.length() > index) {
                        valArray.put(index, parsedArray.get(0));
                    } else {
                        valArray.put(parsedArray.get(0));
                    }
                } else {
                    for (Object res : parsedArray) {
                        valArray.put(res);
                    }
                }

                return val;
            } else {
                return dumpDefault(builder, field);
            }
        } else {
            Object val = null;
            switch (field.getType()) {
                case INT: {
                    val = parsePlainDataLong(input.trim(), ident, field).intValue();
                    break;
                }

                case LONG: {
                    val = parsePlainDataLong(input.trim(), ident, field);
                    break;
                }

                case FLOAT: {
                    val = parsePlainDataDouble(input.trim(), ident, field).floatValue();
                    break;
                }

                case DOUBLE: {
                    val = parsePlainDataDouble(input.trim(), ident, field);
                    break;
                }

                case BOOLEAN: {
                    val = parsePlainDataBoolean(input.trim(), ident, field);
                    break;
                }

                case STRING:
                case BYTES: {
                    val = parsePlainDataString(input.trim(), ident, field);
                    break;
                }

                case MESSAGE: {
                    if (field.isMap()) {
                        HashMap<Object, Object> res = new HashMap<Object, Object>();
                        String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                        // 和Standard模式一样，Map结构要特殊处理。但是Plain模式本身就是外部Merge的。所以不影响原有结构
                        JSONObject msg = new JSONObject();
                        dumpPlainMessageFields(msg, groups, ident, field.getTypeDescriptor());
                        if (!msg.isEmpty()) {
                            Object mapKey = null;
                            Object mapValue = null;
                            for (String key : ((JSONObject) msg).keySet()) {
                                if (key.equalsIgnoreCase("key")) {
                                    mapKey = ((JSONObject) msg).opt(key);
                                } else if (key.equalsIgnoreCase("value")) {
                                    mapValue = ((JSONObject) msg).opt(key);
                                }
                            }

                            if (mapKey != null && mapValue != null) {
                                res.put(mapKey.toString(), mapValue);
                            }
                        }
                        if (res.isEmpty()) {
                            val = getDefault(field);
                        } else {
                            val = res;
                        }
                    } else {
                        String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                        JSONObject res = new JSONObject();
                        dumpPlainMessageFields(res, groups, ident, field.getTypeDescriptor());
                        if (res.isEmpty()) {
                            val = getDefault(field);
                        } else {
                            val = res;
                        }
                    }
                    break;
                }

                default:
                    break;
            }

            if (val != null) {
                builder.put(varName, val);
            }
            return val;
        }
    }

    private void dumpPlainMessageFields(JSONObject builder, String[] inputs, IdentifyDescriptor ident,
            DataDstTypeDescriptor messageType) throws ConvException {
        if (messageType == null || inputs == null || inputs.length == 0) {
            return;
        }

        ArrayList<DataDstFieldDescriptor> children = messageType.getSortedFields();

        // 几种特殊模式
        if (inputs.length == 1) {
            if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT == messageType
                    .getSpecialMessageType() &&
                    messageType.getFullName() == Timestamp.getDescriptor()
                            .getFullName()) {
                Timestamp res = DataDstPb.parseTimestampFromString(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).getName().equalsIgnoreCase("seconds")
                            && !children.get(i).isList()) {
                        String varName = getIdentName(children.get(i).getName());
                        builder.put(varName, res.getSeconds());
                    } else if (children.get(i).getName().equalsIgnoreCase("nanos")
                            && !children.get(i).isList()) {
                        String varName = getIdentName(children.get(i).getName());
                        builder.put(varName, res.getNanos());
                    }
                }
                return;
            } else if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION == messageType
                    .getSpecialMessageType() &&
                    messageType.getFullName() == Duration.getDescriptor().getFullName()) {
                Duration res = DataDstPb.parseDurationFromString(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).getName().equalsIgnoreCase("seconds")
                            && !children.get(i).isList()) {
                        String varName = getIdentName(children.get(i).getName());
                        builder.put(varName, res.getSeconds());
                    } else if (children.get(i).getName().equalsIgnoreCase("nanos")
                            && !children.get(i).isList()) {
                        String varName = getIdentName(children.get(i).getName());
                        builder.put(varName, res.getNanos());
                    }
                }
                return;
            }
        }

        HashSet<String> dumpedOneof = null;
        if (messageType.getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
        }

        int usedInputIdx = 0;
        for (int i = 0; i < children.size(); ++i) {
            if (children.get(i).getReferOneof() != null) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            children.get(i).getName(), messageType.getFullName()));
                }

                String varName = getIdentName(children.get(i).getName());
                String oneofVarName = getIdentName(children.get(i).getReferOneof().getName());
                if (dumpedOneof.contains(oneofVarName)) {
                    // already dumped with other field, dump default
                    if (!builder.has(varName)) {
                        dumpDefault(builder, children.get(i));
                    }

                    continue;
                }

                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getReferOneof().getName(), messageType.getFullName(), usedInputIdx + 1,
                            inputs.length));
                }

                dumpPlainOneofField(builder, oneofVarName, null, children.get(i).getReferOneof(), null,
                        inputs[usedInputIdx], dumpedOneof);
                if (!builder.has(varName)) {
                    dumpDefault(builder, children.get(i));
                }

                ++usedInputIdx;
            } else {
                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getName(), messageType.getFullName(), usedInputIdx + 1, inputs.length));
                }

                dumpPlainMessageField(builder, ident, children.get(i), null, inputs[usedInputIdx]);
                ++usedInputIdx;
            }
        }

        if (usedInputIdx != inputs.length) {
            DataSrcImpl current_source = DataSrcImpl.getOurInstance();
            if (null == current_source) {
                ProgramOptions.getLoger().warn("Try to convert %s need %d fields, but provide %d fields.",
                        messageType.getFullName(), usedInputIdx, inputs.length);
            } else {
                ProgramOptions.getLoger().warn(
                        "Try to convert %s need %d fields, but provide %d fields.%s  > File: %s, Table: %s, Row: %d, Column: %d",
                        messageType.getFullName(), usedInputIdx, inputs.length, ProgramOptions.getEndl(),
                        current_source.getCurrentFileName(), current_source.getCurrentTableName(),
                        current_source.getCurrentRowNum() + 1, current_source.getLastColomnNum() + 1);
            }
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
            if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
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
