package org.xresloader.core.data.dst;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class DataDstJava extends DataDstImpl {
    private static Pattern strick_identify_rule = Pattern.compile("^[a-zA-Z]\\w*$", Pattern.CASE_INSENSITIVE);

    public class SpecialInnerHashMap<K, V> extends HashMap<K, V> {
        public SpecialInnerHashMap(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        /**
         * Constructs an empty <tt>HashMap</tt> with the specified initial capacity and
         * the default load factor (0.75).
         *
         * @param initialCapacity the initial capacity.
         * @throws IllegalArgumentException if the initial capacity is negative.
         */
        public SpecialInnerHashMap(int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Constructs an empty <tt>HashMap</tt> with the default initial capacity (16)
         * and the default load factor (0.75).
         */
        public SpecialInnerHashMap() {
            super();
        }

        /**
         * Constructs a new <tt>HashMap</tt> with the same mappings as the specified
         * <tt>Map</tt>. The <tt>HashMap</tt> is created with default load factor (0.75)
         * and an initial capacity sufficient to hold the mappings in the specified
         * <tt>Map</tt>.
         *
         * @param m the map whose mappings are to be placed in this map
         * @throws NullPointerException if the specified map is null
         */
        public SpecialInnerHashMap(Map<? extends K, ? extends V> m) {
            super(m);
        }
    }

    static public boolean isStrictIdentify(String input) {
        if (input == null) {
            return false;
        }

        if (input.isEmpty()) {
            return false;
        }

        return strick_identify_rule.matcher(input).matches();
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "java";
    }

    public class DataDstDataSource {
        String file = null;
        String sheet = null;
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
        ArrayList<String> description_list = new ArrayList<String>();
        LinkedList<HashMap<String, Object>> data_source = new LinkedList<HashMap<String, Object>>();
        ret.header.put("data_source", data_source);

        List<Object> item_list = new ArrayList<Object>();
        ret.body.put(SchemeConf.getInstance().getProtoName(), item_list);

        while (DataSrcImpl.getOurInstance().nextTable()) {
            // 生成描述集
            DataDstWriterNode desc = compiler.compile();

            while (DataSrcImpl.getOurInstance().nextRow()) {
                HashMap<String, Object> msg = new HashMap<String, Object>();
                if (dumpMessage(msg, desc)) {
                    item_list.add(msg);
                }
            }

            if (desc.getMessageExtension().description != null) {
                description_list.add(desc.getMessageExtension().description);
            }

            ret.data_message_type = desc.getFullName();
            HashMap<String, Object> new_data_source = new HashMap<String, Object>();
            new_data_source.put("file", DataSrcImpl.getOurInstance().getCurrentFileName());
            new_data_source.put("sheet", DataSrcImpl.getOurInstance().getCurrentTableName());
            data_source.add(new_data_source);
        }

        if (!description_list.isEmpty()) {
            ret.header.put("description", String.join(getSystemEndl(), description_list));
        }

        return ret;
    }

    private Object getDefault(DataDstWriterNode.DataDstFieldDescriptor field) {
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
            for (Map.Entry<String, DataDstWriterNode.DataDstFieldDescriptor> sub_item : field.getTypeDescriptor().fields
                    .entrySet()) {
                // 仅仅Required需要导出默认值
                if (sub_item.getValue().isRequired()) {
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
            return null;
        }
        return val;
    }

    private Object getValueFromDataSource(DataDstWriterNode desc) throws ConvException {
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
        return val;
    }

    private void dumpDefault(HashMap<String, Object> builder, DataDstWriterNode.DataDstChildrenNode as_child) {
        dumpDefault(builder, as_child.innerFieldDesc);
    }

    @SuppressWarnings("unchecked")
    private void dumpDefault(HashMap<String, Object> builder, DataDstWriterNode.DataDstFieldDescriptor field) {
        Object val = getDefault(field);
        if (val == null) {
            return;
        }

        if (field.isMap()) {
            Object mapKey = ((HashMap<String, Object>) val).getOrDefault("key", null);
            Object mapValue = ((HashMap<String, Object>) val).getOrDefault("value", null);
            if (mapKey != null && mapValue != null) {
                SpecialInnerHashMap<Object, Object> old = (SpecialInnerHashMap<Object, Object>) builder
                        .getOrDefault(field.getName(), null);
                if (null == old) {
                    old = new SpecialInnerHashMap<Object, Object>();
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
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), child)) {
                        ret = true;
                    }
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                for (int i = 0; i < c.getValue().nodes.size(); i++) {
                    DataDstWriterNode child = c.getValue().nodes.get(i);
                    if (dumpStandardField(builder, child, c.getValue())) {
                        ret = true;
                    }
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getFieldDescriptor(), child)) {
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
            if (as_child.isRequired()
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, as_child);
            }
            return false;
        }

        Object val = getValueFromDataSource(desc);

        if (null == val) {
            if (as_child.isRequired()
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, as_child);
            }
            return false;
        }

        if (as_child.innerFieldDesc.isMap()) {
            Object mapKey = ((HashMap<String, Object>) val).getOrDefault("key", null);
            Object mapValue = ((HashMap<String, Object>) val).getOrDefault("value", null);
            if (mapKey != null && mapValue != null) {
                SpecialInnerHashMap<Object, Object> old = (SpecialInnerHashMap<Object, Object>) builder
                        .getOrDefault(as_child.innerFieldDesc.getName(), null);
                if (null == old) {
                    old = new SpecialInnerHashMap<Object, Object>();
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

            int index = desc.getListIndex();
            if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                while (old.size() < index) {
                    dumpDefault(builder, as_child);
                }
            }

            if (index > 0 && old.size() > index) {
                old.set(index, val);
            } else {
                old.add(val);
            }
        } else {
            builder.put(as_child.innerFieldDesc.getName(), val);
        }

        return true;
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
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

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode, String input)
            throws ConvException {
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
        return dumpPlainField(builder, null, sub_field, null, (String) res[1]);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
        if (null == ident) {
            // Plain模式的repeated对于STRIP_EMPTY_TAIL也可以直接全部strip掉，下同
            if (field.isRequired()
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, field);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            if (field.isRequired()
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, field);
            }
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value);
    }

    @SuppressWarnings("unchecked")
    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input)
            throws ConvException {

        if ((null != maybeFromNode && null != maybeFromNode.identify && !field.isList())
                && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }

        if (field.isList()) {
            Object val = builder.getOrDefault(field.getName(), null);
            if (field.isMap()) {
                if (val == null || !(val instanceof SpecialInnerHashMap<?, ?>)) {
                    val = new SpecialInnerHashMap<Object, Object>();
                    builder.put(field.getName(), val);
                }
            } else {
                if (val == null || !(val instanceof ArrayList<?>)) {
                    val = new ArrayList<Object>();
                    builder.put(field.getName(), val);
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
                if (values != null) {
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
                if (values != null) {
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
                if (values != null) {
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
                if (values != null) {
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
                if (values != null) {
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
                if (values != null) {
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
                    SpecialInnerHashMap<Object, Object> tmp = new SpecialInnerHashMap<Object, Object>();
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
                        parsedDatas = tmp;
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
                        parsedDatas = tmp;
                    }
                }
                break;
            }

            default:
                break;
            }

            if (field.isMap() && parsedDatas != null) {
                SpecialInnerHashMap<Object, Object> parsedMap = (SpecialInnerHashMap<Object, Object>) parsedDatas;
                SpecialInnerHashMap<Object, Object> valMap = (SpecialInnerHashMap<Object, Object>) val;
                valMap.putAll(parsedMap);
            } else if (parsedDatas != null) {
                ArrayList<Object> parsedArray = (ArrayList<Object>) parsedDatas;
                ArrayList<Object> valArray = (ArrayList<Object>) val;
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (parsedArray.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s[%d] failed, too many elements(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), parsedArray.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                        while (valArray.size() < index) {
                            valArray.add(getDefault(field));
                        }
                    }

                    if (index >= 0 && valArray.size() > index) {
                        valArray.set(index, parsedArray.get(0));
                    } else {
                        valArray.add(parsedArray.get(0));
                    }
                } else {
                    valArray.addAll(parsedArray);
                }
            } else if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                ArrayList<Object> valArray = (ArrayList<Object>) val;
                valArray.add(getDefault(field));
            } else {
                return false;
            }

            return true;
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

            if (val == null) {
                return false;
            }

            builder.put(field.getName(), val);
            return true;
        }
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

                if (dumpPlainField(ret, null, children.get(i).getReferOneof(), null, inputs[usedInputIdx])) {
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

                dumpPlainField(ret, null, children.get(i), null, inputs[usedInputIdx]);

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
