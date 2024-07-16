package org.xresloader.core.data.dst;

import org.apache.commons.codec.binary.Hex;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.et.DataETProcessor;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class DataDstJava extends DataDstImpl {
    private static ThreadLocal<Pattern> strick_identify_rule = ThreadLocal
            .withInitial(() -> Pattern.compile("^[a-zA-Z]\\w*$", Pattern.CASE_INSENSITIVE));

    static private class ParseResult {
        public HashMap<String, Object> value = null;
        public String origin = null;
    }

    public static class SpecialInnerHashMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

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

        return strick_identify_rule.get().matcher(input).matches();
    }

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

    public class DataDstTableContent {
        public DataDstWriterNode descriptor = null;
        public LinkedList<HashMap<String, Object>> rows = new LinkedList<HashMap<String, Object>>();

        public HashMap<String, Object> data_source = new HashMap<String, Object>();
        public String description = null;
        public String data_message_type = "";
    }

    protected HashMap<String, Object> buildCurrentRow(DataDstTableContent table, DataRowContext rowContext)
            throws ConvException {
        if (table.descriptor == null) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();
        boolean dumpSucceed = false;
        if (SchemeConf.getInstance().getCallbackScriptPath().isEmpty()) {
            dumpSucceed = dumpMessage(ret, table.descriptor, rowContext, table.descriptor.getMessageName());
        } else {
            dumpSucceed = DataETProcessor.getInstance().dumpMapMessage(ret, table.descriptor, rowContext,
                    table.descriptor.getMessageName());
        }
        if (dumpSucceed) {
            return ret;
        }

        return null;
    }

    protected DataDstTableContent buildCurrentTable(DataDstImpl compiler, DataTableContext tableContext)
            throws ConvException {
        DataDstTableContent ret = new DataDstTableContent();

        ret.descriptor = compiler.compile();
        int previousRowNum = ret.rows.size();

        int tolerateContinueEmptyRows = ProgramOptions.getInstance().tolerateContinueEmptyRows;
        int currentContinueEmptyRows = 0;
        while (DataSrcImpl.getOurInstance().nextRow()) {
            DataRowContext rowContext = new DataRowContext(DataSrcImpl.getOurInstance().getCurrentFileName(),
                    DataSrcImpl.getOurInstance().getCurrentTableName(),
                    DataSrcImpl.getOurInstance().getCurrentRowNum());

            HashMap<String, Object> msg = buildCurrentRow(ret, rowContext);

            if (msg != null && !rowContext.shouldIgnore()) {
                ret.rows.add(msg);

                tableContext.addUniqueCache(rowContext);
                currentContinueEmptyRows = 0;
            } else {
                if (msg != null && rowContext.shouldIgnore()) {
                    ProgramOptions.getLoger().warn(
                            "File: %s, Sheet: %s, Row: %d%s",
                            rowContext.fileName, rowContext.tableName, rowContext.row,
                            rowContext.buildIgnoreIgnoreMessage(4));
                }
                currentContinueEmptyRows++;
                if (currentContinueEmptyRows > tolerateContinueEmptyRows) {
                    throw new ConvException(String.format(
                            "Too many empty row detected, maybe some cells in file \"%s\" , sheet \"%s\" is set by mistake.Or you can use --tolerate-max-empty-rows to change the bound if it's not a mistake.",
                            DataSrcImpl.getOurInstance().getCurrentFileName(),
                            DataSrcImpl.getOurInstance().getCurrentTableName()));
                }
            }
        }

        if (ret.descriptor.getMessageExtension().description != null) {
            ret.description = ret.descriptor.getMessageExtension().description;
        }

        ret.data_message_type = ret.descriptor.getFullName();
        ret.data_source.put("file", DataSrcImpl.getOurInstance().getCurrentFileName());
        ret.data_source.put("sheet", DataSrcImpl.getOurInstance().getCurrentTableName());
        ret.data_source.put("count", ret.rows.size() - previousRowNum);

        return ret;
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

        DataTableContext tableContext = new DataTableContext();
        while (DataSrcImpl.getOurInstance().nextTable()) {
            DataDstTableContent table = buildCurrentTable(compiler, tableContext);
            if (table == null) {
                continue;
            }

            for (HashMap<String, Object> row : table.rows) {
                item_list.add(row);
            }

            if (table.description != null) {
                description_list.add(table.description);
            }

            if (ret.data_message_type == null || ret.data_message_type.isEmpty()) {
                ret.data_message_type = table.data_message_type;
            }
            data_source.add(table.data_source);
        }

        String validateResult = tableContext.checkUnique();
        if (validateResult != null && !validateResult.isEmpty()) {
            throw new ConvException(validateResult);
        }

        if (!description_list.isEmpty()) {
            ret.header.put("description", String.join(getSystemEndl(), description_list));
        }
        ret.header.replace("count", item_list.size());

        // 校验码
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
            updateHashCode(sha256, item_list);
            ret.header.put("hash_code", "sha256:" + Hex.encodeHexString(sha256.digest()));
        } catch (NoSuchAlgorithmException e) {
            ProgramOptions.getLoger().error("%s", e.getMessage());
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private void updateHashCode(MessageDigest hasher, Object input) {
        if (input instanceof List<?>) {
            for (Object elem : (List<Object>) input) {
                updateHashCode(hasher, elem);
            }
        } else if (input instanceof Map<?, ?>) {
            for (Map.Entry<Object, Object> elem : ((Map<Object, Object>) input).entrySet()) {
                updateHashCode(hasher, elem.getKey());
                updateHashCode(hasher, elem.getValue());
            }
        } else if (input instanceof Double) {
            hasher.update(ByteBuffer.allocate(8).putDouble(((Double) input).doubleValue()).array());
        } else if (input instanceof Float) {
            hasher.update(ByteBuffer.allocate(4).putFloat(((Float) input).floatValue()).array());
        } else if (input instanceof Long) {
            hasher.update(ByteBuffer.allocate(8).putLong(((Long) input).longValue()).array());
        } else if (input instanceof Integer) {
            hasher.update(ByteBuffer.allocate(4).putInt(((Integer) input).intValue()).array());
        } else if (input instanceof Boolean) {
            hasher.update((byte) ((Boolean) input ? 1 : 0));
        } else if (input instanceof String) {
            hasher.update(((String) input).getBytes());
        } else if (input instanceof com.google.protobuf.ByteString) {
            hasher.update(((com.google.protobuf.ByteString) input).toByteArray());
        } else if (input instanceof byte[]) {
            hasher.update((byte[]) input);
        }
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
                for (Map.Entry<String, DataDstWriterNode.DataDstFieldDescriptor> sub_item : field
                        .getTypeDescriptor().fields.entrySet()) {
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

    private Object getValueFromDataSource(DataDstWriterNode desc, DataRowContext rowContext, String fieldPath)
            throws ConvException {
        Object val = null;
        switch (desc.getType()) {
            case INT: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value.intValue();
                }
                break;
            }

            case LONG: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value.longValue();
                }
                break;
            }

            case FLOAT: {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value.floatValue();
                }
                break;
            }

            case DOUBLE: {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value.doubleValue();
                }
                break;
            }

            case BOOLEAN: {
                DataContainer<Boolean> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, false);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value.booleanValue();
                }
                break;
            }

            case STRING: {
                DataContainer<String> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != ret && ret.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value;
                }
                break;
            }

            case BYTES: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != res && res.valid) {
                    String validateErrorMessage = desc.validateTypeLimit(res.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

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
                if (dumpMessage(node, desc, rowContext, fieldPath)) {
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
    protected boolean dumpMessage(HashMap<String, Object> builder, DataDstWriterNode node, DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        boolean ret = false;
        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {

            if (c.getValue().isOneof()) {
                boolean fieldHasValue = false;
                String subFieldPath = String.format("%s.%s", fieldPath, c.getKey());

                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), child, rowContext,
                            subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }
                if (!fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("oneof %s is empty but set not null, we will ignore this row", subFieldPath));
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                boolean fieldHasValue = false;
                for (int i = 0; i < c.getValue().nodes.size(); i++) {
                    DataDstWriterNode child = c.getValue().nodes.get(i);
                    String subFieldPath;
                    if (c.getValue().isList()) {
                        subFieldPath = String.format("%s.%s.%d", fieldPath, c.getKey(), i);
                    } else {
                        subFieldPath = String.format("%s.%s", fieldPath, c.getKey());
                    }
                    if (dumpStandardField(builder, child, c.getValue(), rowContext, subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }
                if (!fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("field %s.%s is empty but set not null, we will ignore this row",
                                    fieldPath, c.getKey()));
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
                boolean fieldHasValue = false;
                String subFieldPath = String.format("%s.%s", fieldPath, c.getKey());

                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getFieldDescriptor(), child, rowContext,
                            subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }
                if (!fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("field %s is empty but set not null, we will ignore this row",
                                    subFieldPath));
                }
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private boolean dumpStandardField(HashMap<String, Object> builder, DataDstWriterNode desc,
            DataDstWriterNode.DataDstChildrenNode as_child, DataRowContext rowContext, String fieldPath)
            throws ConvException {
        if (null == desc.identify && DataDstWriterNode.JAVA_TYPE.MESSAGE != desc.getType()) {
            if (as_child.isRequired()
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, as_child);
            }

            return false;
        }

        Object val = getValueFromDataSource(desc, rowContext, fieldPath);

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

                if (null != as_child.getUniqueTags() && !(mapValue instanceof Map<?, ?>)) {
                    String subFieldPath = String.format("%s.%s", fieldPath, mapKey.toString());
                    for (var tagKey : as_child.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, subFieldPath, mapValue);
                    }
                }
            }
        } else if (as_child.innerFieldDesc.isList()) {
            ArrayList<Object> old = (ArrayList<Object>) builder.getOrDefault(as_child.innerFieldDesc.getName(), null);
            if (null == old) {
                old = new ArrayList<Object>();
                builder.put(as_child.innerFieldDesc.getName(), old);
            }

            int index = desc.getListIndex();
            ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
            if (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL ||
                    stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                while (old.size() < index) {
                    dumpDefault(builder, as_child);
                }
            }

            if (index > 0 && old.size() > index) {
                old.set(index, val);
            } else {
                old.add(val);
            }

            if (null != as_child.getUniqueTags() && !(val instanceof Map<?, ?>)) {
                String subFieldPath = String.format("%s.%d", fieldPath, index);
                for (var tagKey : as_child.getUniqueTags()) {
                    rowContext.addUniqueCache(tagKey, subFieldPath, val);
                }
            }
        } else {
            builder.put(as_child.innerFieldDesc.getName(), val);

            if (null != as_child.getUniqueTags() && !(val instanceof Map<?, ?>)) {
                for (var tagKey : as_child.getUniqueTags()) {
                    rowContext.addUniqueCache(tagKey, fieldPath, val);
                }
            }
        }

        return true;
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
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

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode, String input,
            DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        if (field == null) {
            return false;
        }

        boolean ret = false;
        DataDstWriterNode.DataDstFieldDescriptor sub_field = null;
        do {
            Object[] res = parsePlainDataOneof(input, ident, field);
            if (null == res) {
                break;
            }

            if (res.length < 1) {
                break;
            }

            sub_field = (DataDstWriterNode.DataDstFieldDescriptor) res[0];

            if (sub_field == null) {
                break;
            }

            if (res.length == 1) {
                dumpDefault(builder, sub_field);
                ret = true;
                break;
            }

            // 非顶层，不用验证类型
            ret = dumpPlainField(builder, null, sub_field, null, (String) res[1], rowContext, fieldPath);
        } while (false);

        for (var subField : field.getSortedFields()) {
            if (subField.isNotNull() && subField != sub_field) {
                rowContext.addIgnoreReason(
                        String.format("field %s.%s is empty but set not null, we will ignore this row",
                                fieldPath, subField.getName()));
                break;
            }
        }

        return ret;
    }

    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
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

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    @SuppressWarnings("unchecked")
    private boolean dumpPlainField(HashMap<String, Object> builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input,
            DataRowContext rowContext,
            String fieldPath)
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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                            String validateErrorMessage = field.validateTypeLimit(v);
                            if (null != validateErrorMessage) {
                                throw new ConvException(validateErrorMessage);
                            }

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
                        for (int i = 0; i < groups.length; ++i) {
                            String v = groups[i];
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            ParseResult res = parsePlainDataMessage(subGroups, ident, field, rowContext,
                                    String.format("%s.%d", fieldPath, i));
                            if (res != null && res.value != null) {
                                Object mapKey = res.value.getOrDefault("key", null);
                                Object mapValue = res.value.getOrDefault("value", null);
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
                        DataDstWriterNode.DataDstFieldDescriptor referOriginField = field
                                .getReferOriginField();
                        ArrayList<Object> referOrigin = null;
                        if (referOriginField != null) {
                            Object refer = builder.getOrDefault(referOriginField.getName(), null);
                            if (refer != null && refer instanceof ArrayList<?>) {
                                referOrigin = (ArrayList<Object>) refer;
                            }
                        }
                        for (int i = 0; i < groups.length; ++i) {
                            String v = groups[i];
                            String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                            ParseResult res = parsePlainDataMessage(subGroups, ident, field, rowContext,
                                    String.format("%s.%d", fieldPath, i));
                            if (res != null && res.value != null) {
                                tmp.add(res.value);

                                if (res.origin != null && referOriginField != null) {
                                    if (referOrigin == null) {
                                        referOrigin = new ArrayList<Object>();
                                        referOrigin.ensureCapacity(groups.length);
                                        builder.put(referOriginField.getName(), referOrigin);
                                    }
                                    while (referOrigin.size() <= i) {
                                        referOrigin.add("");
                                    }
                                    referOrigin.set(i, res.origin);
                                }
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

                if (null != field.getUniqueTags()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, valMap);
                    }
                }
            } else if (parsedDatas != null) {
                ArrayList<Object> parsedArray = (ArrayList<Object>) parsedDatas;
                ArrayList<Object> valArray = (ArrayList<Object>) val;
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (parsedArray.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s.%d failed, too many elements(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), parsedArray.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
                    if (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL
                            || stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
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

                if (null != field.getUniqueTags() && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, valArray);
                    }
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
                    Long res = parsePlainDataLong(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(res);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    val = res.intValue();
                    break;
                }

                case LONG: {
                    val = parsePlainDataLong(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(val);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    break;
                }

                case FLOAT: {
                    Double res = parsePlainDataDouble(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(res);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    val = res.floatValue();
                    break;
                }

                case DOUBLE: {
                    val = parsePlainDataDouble(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(val);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    break;
                }

                case BOOLEAN: {
                    val = parsePlainDataBoolean(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(val);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    break;
                }

                case STRING:
                case BYTES: {
                    val = parsePlainDataString(input.trim(), ident, field);
                    String validateErrorMessage = field.validateTypeLimit(val);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }
                    break;
                }

                case MESSAGE: {
                    String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                    ParseResult res = parsePlainDataMessage(groups, ident, field, rowContext, fieldPath);
                    if (res != null && res.value != null) {
                        val = res.value;
                        if (res.origin != null && field.getReferOriginField() != null) {
                            builder.put(field.getReferOriginField().getName(), res.origin);
                        }
                    } else if (field.isRequired()) {
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

            if (null != field.getUniqueTags() && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                for (var tagKey : field.getUniqueTags()) {
                    rowContext.addUniqueCache(tagKey, fieldPath, val);
                }
            }
            return true;
        }
    }

    public ParseResult parsePlainDataMessage(String[] inputs, IdentifyDescriptor ident,
            org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor field,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        ArrayList<DataDstWriterNode.DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();

        HashSet<String> dumpedOneof = null;
        if (field.getTypeDescriptor().getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
        }

        ParseResult ret = new ParseResult();
        ret.value = new HashMap<String, Object>();

        // 几种特殊模式
        if (inputs.length == 1) {
            if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDatetime(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).getName().equalsIgnoreCase("seconds")
                            && !children.get(i).isList()) {
                        ret.value.put(children.get(i).getName(), res.toEpochMilli() / 1000);
                    } else if (children.get(i).getName().equalsIgnoreCase("nanos")
                            && !children.get(i).isList()) {
                        ret.value.put(children.get(i).getName(), res.getNano());
                    }
                }

                ret.origin = inputs[0];
                return ret;
            } else if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDuration(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).getName().equalsIgnoreCase("seconds")
                            && !children.get(i).isList()) {
                        ret.value.put(children.get(i).getName(),
                                res.toEpochMilli() / 1000);
                    } else if (children.get(i).getName().equalsIgnoreCase("nanos")
                            && !children.get(i).isList()) {
                        ret.value.put(children.get(i).getName(), res.getNano());
                    }
                }

                ret.origin = inputs[0];
                return ret;
            }
        }

        int usedInputIdx = 0;
        int fieldSize = 0;
        int atLeastFieldSize = 0;
        HashMap<Integer, DataDstFieldDescriptor> missingFields = new HashMap<>();
        for (int i = 0; i < children.size(); ++i) {
            DataDstFieldDescriptor child = children.get(i);
            if (child.allowMissingInPlainMode()) {
                continue;
            }

            if (child.getReferOneof() != null) {
                if (child.getReferOneof().allowMissingInPlainMode()) {
                    continue;
                }
            }

            missingFields.put(child.getIndex(), child);
            atLeastFieldSize += 1;
        }

        for (int i = 0; i < children.size(); ++i) {
            DataDstFieldDescriptor child = children.get(i);

            if (child.getLinkedValueField() != null) {
                ++fieldSize;
                continue;
            }

            if (null != child.getReferOneof()) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            child.getName(), field.getTypeDescriptor().getFullName()));
                }
                if (dumpedOneof.contains(child.getReferOneof().getFullName())) {
                    continue;
                }

                if (usedInputIdx < inputs.length
                        && dumpPlainField(ret.value, null, child.getReferOneof(), null, inputs[usedInputIdx],
                                rowContext, fieldPath)) {
                    dumpedOneof.add(child.getReferOneof().getFullName());

                    for (var subField : child.getReferOneof().getSortedFields()) {
                        missingFields.remove(subField.getIndex());
                    }
                } else {
                    if (child.isNotNull()) {
                        rowContext.addIgnoreReason(
                                String.format("oneof %s.%s is empty but set not null, we will ignore this row",
                                        fieldPath, child.getName()));
                    }
                }

                ++fieldSize;
                ++usedInputIdx;
            } else {
                String subFieldPath = String.format("%s.%s", fieldPath, children.get(i).getName());
                if (usedInputIdx < inputs.length
                        && dumpPlainField(ret.value, null, children.get(i), null, inputs[usedInputIdx], rowContext,
                                subFieldPath)) {
                    missingFields.remove(child.getIndex());
                } else {
                    if (children.get(i).isNotNull()) {
                        rowContext.addIgnoreReason(
                                String.format("field %s is empty but set not null, we will ignore this row",
                                        subFieldPath));
                    }
                }

                ++fieldSize;
                ++usedInputIdx;
            }
        }

        if (!missingFields.isEmpty()) {
            String message = String.format(
                    "Try to convert %s need at least %d fields, at most %d fields, but only provide %d fields.%s  > Missing fields: %s",
                    field.getTypeDescriptor().getFullName(), atLeastFieldSize, fieldSize, inputs.length,
                    ProgramOptions.getEndl(),
                    String.join(",", missingFields.values().stream().map(DataDstFieldDescriptor::getName)
                            .collect(Collectors.toList())));
            ProgramOptions.getLoger().warn(message);
        } else if (inputs.length > fieldSize) {
            String message = String.format(
                    "Try to convert %s need at least %d fields, at most %d fields, but provide %d fields.",
                    field.getTypeDescriptor().getFullName(), atLeastFieldSize, fieldSize, inputs.length,
                    ProgramOptions.getEndl());
            throw new ConvException(message);
        }

        if (ret.value.isEmpty()) {
            return null;
        }

        return ret;
    }
}
