package org.xresloader.core.data.dst;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.engine.IdentifyDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by owentou on 2014/10/11.
 */
public class DataDstWriterNode {
    public enum JAVA_TYPE {
        INT, LONG, BOOLEAN, STRING, BYTES, FLOAT, DOUBLE, MESSAGE, UNKNOWN
    }

    public enum SPECIAL_MESSAGE_TYPE {
        NONE, MAP, TIMEPOINT, DURATION
    }

    public enum SPECIAL_TYPE_LIMIT {
        NONE, INT32, UINT32
    }

    public enum FIELD_LABEL_TYPE {
        OPTIONAL, LIST, REQUIRED
    }

    public enum CHILD_NODE_TYPE {
        STANDARD, PLAIN
    }

    static public class DataDstFieldExtUE {
        public long keyTag = 0;
        public String ueTypeName = null;
        public boolean ueTypeIsClass = false;
        public String ueOriginTypeName = null;
        public String ueOriginTypeDefaultValue = null;
    }

    public enum ListStripRule {
        DEFAULT, STRIP_TAIL, STRIP_NOTHING, STRIP_ALL
    }

    static public class DataDstFieldExtList {
        public ListStripRule stripOption = ListStripRule.DEFAULT;
        public int minSize = 0;
        public int maxSize = 0;
        public boolean strictSize = false;
    }

    static public class DataDstFieldExt {
        public String description = null;
        public String validator = null;
        public String plainSeparator = null;
        public int ratio = 1;
        public boolean notNull = false;
        public boolean allowMissingInPlainMode = false;
        public ArrayList<String> uniqueTags = null;
        public HashSet<String> fieldTags = null;
        private Boolean withIgnoredFieldTagsValueCache = null;
        private String[] withIgnoredFieldTagsCheckCache = null;
        private DataDstFieldExtUE ue = null;
        private DataDstFieldExtList list = null;

        public DataDstFieldExtUE mutableUE() {
            if (null != ue) {
                return ue;
            }

            ue = new DataDstFieldExtUE();
            return ue;
        }

        public DataDstFieldExtList mutableList() {
            if (null != list) {
                return list;
            }

            list = new DataDstFieldExtList();
            return list;
        }

        public DataDstFieldExtList getList() {
            return list;
        }
    }

    static public class DataDstOneofExt {
        public String description = null;
        public String plainSeparator = null;
        public boolean notNull = false;
        public boolean allowMissingInPlainMode = false;
        public HashSet<String> fieldTags = null;
        private Boolean withIgnoredFieldTagsValueCache = null;
        private String[] withIgnoredFieldTagsCheckCache = null;
    }

    static public class DataDstMessageExtUE {
        public String helper = null;
        public boolean notDataTable = false;
        public Boolean enableDefaultLoader = null;
        public List<String> includeHeader = null;
    }

    static public class DataDstMessageExt {
        public String description = null;
        public String plainSeparator = null;
        // public List<Xresloader.IndexDescriptor> kvIndex = null;
        // public List<Xresloader.IndexDescriptor> klIndex = null;

        private DataDstMessageExtUE ue = null;

        public DataDstMessageExtUE mutableUE() {
            if (null != ue) {
                return ue;
            }

            ue = new DataDstMessageExtUE();
            return ue;
        }
    }

    static public ListStripRule getDefaultListStripRule() {
        if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
            return ListStripRule.STRIP_NOTHING;
        }

