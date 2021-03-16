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
        return pickValueFieldJsonImpl(null, fieldSet);
    }

    @SuppressWarnings("unchecked")
    protected Object pickValueFieldJsonImpl(Object reuseOldValue, ArrayList<DataDstWriterNodeWrapper> fieldSet)
            throws ConvException {
        DataDstWriterNode msgDesc = getFirstWriterNode(fieldSet);
        if (msgDesc == null) {
            return null;
        }

        if (msgDesc.getReferBrothers().isOneof()) {
            return pickValueFieldJsonPlainField(reuseOldValue, msgDesc.identify, msgDesc.getOneofDescriptor(), msgDesc);
        }

        DataDstFieldDescriptor field = getFieldDescriptor(fieldSet);
        if (field == null) {
            return null;
        }
        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
            if (isRecursiveEnabled() && field.isMap()) {
                JSONObject ret;
                if (null != reuseOldValue && reuseOldValue instanceof JSONObject) {
                    ret = (JSONObject) reuseOldValue;
                } else {
                    ret = new JSONObject();
                }
                for (int i = 0; i < fieldSet.size(); ++i) {
                    Object obj = pickValueFieldJsonStandardImpl(ret, fieldSet.get(i));
                    if (obj != null && obj instanceof JSONObject) {
                        Object mapKey = null;
                        Object mapValue = null;
                        for (String key : ((JSONObject) obj).keySet()) {
                            if (key.equalsIgnoreCase("key")) {
                                mapKey = ((JSONObject) obj).opt(key);
                            } else if (key.equalsIgnoreCase("value")) {
                                mapValue = ((JSONObject) obj).opt(key);
                            }
                        }

                        if (mapKey != null && mapValue != null) {
                            ret.put(mapKey.toString(), mapValue);
                        }
                    }
                }
                return ret;
            } else if (isRecursiveEnabled() && field.isList()) {
                JSONArray ret;
                if (null != reuseOldValue && reuseOldValue instanceof JSONArray) {
                    ret = (JSONArray) reuseOldValue;
                } else {
                    ret = new JSONArray();
                }
                for (int i = 0; i < fieldSet.size(); ++i) {
                    Object obj = pickValueFieldJsonStandardImpl(ret, fieldSet.get(i));
                    if (null == obj
                            && ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                        obj = getDefault(field);
                    }

                    if (null != obj) {
                        int index = fieldSet.get(i).getReferNode().getListIndex();
                        if (ProgramOptions
                                .getInstance().stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
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
                return ret;
            } else {
                return pickValueFieldJsonStandardImpl(reuseOldValue, fieldSet.get(0));
            }
        }

        if (msgDesc.getReferBrothers().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
            return pickValueFieldJsonPlainImpl(reuseOldValue, field, fieldSet);
        }

        return null;
    }

    protected Object pickValueFieldJsonStandardImpl(Object reuseOldValue, DataDstWriterNodeWrapper descWrapper)
            throws ConvException {
        if (null == descWrapper || null == descWrapper.getReferNode()) {
            return null;
        }

        DataDstWriterNode desc = descWrapper.getReferNode();
        if (desc == null) {
            return null;
        }

        // oneof data
        if (descWrapper.getReferOneof() != null) {
            return pickValueFieldJsonPlainField(reuseOldValue, desc.identify, descWrapper.getReferOneof(), desc);
        }

        if (desc.getType() == JAVA_TYPE.MESSAGE) {
            HashSet<String> dumpedFields = null;
            if (isRecursiveEnabled()) {
                dumpedFields = new HashSet<String>();
            }

            JSONObject ret;
            if (null != reuseOldValue && reuseOldValue instanceof JSONObject) {
                ret = (JSONObject) reuseOldValue;
            } else {
                ret = new JSONObject();
            }
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
                        Object res = pickValueFieldJsonImpl(ret.opt(varName), child);
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

                    Object val = pickValueFieldJsonImpl(ret.opt(varName), child);
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

    protected Object pickValueFieldJsonPlainImpl(Object reuseOldValue, DataDstFieldDescriptor field,
            ArrayList<DataDstWriterNodeWrapper> fieldSet) throws ConvException {
        if (null == field) {
            return null;
        }

        if (fieldSet.isEmpty()) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        for (DataDstWriterNodeWrapper node : fieldSet) {
            Object res = null;
            DataDstWriterNode desc = node.getReferNode();
            if (desc != null) {
                res = pickValueFieldJsonPlainField(reuseOldValue, desc.identify, desc.getFieldDescriptor(), desc);
            }

            if (null != res) {
                reuseOldValue = res;
            } else if (null == reuseOldValue) {
                reuseOldValue = pickValueFieldJsonDefaultImpl(field);
            } else if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                if (reuseOldValue instanceof JSONArray) {
                    ((JSONArray) reuseOldValue).put(getDefault(field));
                }
            }
        }

        return reuseOldValue;
    }

    private OneofDataObject pickValueFieldJsonPlainField(Object reuseOldValue, IdentifyDescriptor ident,
            DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
        if (null == ident) {
            return null;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return null;
        }

        return pickValueFieldJsonPlainField(reuseOldValue, ident, field, maybeFromNode, res.value);
    }

    private OneofDataObject pickValueFieldJsonPlainField(Object reuseOldValue, IdentifyDescriptor ident,
            DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode, String input) throws ConvException {
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
        return new OneofDataObject(sub_field,
                pickValueFieldJsonPlainField(reuseOldValue, null, sub_field, null, (String) res[1]));
    }

    private Object pickValueFieldJsonPlainField(Object reuseOldValue, IdentifyDescriptor ident,
            DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
        if (null == ident) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return pickValueFieldJsonDefaultImpl(field);
        }

        return pickValueFieldJsonPlainField(reuseOldValue, ident, field, maybeFromNode, res.value);
    }

    private Object pickValueFieldJsonPlainField(Object reuseOldValue, IdentifyDescriptor ident,
            DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input) throws ConvException {
        if ((null != maybeFromNode && null != maybeFromNode.identify && !field.isList())
                && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }

        Object ret = null;
        if (field.isList()) {
            if (field.isMap()) {
                if (reuseOldValue != null && reuseOldValue instanceof JSONObject) {
                    ret = (JSONObject) reuseOldValue;
                } else {
                    ret = new JSONObject();
                }
            } else {
                if (reuseOldValue != null && reuseOldValue instanceof JSONArray) {
                    ret = (JSONArray) reuseOldValue;
                } else {
                    ret = new JSONArray();
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
                        JSONObject msg = pickValueFieldJsonPlainField(subGroups, ident, field);
                        if (msg != null) {
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
                        JSONObject msg = pickValueFieldJsonPlainField(subGroups, ident, field);
                        if (msg != null) {
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

            if (field.isMap() && parsedDatas != null) {
                HashMap<Object, Object> parsedMap = (HashMap<Object, Object>) parsedDatas;
                JSONObject valMap = (JSONObject) ret;
                for (HashMap.Entry<Object, Object> pair : parsedMap.entrySet()) {
                    valMap.put(pair.getKey().toString(), pair.getValue());
                }
            } else if (parsedDatas != null) {
                ArrayList<Object> parsedArray = (ArrayList<Object>) parsedDatas;
                JSONArray valArray = (JSONArray) ret;
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (parsedArray.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s[%d] failed, too many elements(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), parsedArray.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
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
            } else if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                JSONArray valArray = (JSONArray) ret;
                valArray.put(getDefault(field));
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

                Object res = pickValueFieldJsonPlainField(null, null, children.get(i).getReferOneof(), null,
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

                Object fieldVal = pickValueFieldJsonPlainField(null, ident, children.get(i), null,
                        inputs[usedInputIdx]);
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

    private Object getDefault(DataDstFieldDescriptor fd) {
        switch (fd.getType()) {
        case INT: {
            return 0;
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

                ret.put(varName, pickValueFieldJsonDefaultImpl(subField));
            }

            return ret;
        }
        default:
            return null;
        }
    }

    protected Object pickValueFieldJsonDefaultImpl(DataDstFieldDescriptor fd) {
        if (fd.isMap()) {
            return new HashMap<Object, Object>();
        } else if (fd.isList()) {
            return new JSONArray();
        }

        return getDefault(fd);
    }
}
