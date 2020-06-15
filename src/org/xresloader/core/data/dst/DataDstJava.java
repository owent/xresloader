package org.xresloader.core.data.dst;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class DataDstJava extends DataDstImpl {
    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "java";
    }

    public class DataDstObject {
        public HashMap<String, Object> header = new HashMap<String, Object>();
        public HashMap<String, List<Object>> body = new HashMap<String, List<Object>>();
        public String data_message_type = "";
    }

    protected DataDstObject build_data(DataDstImpl compiler) throws ConvException {
        DataDstObject ret = new DataDstObject();

        ret.header.put("xres_ver", ProgramOptions.getInstance().getVersion());
        ret.header.put("data_ver", ProgramOptions.getInstance().getDataVersion());
        ret.header.put("count", DataSrcImpl.getOurInstance().getRecordNumber());
        ret.header.put("hash_code", "no hash code");
        ArrayList<String> descriptionList = new ArrayList<String>();

        List<Object> item_list = new ArrayList<Object>();
        ret.body.put(SchemeConf.getInstance().getProtoName(), item_list);

        while (DataSrcImpl.getOurInstance().next_table()) {
            // 生成描述集
            DataDstWriterNode desc = compiler.compile();

            while (DataSrcImpl.getOurInstance().next_row()) {
                HashMap<String, Object> msg = new HashMap<String, Object>();
                if (dumpMessage(msg, desc)) {
                    item_list.add(msg);
                }
            }

            if (desc.getMessageExtension().description != null) {
                descriptionList.add(desc.getMessageExtension().description);
            }

            ret.data_message_type = desc.getFullName();
        }

        if (!descriptionList.isEmpty()) {
            ret.header.put("description", String.join(getSystemEndl(), descriptionList));
        }

        return ret;
    }

    private void dumpDefault(HashMap<String, Object> builder, DataDstWriterNode.DataDstChildrenNode as_child) {
        dumpDefault(builder, as_child.innerFieldDesc);
    }

    @SuppressWarnings("unchecked")
    private void dumpDefault(HashMap<String, Object> builder, DataDstWriterNode.DataDstFieldDescriptor field) {
        Object val = null;
        switch (field.getType()) {
            case INT:
                val = Integer.valueOf(0);
                break;
            case LONG:
                val = Long.valueOf(0);
                break;
            case BOOLEAN:
                val = Boolean.FALSE;
                break;
            case STRING:
                val = "";
                break;
            case BYTES:
                val = new byte[0];
                break;
            case FLOAT:
                val = Float.valueOf(0);
                break;
            case DOUBLE:
                val = Double.valueOf(0);
                break;
            case MESSAGE: {
                HashMap<String, Object> sub_msg = new HashMap<String, Object>();
                for (Map.Entry<String, DataDstWriterNode.DataDstFieldDescriptor> sub_item : field
                        .getTypeDescriptor().fields.entrySet()) {
                    if (sub_item.getValue().isRequired() || ProgramOptions.getInstance().enbleEmptyList) {
                        dumpDefault(sub_msg, sub_item.getValue());
                    }
                }
                val = sub_msg;
                break;
            }
            default:
                break;
        }

        if (null == val) {
            this.logErrorMessage("serialize failed. %s is not supported for java default value",
                    field.getType().toString());
            return;
        }

        if (field.isMap()) {
            Object mapKey = ((HashMap<String, Object>) val).getOrDefault("key", null);
            Object mapValue = ((HashMap<String, Object>) val).getOrDefault("value", null);
            if (mapKey != null && mapValue != null) {
                HashMap<Object, Object> old = (HashMap<Object, Object>) builder.getOrDefault(field.getName(), null);
                if (null == old) {
                    old = new HashMap<Object, Object>();
                    builder.put(field.getName(), old);
                }
                old.put(mapKey, mapValue);
            }
        } else if (field.isList()) {
            ArrayList<Object> old = (ArrayList<Object>) builder.getOrDefault(field.getName(), null);
            if (null == old) {
                old = new ArrayList<Object>();
                builder.put(field.getName(), old);
            }
            old.add(val);
        } else {
            builder.put(field.getName(), val);
        }
    }

    /**
     * 转储数据到builder
     * 
     * @param builder 转储目标
     * @param node    message的描述结构
     * @return 有数据则返回true
     * @throws ConvException
     */
    private boolean dumpMessage(HashMap<String, Object> builder, DataDstWriterNode node) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            if (c.getValue().isOneof()) {
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), true)) {
                        ret = true;
                    }
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpStandardField(builder, child, c.getValue())) {
                        ret = true;
                    }
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getFieldDescriptor(), true)) {
                        ret = true;
                    }
                }
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private boolean dumpStandardField(HashMap<String, Object> builder, DataDstWriterNode desc,
            DataDstWriterNode.DataDstChildrenNode as_child) throws ConvException {
        if (null == desc.identify && DataDstWriterNode.JAVA_TYPE.MESSAGE != desc.getType()) {
            if (ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, as_child);
            }
            return false;
        }

        Object val = null;
        switch (desc.getType()) {
            case INT: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = ret.value.intValue();
                }
                break;
            }

            case LONG: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = ret.value.longValue();
                }
                break;
            }

            case FLOAT: {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    val = ret.value.floatValue();
                }
                break;
            }

            case DOUBLE: {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    val = ret.value.doubleValue();
                }
                break;
            }

            case BOOLEAN: {
                DataContainer<Boolean> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, false);
                if (null != ret && ret.valid) {
                    val = ret.value.booleanValue();
                }
                break;
            }

            case STRING: {
                DataContainer<String> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case BYTES: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != res && res.valid) {
                    String encoding = SchemeConf.getInstance().getKey().getEncoding();
                    if (null == encoding || encoding.isEmpty()) {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes());
                    } else {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes(Charset.forName(encoding)));
                    }
                }
                break;
            }

            case MESSAGE: {
                HashMap<String, Object> node = new HashMap<String, Object>();
                if (dumpMessage(node, desc)) {
                    val = node;
                }
                break;
            }

            default:
                break;
        }

        if (null == val) {
            if (as_child.isRequired() || ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, as_child);
            }
            return false;
        }

        if (as_child.innerFieldDesc.isMap()) {
            Object mapKey = ((HashMap<String, Object>) val).getOrDefault("key", null);
            Object mapValue = ((HashMap<String, Object>) val).getOrDefault("value", null);
            if (mapKey != null && mapValue != null) {
                HashMap<Object, Object> old = (HashMap<Object, Object>) builder
                        .getOrDefault(as_child.innerFieldDesc.getName(), null);
                if (null == old) {
                    old = new HashMap<Object, Object>();
                    builder.put(as_child.innerFieldDesc.getName(), old);
                }
                old.put(mapKey, mapValue);
            }
        } else if (as_child.innerFieldDesc.isList()) {
            ArrayList<Object> old = (ArrayList<Object>) builder.getOrDefault(as_child.innerFieldDesc.getName(), null);
            if (null == old) {
                old = new ArrayList<Object>();
                builder.put(as_child.innerFieldDesc.getName(), old);
            }
            old.add(val);
        } else {
            builder.put(as_child.innerFieldDesc.getName(), val);
        }

        return true;
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, boolean isTopLevel) throws ConvException {
        if (field == null) {
            return false;
        }

        if (null == ident) {
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return false;
        }

        return dumpPlainField(builder, ident, field, isTopLevel, res.value);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, boolean isTopLevel, String input) throws ConvException {
        if (field == null) {
            return false;
        }

        Object[] res = parsePlainDataOneof(input, ident, field);
        if (null == res) {
            return false;
        }

        if (res.length < 1) {
            return false;
        }

        DataDstWriterNode.DataDstFieldDescriptor sub_field = (DataDstWriterNode.DataDstFieldDescriptor) res[0];

        if (sub_field == null) {
            return false;
        }

        if (res.length == 1) {
            dumpDefault(builder, sub_field);
            return true;
        }

        // 非顶层，不用验证类型
        return dumpPlainField(builder, null, sub_field, false, (String) res[1]);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, boolean isTopLevel) throws ConvException {
        if (null == ident) {
            if (ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, field);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            if (field.isRequired() || ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, field);
            }
            return false;
        }

        return dumpPlainField(builder, ident, field, isTopLevel, res.value);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, boolean isTopLevel, String input) throws ConvException {

        if ((isTopLevel && !field.isList()) && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }

        Object val = null;
        if (field.isList()) {
            String[] groups = splitPlainGroups(input.trim(), getPlainFieldSeparator(field));
            switch (field.getType()) {
                case INT: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (Long v : values) {
                        tmp.add(v.intValue());
                    }
                    val = tmp;
                    break;
                }

                case LONG: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (Long v : values) {
                        tmp.add(v);
                    }
                    val = tmp;
                    break;
                }

                case FLOAT: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (Double v : values) {
                        tmp.add(v.floatValue());
                    }
                    val = tmp;
                    break;
                }

                case DOUBLE: {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (Double v : values) {
                        tmp.add(v);
                    }
                    val = tmp;
                    break;
                }

                case BOOLEAN: {
                    Boolean[] values = parsePlainDataBoolean(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (Boolean v : values) {
                        tmp.add(v);
                    }
                    val = tmp;
                    break;
                }

                case STRING:
                case BYTES: {
                    String[] values = parsePlainDataString(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                    }
                    for (String v : values) {
                        tmp.add(v);
                    }
                    val = tmp;
                    break;
                }

                case MESSAGE: {
                    if (field.isMap()) {
                        HashMap<Object, Object> tmp = new HashMap<Object, Object>();
                        for (String v : groups) {
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            HashMap<String, Object> msg = parsePlainDataMessage(subGroups, ident, field);
                            if (msg != null) {
                                Object mapKey = msg.getOrDefault("key", null);
                                Object mapValue = msg.getOrDefault("value", null);
                                if (mapKey != null && mapValue != null) {
                                    tmp.put(mapKey, mapValue);
                                }
                            }
                        }
                        if (!tmp.isEmpty()) {
                            val = tmp;
                        }
                    } else {
                        ArrayList<Object> tmp = new ArrayList<Object>();
                        tmp.ensureCapacity(groups.length);
                        for (String v : groups) {
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            HashMap<String, Object> msg = parsePlainDataMessage(subGroups, ident, field);
                            if (msg != null) {
                                tmp.add(msg);
                            }
                        }
                        if (!tmp.isEmpty()) {
                            val = tmp;
                        }
                    }
                    break;
                }

                default:
                    break;
            }
        } else {
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
                    String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                    val = parsePlainDataMessage(groups, ident, field);
                    if (val == null && field.isRequired()) {
                        dumpDefault(builder, field);
                    }
                    break;
                }

                default:
                    break;
            }
        }

        if (val == null) {
            return false;
        }

        builder.put(field.getName(), val);
        return true;
    }

    public HashMap<String, Object> parsePlainDataMessage(String[] inputs, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        ArrayList<DataDstWriterNode.DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();

        HashSet<String> dumpedOneof = null;
        if (field.getTypeDescriptor().getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();
        int usedInputIdx = 0;
        for (int i = 0; i < children.size(); ++i) {
            if (null != children.get(i).getReferOneof()) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            children.get(i).getName(), field.getTypeDescriptor().getFullName()));
                }
                if (dumpedOneof.contains(children.get(i).getReferOneof().getFullName())) {
                    continue;
                }

                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getReferOneof().getName(), field.getTypeDescriptor().getFullName(),
                            usedInputIdx + 1, inputs.length));
                }

                if (dumpPlainField(ret, null, children.get(i).getReferOneof(), false, inputs[usedInputIdx])) {
                    dumpedOneof.add(children.get(i).getReferOneof().getFullName());
                }

                ++usedInputIdx;
            } else {
                if (usedInputIdx >= inputs.length) {
                    throw new ConvException(String.format(
                            "Try to convert %s of %s failed, field count not matched(expect %d, real %d).",
                            children.get(i).getName(), field.getTypeDescriptor().getFullName(), usedInputIdx + 1,
                            inputs.length));
                }

                dumpPlainField(ret, null, children.get(i), false, inputs[usedInputIdx]);

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
}