        if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
            return ListStripRule.STRIP_TAIL;
        }

        return ListStripRule.STRIP_ALL;
    }

    static public class DataDstFieldDescriptor {
        private int index = 0;
        private String name = null;
        private FIELD_LABEL_TYPE label = FIELD_LABEL_TYPE.OPTIONAL;
        private DataDstTypeDescriptor referTypeDescriptor = null;
        private DataDstFieldExt extension = null;
        private List<DataVerifyImpl> verifyEngine = null;
        private Object rawDescriptor = null;
        private DataDstOneofDescriptor referOneofDescriptor = null;

        private DataDstFieldDescriptor referOriginFieldDescriptor = null;
        private DataDstFieldDescriptor linkValueFieldDescriptor = null;

        public DataDstFieldDescriptor(DataDstTypeDescriptor typeDesc, int index, String name, FIELD_LABEL_TYPE label,
                Object rawDesc) {
            this.referTypeDescriptor = typeDesc;
            this.index = index;
            this.name = name;
            this.label = label;

            this.rawDescriptor = rawDesc;
        }

        public Object getRawDescriptor() {
            return rawDescriptor;
        }

        public boolean hasValidator() {
            return null != verifyEngine && false == verifyEngine.isEmpty();
        }

        public void resetValidator() {
            verifyEngine = null;
        }

        public void addValidator(DataVerifyImpl ver) {
            if (null == ver) {
                return;
            }

            if (null == verifyEngine) {
                verifyEngine = new LinkedList<DataVerifyImpl>();
            }

            verifyEngine.add(ver);
        }

        public List<DataVerifyImpl> getValidator() {
            return verifyEngine;
        }

        public JAVA_TYPE getType() {
            return this.referTypeDescriptor.getType();
        }

        public DataDstTypeDescriptor getTypeDescriptor() {
            return this.referTypeDescriptor;
        }

        public int getIndex() {
            return this.index;
        }

        public String getName() {
            return this.name;
        }

        public boolean isList() {
            return this.label == FIELD_LABEL_TYPE.LIST;
        }

        public boolean isMap() {
            return this.label == FIELD_LABEL_TYPE.LIST && this.referTypeDescriptor != null
                    && SPECIAL_MESSAGE_TYPE.MAP == this.referTypeDescriptor.getSpecialMessageType();
        }

        public SPECIAL_MESSAGE_TYPE getSpecialMessageType() {
            if (this.referTypeDescriptor != null) {
                return this.referTypeDescriptor.getSpecialMessageType();
            }

            return SPECIAL_MESSAGE_TYPE.NONE;
        }

        public SPECIAL_TYPE_LIMIT getSpecialTypeLimit() {
            if (this.referTypeDescriptor != null) {
                return this.referTypeDescriptor.getSpecialTypeLimit();
            }

            return SPECIAL_TYPE_LIMIT.NONE;
        }

        public String validateTypeLimit(Object value) {
            if (this.referTypeDescriptor != null) {
                return this.referTypeDescriptor.validateTypeLimit(value);
            }

            return null;
        }

        public ArrayList<String> getUniqueTags() {
            if (this.extension == null) {
                return null;
            }

            return this.extension.uniqueTags;
        }

        public HashSet<String> getFieldTags() {
            if (this.extension == null) {
                return null;
            }

            return this.extension.fieldTags;
        }

        public boolean containsFieldTags(String[] tags) {
            if (this.extension == null || tags == null) {
                if (null != this.referOneofDescriptor) {
                    return this.referOneofDescriptor.containsFieldTags(tags);
                } else {
                    return false;
                }
            }

            if (this.extension.fieldTags == null) {
                if (null != this.referOneofDescriptor) {
                    return this.referOneofDescriptor.containsFieldTags(tags);
                } else {
                    return false;
                }
            }

            if (this.extension.withIgnoredFieldTagsValueCache != null
                    && this.extension.withIgnoredFieldTagsCheckCache == tags) {
                return this.extension.withIgnoredFieldTagsValueCache;
            }

            this.extension.withIgnoredFieldTagsCheckCache = tags;
            this.extension.withIgnoredFieldTagsValueCache = false;
            for (var tag : tags) {
                if (this.extension.fieldTags.contains(tag)) {
                    this.extension.withIgnoredFieldTagsValueCache = true;
                    break;
                }
            }

            if (null != this.referOneofDescriptor) {
                if (this.referOneofDescriptor.containsFieldTags(tags)) {
                    this.extension.withIgnoredFieldTagsValueCache = true;
                }
            }

            return this.extension.withIgnoredFieldTagsValueCache;
        }

        public boolean isNotNull() {
            if (this.extension == null) {
                return false;
            }

            return this.extension.notNull;
        }

        public boolean allowMissingInPlainMode() {
            if (this.extension == null) {
                return false;
            }

            return this.extension.allowMissingInPlainMode;
        }

        public boolean isRequired() {
            return this.label == FIELD_LABEL_TYPE.REQUIRED;
        }

        public DataDstFieldExt mutableExtension() {
            if (null != extension) {
                return extension;
            }

            extension = new DataDstFieldExt();
            return extension;
        }

        public DataDstFieldExtList getListExtension() {
            if (this.extension == null) {
                return null;
            }

            return this.extension.getList();
        }

        public ListStripRule getListStripRule() {
            if (this.extension == null) {
                return getDefaultListStripRule();
            }

            if (this.extension.list == null) {
                return getDefaultListStripRule();
            }

            if (this.extension.list.stripOption == ListStripRule.DEFAULT) {
                return getDefaultListStripRule();
            }

            return this.extension.list.stripOption;
        }

        public void setReferOneof(DataDstOneofDescriptor fd) {
            this.referOneofDescriptor = fd;
        }

        public DataDstOneofDescriptor getReferOneof() {
            return this.referOneofDescriptor;
        }

        public DataDstFieldDescriptor getReferOriginField() {
            return this.referOriginFieldDescriptor;
        }

        public DataDstFieldDescriptor getLinkedValueField() {
            return this.linkValueFieldDescriptor;
        }

        public void setReferOriginField(DataDstFieldDescriptor referTo) {
            if (this.referOriginFieldDescriptor != null) {
                this.referOriginFieldDescriptor.linkValueFieldDescriptor = null;
            }

            this.referOriginFieldDescriptor = referTo;
            if (referTo != null) {
                referTo.linkValueFieldDescriptor = this;
            }
        }
    }

    static public class DataDstOneofDescriptor {
        private int index = 0;
        private String name = null;
        private String fullName = null;
        private HashMap<String, DataDstFieldDescriptor> fields_by_name = null;
        private HashMap<Integer, DataDstFieldDescriptor> fields_by_id = null;
        private ArrayList<DataDstFieldDescriptor> sortedFields = null;
        private DataDstTypeDescriptor owner = null;
        private DataDstOneofExt extension = null;
        private List<DataVerifyImpl> verifyEngine = null;
        private Object rawDescriptor = null;

        public DataDstOneofDescriptor(DataDstTypeDescriptor owner, HashMap<String, DataDstFieldDescriptor> fields,
                int index, String name, Object rawDesc) {
            this.owner = owner;
            this.fields_by_name = fields;
            this.index = index;
            this.name = name;

            this.rawDescriptor = rawDesc;

            this.fields_by_id = new HashMap<Integer, DataDstFieldDescriptor>();
            for (HashMap.Entry<String, DataDstFieldDescriptor> d : fields.entrySet()) {
                this.fields_by_id.put(d.getValue().getIndex(), d.getValue());
                d.getValue().setReferOneof(this);
            }
            this.fullName = String.format("%s.%s", owner.getFullName(), name);
        }

        public Object getRawDescriptor() {
            return rawDescriptor;
        }

        public boolean hasValidator() {
            return null != verifyEngine && false == verifyEngine.isEmpty();
        }

        public void resetValidator() {
            verifyEngine = null;
        }

        public void addValidator(DataVerifyImpl ver) {
            if (null == ver) {
                return;
            }

            if (null == verifyEngine) {
                verifyEngine = new LinkedList<DataVerifyImpl>();
            }

            verifyEngine.add(ver);
        }

        public List<DataVerifyImpl> getValidator() {
            return verifyEngine;
        }

        public DataDstTypeDescriptor getOwnerDescriptor() {
            return this.owner;
        }

        public int getIndex() {
            return this.index;
        }

        public String getName() {
            return this.name;
        }

        public String getFullName() {
            return this.fullName;
        }

        public DataDstOneofExt mutableExtension() {
            if (null != extension) {
                return extension;
            }

            extension = new DataDstOneofExt();
            return extension;
        }

        public DataDstFieldDescriptor getFieldById(int index) {
            return fields_by_id.getOrDefault(index, null);
        }

        public DataDstFieldDescriptor getFieldByName(String name) {
            return fields_by_name.getOrDefault(name, null);
        }

        public ArrayList<DataDstFieldDescriptor> getSortedFields() {
            if (this.fields_by_name == null) {
                return null;
            }

            if (this.sortedFields != null && this.sortedFields.size() == this.fields_by_name.size()) {
                return this.sortedFields;
            }

            this.sortedFields = new ArrayList<DataDstFieldDescriptor>();
            this.sortedFields.ensureCapacity(this.fields_by_name.size());
            for (HashMap.Entry<String, DataDstFieldDescriptor> d : this.fields_by_name.entrySet()) {
                this.sortedFields.add(d.getValue());
            }
            this.sortedFields.sort((l, r) -> {
                return Integer.compare(l.getIndex(), r.getIndex());
            });
            return this.sortedFields;
        }

        public boolean isNotNull() {
            if (this.extension == null) {
                return false;
            }

            return this.extension.notNull;
        }

        public boolean allowMissingInPlainMode() {
            if (this.extension == null) {
                return false;
            }

            return this.extension.allowMissingInPlainMode;
        }

        public HashSet<String> getFieldTags() {
            if (this.extension == null) {
                return null;
            }

            return this.extension.fieldTags;
        }

        public boolean containsFieldTags(String[] tags) {
            if (this.extension == null || tags == null) {
                return false;
            }

            if (this.extension.fieldTags == null) {
                return false;
            }

            if (this.extension.withIgnoredFieldTagsValueCache != null
                    && this.extension.withIgnoredFieldTagsCheckCache == tags) {
                return this.extension.withIgnoredFieldTagsValueCache;
            }

            this.extension.withIgnoredFieldTagsCheckCache = tags;
            this.extension.withIgnoredFieldTagsValueCache = false;
            for (var tag : tags) {
                if (this.extension.fieldTags.contains(tag)) {
                    this.extension.withIgnoredFieldTagsValueCache = true;
                    break;
                }
            }

            return this.extension.withIgnoredFieldTagsValueCache;
        }
    }

    static public class DataDstTypeDescriptor {
        private JAVA_TYPE type = JAVA_TYPE.INT;
        private SPECIAL_MESSAGE_TYPE special_message_type = SPECIAL_MESSAGE_TYPE.NONE;
        private SPECIAL_TYPE_LIMIT special_type_limit = SPECIAL_TYPE_LIMIT.NONE;
        private String packageName = null;
        private String messageName = null;
        private String fullName = null;
        private DataDstMessageExt extension = null;
        public HashMap<String, DataDstFieldDescriptor> fields = null;
        public HashMap<String, DataDstOneofDescriptor> oneofs = null;
        private ArrayList<DataDstFieldDescriptor> sortedFields = null;
        private ArrayList<DataDstOneofDescriptor> sortedOneofs = null;
        private Object rawDescriptor = null;

        public DataDstTypeDescriptor(JAVA_TYPE type, String pkgName, String msgName, Object rawDesc,
                SPECIAL_MESSAGE_TYPE specialMessageType, SPECIAL_TYPE_LIMIT specialTypeLimit) {
            this.type = type;
            if (msgName == null || msgName.isEmpty()) {
                msgName = type.toString();
            }

            this.packageName = pkgName;
            this.messageName = msgName;
            if (this.packageName == null || this.packageName.isEmpty()) {
                this.fullName = msgName;
            } else {
                this.fullName = String.format("%s.%s", this.packageName, this.messageName);
            }
            this.rawDescriptor = rawDesc;

            this.special_message_type = specialMessageType;
            this.special_type_limit = specialTypeLimit;
        }

        public Object getRawDescriptor() {
            return rawDescriptor;
        }

        public JAVA_TYPE getType() {
            return this.type;
        }

        public SPECIAL_MESSAGE_TYPE getSpecialMessageType() {
            return this.special_message_type;
        }

        public SPECIAL_TYPE_LIMIT getSpecialTypeLimit() {
            return this.special_type_limit;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public String getMessageName() {
            return this.messageName;
        }

        public String getFullName() {
            return this.fullName;
        }

        public String validateTypeLimit(Object value) {
            switch (getSpecialTypeLimit()) {
                case INT32:
                    if (value instanceof Long) {
                        long v = (long) value;
                        if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
                            return String.format("Value %d out of int32 range", v);
                        }
                    }
                    break;
                case UINT32:
                    if (value instanceof Long) {
                        long v = (long) value;
                        if (v > 0xFFFFFFFFL || v < 0) {
                            return String.format("Value %d out of uint32 range", v);
                        }
                    }
                    break;
                default:
                    break;
            }

            return null;
        }

        public DataDstMessageExt mutableExtension() {
            if (null != extension) {
                return extension;
            }

            extension = new DataDstMessageExt();
            return extension;
        }

        public boolean hasChildrenFields() {
            return fields != null && false == fields.isEmpty();
        }

        public ArrayList<DataDstFieldDescriptor> getSortedFields() {
            if (fields == null) {
                return null;
            }

            if (sortedFields != null && sortedFields.size() == fields.size()) {
                return sortedFields;
            }

            sortedFields = new ArrayList<DataDstFieldDescriptor>();
            sortedFields.ensureCapacity(fields.size());
            for (HashMap.Entry<String, DataDstFieldDescriptor> d : fields.entrySet()) {
                sortedFields.add(d.getValue());
            }
            sortedFields.sort((l, r) -> {
                return Integer.compare(l.getIndex(), r.getIndex());
            });
            return sortedFields;
        }

        public DataDstFieldDescriptor getChildField(String name) {
            if (fields == null) {
                return null;
            }

            return fields.getOrDefault(name, null);
        }

        public DataDstFieldDescriptor getMapKeyField() {
            if (SPECIAL_MESSAGE_TYPE.MAP != getSpecialMessageType()) {
                return null;
            }

            for (HashMap.Entry<String, DataDstFieldDescriptor> d : fields.entrySet()) {
                if (d.getKey().equalsIgnoreCase("key")) {
                    return d.getValue();
                }
            }

            return null;
        }

        public DataDstFieldDescriptor getMapValueField() {
            if (SPECIAL_MESSAGE_TYPE.MAP != getSpecialMessageType()) {
                return null;
            }

            for (HashMap.Entry<String, DataDstFieldDescriptor> d : fields.entrySet()) {
                if (d.getKey().equalsIgnoreCase("value")) {
                    return d.getValue();
                }
            }

            return null;
        }

        public ArrayList<DataDstOneofDescriptor> getSortedOneofs() {
            if (fields == null) {
                return null;
            }

            if (sortedOneofs != null && sortedOneofs.size() == fields.size()) {
                return sortedOneofs;
            }

            sortedOneofs = new ArrayList<DataDstOneofDescriptor>();
            sortedOneofs.ensureCapacity(fields.size());
            for (HashMap.Entry<String, DataDstOneofDescriptor> d : oneofs.entrySet()) {
                sortedOneofs.add(d.getValue());
            }
            sortedOneofs.sort((l, r) -> {
                return Integer.compare(l.getIndex(), r.getIndex());
            });
            return sortedOneofs;
        }

        public DataDstOneofDescriptor getChildOneof(String name) {
            if (oneofs == null) {
                return null;
            }

            return oneofs.getOrDefault(name, null);
        }

    }

    public static class DataDstChildrenNode {
        public DataDstFieldDescriptor innerFieldDesc = null;
        public DataDstOneofDescriptor innerOneofDesc = null;
        public CHILD_NODE_TYPE mode = CHILD_NODE_TYPE.STANDARD;
        public Object rawDescriptor = null;
        public ArrayList<DataDstWriterNode> nodes = null;

        public boolean isNotNull() {
            if (this.innerFieldDesc != null) {
                return this.innerFieldDesc.isNotNull();
            }

            if (this.innerOneofDesc != null) {
                return this.innerOneofDesc.isNotNull();
            }

            return false;
        }

        public ArrayList<String> getUniqueTags() {
            if (this.innerFieldDesc == null) {
                return null;
            }

            return this.innerFieldDesc.getUniqueTags();
        }

        public HashSet<String> getFieldTags() {
            if (this.innerFieldDesc != null) {
                return this.innerFieldDesc.getFieldTags();
            }

            if (this.innerOneofDesc != null) {
                return this.innerOneofDesc.getFieldTags();
            }

            return null;
        }

        public boolean containsFieldTags(String[] tags) {
            if (this.innerFieldDesc != null) {
                return this.innerFieldDesc.containsFieldTags(tags);
            }

            if (this.innerOneofDesc != null) {
                return this.innerOneofDesc.containsFieldTags(tags);
            }

            return false;
        }

        public boolean isRequired() {
            return this.innerFieldDesc != null && this.innerFieldDesc.isRequired();
        }

        public boolean isList() {
            return this.innerFieldDesc != null && this.innerFieldDesc.isList();
        }

        public boolean isMap() {
            return this.innerFieldDesc != null && this.innerFieldDesc.isMap();
        }

        public int getFieldIndex() {
            if (this.innerFieldDesc == null) {
                return 0;
            }

            return this.innerFieldDesc.getIndex();
        }

        public ListStripRule getFieldListStripRule() {
            if (this.innerFieldDesc == null) {
                return getDefaultListStripRule();
            }

            return this.innerFieldDesc.getListStripRule();
        }

        public boolean isOneof() {
            return this.innerOneofDesc != null;
        }

        public int getOneofIndex() {
            if (this.innerOneofDesc == null) {
                return -1;
            }

            return this.innerOneofDesc.getIndex();
        }
    }

    private HashMap<String, DataDstChildrenNode> children = null;
    private DataDstTypeDescriptor typeDescriptor = null;
    private DataDstFieldDescriptor fieldDescriptor = null;
    private DataDstOneofDescriptor oneofDescriptor = null;
    public Object privateData = null; // 关联的Message描述信息，不同的DataDstImpl子类不一样。这里的数据和抽象数据结构的类型有关
    public IdentifyDescriptor identify = null; // 关联的Field/Excel列信息，同一个抽象数据结构类型可能对应的数据列不一样。和具体某个结构内的字段有关
    private DataDstChildrenNode referBrothers = null;
    private int listIndex = -1;

    static private HashMap<String, DataDstTypeDescriptor> defaultDescs = new HashMap<String, DataDstTypeDescriptor>();

    static public DataDstTypeDescriptor getDefaultMessageDescriptor(JAVA_TYPE type, SPECIAL_TYPE_LIMIT typeLimit) {
        String key = String.format("%s:%s", type.name(), typeLimit.name());
        DataDstTypeDescriptor ret = defaultDescs.getOrDefault(key, null);
        if (ret != null) {
            return ret;
        }

        if (type == JAVA_TYPE.MESSAGE) {
            ProgramOptions.getLoger().error(
                    "Can not get default description of message type, please report this bug to %s",
                    ProgramOptions.getReportUrl());
            return null;
        }

        ret = new DataDstTypeDescriptor(type, null, null, null, SPECIAL_MESSAGE_TYPE.NONE, typeLimit);
        defaultDescs.put(key, ret);
        return ret;
    }

    private DataDstWriterNode(Object privateData, DataDstTypeDescriptor typeDesc, int listIndex) {
        this.privateData = privateData;
        this.typeDescriptor = typeDesc;
        this.oneofDescriptor = null;
        this.listIndex = listIndex;
    }

    private DataDstWriterNode(Object privateData, DataDstOneofDescriptor oneofDesc) {
        this.privateData = privateData;
        this.typeDescriptor = null;
        this.oneofDescriptor = oneofDesc;
        this.listIndex = -1;
    }

    static public String makeChildPath(String prefix, String child_name, int list_index) {
        if (list_index >= 0)
            return prefix.isEmpty() ? String.format("%s[%d]", child_name, list_index)
                    : String.format("%s.%s[%d]", prefix, child_name, list_index);

        return makeChildPath(prefix, child_name);
    }

    static public String makeChildPath(String prefix, String child_name) {
        return prefix.isEmpty() ? child_name : String.format("%s.%s", prefix, child_name);
    }

    static public String makeNodeName(String _name, int list_index) {
        if (list_index >= 0)
            return String.format("%s[%d]", _name, list_index);

        return makeNodeName(_name);
    }

    static public String makeNodeName(String _name) {
        return _name;
    }

    public DataDstChildrenNode getReferBrothers() {
        return this.referBrothers;
    }

    public HashMap<String, DataDstChildrenNode> getChildren() {
        if (null == children)
            children = new HashMap<String, DataDstChildrenNode>();
        return children;
    }

    public DataDstTypeDescriptor getTypeDescriptor() {
        return this.typeDescriptor;
    }

    public JAVA_TYPE getType() {
        if (this.typeDescriptor != null) {
            return this.typeDescriptor.getType();
        }

        if (this.fieldDescriptor != null) {
            return this.fieldDescriptor.getType();
        }

        // enum retrun JAVA_TYPE as UNKNOWN
        return JAVA_TYPE.UNKNOWN;
    }

    public String validateTypeLimit(Object value) {
        if (this.typeDescriptor != null) {
            return this.typeDescriptor.validateTypeLimit(value);
        }

        if (this.fieldDescriptor != null) {
            return this.fieldDescriptor.validateTypeLimit(value);
        }

        return null;
    }

    public String getPackageName() {
        if (this.oneofDescriptor != null) {
            return this.oneofDescriptor.getOwnerDescriptor().getPackageName();
        }

        return this.typeDescriptor.getPackageName();
    }

    public String getMessageName() {
        if (this.oneofDescriptor != null) {
            return this.oneofDescriptor.getOwnerDescriptor().getMessageName();
        }

        return this.typeDescriptor.getMessageName();
    }

    public String getFullName() {
        if (this.oneofDescriptor != null) {
            return this.oneofDescriptor.getFullName();
        }

        return this.typeDescriptor.getFullName();
    }

    public DataDstMessageExt getMessageExtension() {
        if (this.typeDescriptor == null) {
            return null;
        }

        return this.typeDescriptor.mutableExtension();
    }

    public DataDstFieldDescriptor getFieldDescriptor() {
        return this.fieldDescriptor;
    }

    public int getFieldIndex() {
        if (this.fieldDescriptor == null) {
            return 0;
        }

        return this.fieldDescriptor.getIndex();
    }

    public String getFieldName() {
        if (this.fieldDescriptor == null) {
            return "";
        }

        return this.fieldDescriptor.getName();
    }

    public DataDstFieldExt getFieldExtension() {
        if (this.fieldDescriptor == null) {
            return null;
        }

        return this.fieldDescriptor.mutableExtension();
    }

    public ListStripRule getFieldListStripRule() {
        if (this.fieldDescriptor == null) {
            return getDefaultListStripRule();
        }

        return this.fieldDescriptor.getListStripRule();
    }

    public DataDstOneofDescriptor getOneofDescriptor() {
        return this.oneofDescriptor;
    }

    public int getOneofIndex() {
        if (this.oneofDescriptor == null) {
            return -1;
        }

        return this.oneofDescriptor.getIndex();
    }

    public String getOneofName() {
        if (this.oneofDescriptor == null) {
            return "";
        }

        return this.oneofDescriptor.getName();
    }

    public DataDstOneofExt getOneofExtension() {
        if (this.oneofDescriptor == null) {
            return null;
        }

        return this.oneofDescriptor.mutableExtension();
    }

    public int getListIndex() {
        if (this.fieldDescriptor == null) {
            return -1;
        }

        if (!this.fieldDescriptor.isList()) {
            return -1;
        }

        return this.listIndex;
    }

    public String getChildPath(String prefix, int list_index, String child_name) {
        DataDstChildrenNode res = getChildren().getOrDefault(child_name, null);
        if (null == res)
            return null;

        if (res.innerFieldDesc.isList())
            return String.format("%s[%d].%s", prefix, list_index, child_name);

        return getChildPath(prefix, child_name);
    }

    public String getChildPath(String prefix, String child_name) {
        return prefix.isEmpty() ? child_name : String.format("%s.%s", prefix, child_name);
    }

    public DataDstChildrenNode addChild(String child_name, DataDstWriterNode node, Object _raw_descriptor,
            CHILD_NODE_TYPE mode) throws ConvException {
        DataDstFieldDescriptor child_field_desc = null;
        DataDstOneofDescriptor child_oneof_desc = null;
        if (this.oneofDescriptor != null) {
            child_field_desc = this.oneofDescriptor.getFieldByName(child_name);
        } else if (this.typeDescriptor != null) {
            child_field_desc = this.typeDescriptor.fields.getOrDefault(child_name, null);
            if (null == child_field_desc) {
                child_oneof_desc = this.typeDescriptor.oneofs.getOrDefault(child_name, null);
            }
        }

        if (null == child_field_desc && null == child_oneof_desc) {
            throw new ConvException(
                    String.format("Can not find any child with name \"%s\" for %s.", child_name, getFullName()));
        }

        DataDstChildrenNode res = getChildren().getOrDefault(child_name, null);
        if (null == res) {
            res = new DataDstChildrenNode();
            getChildren().put(child_name, res);
            res.innerFieldDesc = child_field_desc;
            res.innerOneofDesc = child_oneof_desc;
            res.rawDescriptor = _raw_descriptor;
        }

        res.mode = mode;
        if (null == res.nodes) {
            res.nodes = new ArrayList<DataDstWriterNode>();
        }
        res.nodes.add(node);
        node.referBrothers = res;

        // 复制一份字段数据结构描述
        node.setFieldDescriptor(res.innerFieldDesc);
        node.setFieldDescriptor(res.innerOneofDesc);

        return res;
    }

    public void setFieldDescriptor(DataDstFieldDescriptor fd) throws ConvException {
        if (fd == null) {
            this.fieldDescriptor = fd;
            return;
        }

        if (fd.getTypeDescriptor() != this.typeDescriptor) {
            throw new ConvException(String.format("writer's type descriptor and field's type descriptor not match"));
        }

        this.fieldDescriptor = fd;
    }

    public void setFieldDescriptor(DataDstOneofDescriptor fd) throws ConvException {
        this.oneofDescriptor = fd;
    }

    /**
     * 创建节点
     *
     * @param privateData 原始协议描述器
     * @param typeDesc    内部描述类型
     * @return 创建的节点
     */
    static public DataDstWriterNode create(Object privateData, DataDstTypeDescriptor typeDesc, int listIndex) {
        return new DataDstWriterNode(privateData, typeDesc, listIndex);
    }

    /**
     * 创建节点
     *
     * @param privateData 原始协议描述器
     * @param typeDesc    内部描述类型
     * @return 创建的节点
     */
    static public DataDstWriterNode create(Object privateData, DataDstOneofDescriptor oneofDesc) {
        return new DataDstWriterNode(privateData, oneofDesc);
    }
}
