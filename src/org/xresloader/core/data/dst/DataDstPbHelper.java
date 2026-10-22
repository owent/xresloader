package org.xresloader.core.data.dst;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry.ExtensionInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import org.xresloader.core.ProgramOptions;

/**
 * Created by owent on 2019/11/08.
 */
public class DataDstPbHelper {
    // ==================== dump const ====================
    @SuppressWarnings("unchecked")
    public static void dumpConstIntoHashMap(String packageName, HashMap<String, Object> parent,
            Descriptors.EnumDescriptor enumDesc) {
        String enumSeg = enumDesc.getName();
        HashMap<String, Object> enumRoot;
        if (parent.containsKey(enumSeg)) {
            Object node = parent.get(enumSeg);
            if (node instanceof HashMap) {
                enumRoot = (HashMap<String, Object>) node;
            } else {
                ProgramOptions.getLogger().error("Enum name %s.%s conflict.", packageName, enumSeg);
                return;
            }
        } else {
            enumRoot = new HashMap<String, Object>();
            parent.put(enumSeg, enumRoot);
        }

        // 写出所有常量值
        for (Descriptors.EnumValueDescriptor enumVal : enumDesc.getValues()) {
            enumRoot.put(enumVal.getName(), enumVal.getNumber());
        }
    }

    @SuppressWarnings("unchecked")
    public static void dumpConstIntoHashMap(String packageName, HashMap<String, Object> parent,
            Descriptors.OneofDescriptor oneofDesc) {
        String oneofSeg = oneofDesc.getName();
        HashMap<String, Object> oneofRoot;
        if (parent.containsKey(oneofSeg)) {
            Object node = parent.get(oneofSeg);
            if (node instanceof HashMap) {
                oneofRoot = (HashMap<String, Object>) node;
            } else {
                ProgramOptions.getLogger().error("Oneof name %s.%s conflict.", packageName, oneofSeg);
                return;
            }
        } else {
            oneofRoot = new HashMap<String, Object>();
            parent.put(oneofSeg, oneofRoot);
        }

        // 写出所有常量值
        for (Descriptors.FieldDescriptor oneofOption : oneofDesc.getFields()) {
            String fieldName = oneofOption.getJsonName();
            if (fieldName.length() > 0) {
                fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            }
            fieldName = String.format("k%s", fieldName);
            oneofRoot.put(fieldName, oneofOption.getNumber());
        }
    }

    @SuppressWarnings("unchecked")
    public static void dumpConstIntoHashMap(String packageName, HashMap<String, Object> parent,
            Descriptors.Descriptor msgDesc) {
        String msgSeg = msgDesc.getName();
        HashMap<String, Object> msgRoot = null;
        Object msgNode = parent.getOrDefault(msgSeg, null);
        String msgFullName = String.format("%s.%s", packageName, msgSeg);
        if (msgNode != null) {
            if (msgNode instanceof HashMap) {
                msgRoot = (HashMap<String, Object>) msgNode;
            } else {
                ProgramOptions.getLogger().error("Message name %s conflict.", msgFullName);
                return;
            }
        }

        // enum in message.
        for (Descriptors.EnumDescriptor enumDesc : msgDesc.getEnumTypes()) {
            if (null == msgRoot) {
                msgRoot = new HashMap<String, Object>();
                parent.put(msgSeg, msgRoot);
            }

            dumpConstIntoHashMap(msgFullName, msgRoot, enumDesc);
        }

        // if has oneof in message, dump all fields's number.
        for (Descriptors.OneofDescriptor oneofDesc : msgDesc.getOneofs()) {
            if (oneofDesc.getFieldCount() <= 0) {
                continue;
            }
            if (null == msgRoot) {
                msgRoot = new HashMap<String, Object>();
                parent.put(msgSeg, msgRoot);
            }

            dumpConstIntoHashMap(msgFullName, msgRoot, oneofDesc);
        }

        // nested message
        for (Descriptors.Descriptor subMsgDesc : msgDesc.getNestedTypes()) {
            if (null == msgRoot) {
                msgRoot = new HashMap<String, Object>();
                parent.put(msgSeg, msgRoot);
            }

            dumpConstIntoHashMap(msgFullName, msgRoot, subMsgDesc);
        }
    }

    // ==================== dump options ====================

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.EnumValueDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            msgRoot.put("number", msgDesc.getNumber());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.EnumDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        // value
        HashMap<String, Object> childMessage = null;
        for (Descriptors.EnumValueDescriptor subDesc : msgDesc.getValues()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("value", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.OneofDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.FieldDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            msgRoot.put("type_name", msgDesc.getType().name());
            msgRoot.put("number", msgDesc.getNumber());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.Descriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        // nested_type
        HashMap<String, Object> childMessage = null;
        for (Descriptors.Descriptor subDesc : msgDesc.getNestedTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("enum_type", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // enum_type
        childMessage = null;
        for (Descriptors.EnumDescriptor subDesc : msgDesc.getEnumTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("nested_type", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // oneof_decl
        childMessage = null;
        for (Descriptors.OneofDescriptor subDesc : msgDesc.getOneofs()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("oneof_decl", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // field
        childMessage = null;
        for (Descriptors.FieldDescriptor subDesc : msgDesc.getFields()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("field", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.MethodDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            msgRoot.put("input_type", msgDesc.getInputType().getFullName());
            msgRoot.put("output_type", msgDesc.getOutputType().getFullName());
            return msgRoot;
        }

        return null;
    }

    public static HashMap<String, Object> dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType,
            Descriptors.ServiceDescriptor msgDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(msgDesc.getOptions(),
                msgDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        // method
        HashMap<String, Object> childMessage = null;
        for (Descriptors.MethodDescriptor subDesc : msgDesc.getMethods()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("method", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        if (hasData) {
            msgRoot.put("name", msgDesc.getName());
            return msgRoot;
        }

        return null;
    }

    public static void dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType dumpType, LinkedList<Object> parent,
            Descriptors.FileDescriptor fileDesc, com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> msgRoot = new HashMap<String, Object>();
        boolean hasData = dumpType == ProgramOptions.ProtoDumpType.DESCRIPTOR;

        HashMap<String, Object> optionsData = dumpMessageExtensions(fileDesc.getOptions(),
                fileDesc.getOptions().getAllFields(), customExtensions);
        if (optionsData != null && !optionsData.isEmpty()) {
            hasData = true;
            msgRoot.put("options", optionsData);
        }

        HashMap<String, Object> childMessage = null;
        for (Descriptors.Descriptor subDesc : fileDesc.getMessageTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("message_type", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // enum_type
        childMessage = null;
        for (Descriptors.EnumDescriptor subDesc : fileDesc.getEnumTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("enum_type", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // service
        childMessage = null;
        for (Descriptors.ServiceDescriptor subDesc : fileDesc.getServices()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("extension", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        // extensions
        childMessage = null;
        for (Descriptors.FieldDescriptor subDesc : fileDesc.getExtensions()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(dumpType, subDesc, customExtensions);
            if (subset == null) {
                continue;
            }

            if (childMessage == null) {
                childMessage = new HashMap<String, Object>();
                msgRoot.put("extension", childMessage);
            }

            hasData = true;
            childMessage.put(subDesc.getName(), subset);
        }

        if (hasData) {
            msgRoot.put("package", fileDesc.getPackage());
            msgRoot.put("path", fileDesc.getName());
            msgRoot.put("name", new File(fileDesc.getName()).getName());

            parent.add(msgRoot);
        }
    }

    private static Object convertMessageFieldIntoObject(Descriptors.FieldDescriptor desc, Object value,
            boolean expandEnum) {
        if (desc == null || value == null) {
            return null;
        }
        switch (desc.getJavaType()) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case BYTE_STRING: {
                return value;
            }
            case ENUM: {
                if (value instanceof List<?>) {
                    if (expandEnum) {
                        ArrayList<HashMap<String, Object>> ret = new ArrayList<HashMap<String, Object>>();
                        ret.ensureCapacity(((List<?>) value).size());

                        for (Object enumValueDesc : (List<?>) value) {
                            if (enumValueDesc instanceof EnumValueDescriptor) {
                                HashMap<String, Object> res = new HashMap<String, Object>();
                                res.put("name", ((EnumValueDescriptor) enumValueDesc).getName());
                                res.put("number", ((EnumValueDescriptor) enumValueDesc).getNumber());
                                ret.add(res);
                            }
                        }
                        return ret;
                    } else {
                        ArrayList<Integer> ret = new ArrayList<Integer>();
                        ret.ensureCapacity(((List<?>) value).size());

                        for (Object enumValueDesc : (List<?>) value) {
                            if (enumValueDesc instanceof EnumValueDescriptor) {
                                ret.add(((EnumValueDescriptor) enumValueDesc).getNumber());
                            }
                        }
                        return ret;
                    }
                } else if (value instanceof EnumValueDescriptor) {
                    if (expandEnum) {
                        HashMap<String, Object> ret = new HashMap<String, Object>();
                        ret.put("name", ((EnumValueDescriptor) value).getName());
                        ret.put("number", ((EnumValueDescriptor) value).getNumber());
                        return ret;
                    } else {
                        return ((EnumValueDescriptor) value).getNumber();
                    }
                }
                return null;
            }
            case MESSAGE: {
                if (value instanceof List<?>) {
                    ArrayList<HashMap<String, Object>> ret = new ArrayList<HashMap<String, Object>>();
                    ret.ensureCapacity(((List<?>) value).size());

                    for (Object submsg : (List<?>) value) {
                        if (submsg instanceof Message) {
                            ret.add(convertMessageIntoInnerMap((Message) submsg, expandEnum));
                        }
                    }
                    return ret;
                } else if (value instanceof Message) {
                    return convertMessageIntoInnerMap((Message) value, expandEnum);
                } else {
                    return null;
                }
            }
            default:
                return null;
        }
    }

    private static HashMap<String, Object> convertMessageIntoInnerMap(Message msg, boolean expandEnum) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        if (msg == null) {
            return ret;
        }

        for (Map.Entry<Descriptors.FieldDescriptor, Object> kv : msg.getAllFields().entrySet()) {
            ret.put(kv.getKey().getName(), convertMessageFieldIntoObject(kv.getKey(), kv.getValue(), expandEnum));
        }

        return ret;
    }

    private static Object pickMessageField(com.google.protobuf.UnknownFieldSet.Field fieldData,
            FieldDescriptor fieldDesc, Message defaultInstance, boolean expandEnum)
            throws InvalidProtocolBufferException, UnsupportedEncodingException {
        if (null == fieldDesc || null == fieldData) {
            return null;
        }

        switch (fieldDesc.getType()) {
            case DOUBLE: {
                List<Long> rawData = fieldData.getFixed64List();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<Double> ret = new ArrayList<Double>();
                    ret.ensureCapacity(rawData.size());
                    for (Long v : rawData) {
                        ret.add(Double.longBitsToDouble(v));
                    }
                    return ret;
                } else {
                    return Double.longBitsToDouble(rawData.get(0));
                }
            }
            case FLOAT: {
                List<Integer> rawData = fieldData.getFixed32List();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<Float> ret = new ArrayList<Float>();
                    ret.ensureCapacity(rawData.size());
                    for (Integer v : rawData) {
                        ret.add(Float.intBitsToFloat(v));
                    }
                    return ret;
                } else {
                    return Float.intBitsToFloat(rawData.get(0));
                }
            }
            case INT64:
            case UINT64:
            case INT32:
            case UINT32: {
                List<Long> rawData = fieldData.getVarintList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    return rawData;
                } else {
                    return rawData.get(0);
                }
            }

            case SINT64: {
                List<Long> rawData = fieldData.getVarintList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<Long> ret = new ArrayList<Long>();
                    ret.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        ret.add((rawData.get(i) << 63) ^ (rawData.get(i) >> 63));
                    }
                    return rawData;
                } else {
                    return (rawData.get(0) << 63) ^ (rawData.get(0) >> 63);
                }
            }
            case SINT32: {
                List<Long> rawData = fieldData.getVarintList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<Integer> ret = new ArrayList<Integer>();
                    ret.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        ret.add((rawData.get(i).intValue() << 31) ^ (rawData.get(i).intValue() >> 31));
                    }
                    return rawData;
                } else {
                    return (rawData.get(0).intValue() << 31) ^ (rawData.get(0).intValue() >> 31);
                }
            }

            case ENUM: {
                List<Long> rawData = fieldData.getVarintList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<EnumValueDescriptor> res = new ArrayList<EnumValueDescriptor>();
                    res.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        EnumValueDescriptor enumValueDesc = fieldDesc.getEnumType()
                                .findValueByNumber(rawData.get(0).intValue());
                        if (null == enumValueDesc) {
                            continue;
                        }
                        res.add(enumValueDesc);
                    }
                    return convertMessageFieldIntoObject(fieldDesc, res, expandEnum);
                } else {
                    EnumValueDescriptor enumValueDesc = fieldDesc.getEnumType()
                            .findValueByNumber(rawData.get(0).intValue());
                    if (null == enumValueDesc) {
                        return null;
                    }

                    return convertMessageFieldIntoObject(fieldDesc, enumValueDesc, expandEnum);
                }
            }
            case BOOL: {
                List<Long> rawData = fieldData.getVarintList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<Boolean> ret = new ArrayList<Boolean>();
                    ret.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        ret.add(rawData.get(i) != 0);
                    }
                    return ret;
                } else {
                    return rawData.get(0) != 0;
                }
            }
            case SFIXED64:
            case FIXED64: {
                List<Long> rawData = fieldData.getFixed64List();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    return rawData;
                } else {
                    return rawData.get(0);
                }
            }
            case SFIXED32:
            case FIXED32: {
                List<Integer> rawData = fieldData.getFixed32List();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    return rawData;
                } else {
                    return rawData.get(0);
                }
            }
            case STRING: {
                List<ByteString> rawData = fieldData.getLengthDelimitedList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<String> ret = new ArrayList<String>();
                    ret.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        ret.add(new String(rawData.get(i).toByteArray(), "UTF-8"));
                    }
                    return ret;
                } else {
                    return new String(rawData.get(0).toByteArray(), "UTF-8");
                }
            }
            case GROUP: // ignore & skip
                return null;
            case MESSAGE: {
                List<ByteString> rawData = fieldData.getLengthDelimitedList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    ArrayList<HashMap<String, Object>> ret = new ArrayList<HashMap<String, Object>>();
                    ret.ensureCapacity(rawData.size());
                    for (int i = 0; i < rawData.size(); ++i) {
                        Message.Builder builder = defaultInstance.newBuilderForType();
                        ret.add(convertMessageIntoInnerMap(builder.mergeFrom(rawData.get(i)).build(), expandEnum));
                    }
                    return ret;
                } else {
                    Message.Builder builder = defaultInstance.newBuilderForType();
                    return convertMessageIntoInnerMap(builder.mergeFrom(rawData.get(0)).build(), expandEnum);
                }
            }
            case BYTES: {
                List<ByteString> rawData = fieldData.getLengthDelimitedList();
                if (rawData == null || rawData.isEmpty()) {
                    return null;
                }

                if (fieldDesc.isRepeated()) {
                    return rawData;
                } else {
                    return rawData.get(0);
                }
            }
            default:
                return null;
        }
    }

    private static <T extends com.google.protobuf.GeneratedMessage.ExtendableMessage<T>> HashMap<String, Object> dumpMessageExtensions(
            T msg, Map<Descriptors.FieldDescriptor, Object> options,
            com.google.protobuf.ExtensionRegistry customExtensions) {
        HashMap<String, Object> ret = null;
        HashMap<String, String> conflictChecker = new HashMap<String, String>();
        String empty = "";
        if (null == customExtensions) {
            return ret;
        }

        if (options != null && !options.isEmpty()) {
            ret = new HashMap<String, Object>();
            for (Map.Entry<Descriptors.FieldDescriptor, Object> kv : options.entrySet()) {
                ret.put(kv.getKey().getName(), convertMessageFieldIntoObject(kv.getKey(), kv.getValue(), true));
                conflictChecker.put(kv.getKey().getName(), empty);
            }
        }

        Map<Integer, com.google.protobuf.UnknownFieldSet.Field> allUnknownFields = msg.getUnknownFields().asMap();
        if (allUnknownFields.isEmpty()) {
            return ret;
        }

        Set<ExtensionInfo> allExtTypes = customExtensions
                .getAllImmutableExtensionsByExtendedType(msg.getDescriptorForType().getFullName());

        if (allExtTypes.isEmpty()) {
            return ret;
        }

        for (ExtensionInfo extType : allExtTypes) {
            com.google.protobuf.UnknownFieldSet.Field fieldData = allUnknownFields
                    .getOrDefault(extType.descriptor.getNumber(), null);
            if (fieldData == null) {
                continue;
            }

            String shortName = extType.descriptor.getName();
            if (ret != null && ret.containsKey(shortName)) {
                continue;
            }

            try {
                Object val;
                val = pickMessageField(fieldData, extType.descriptor, extType.defaultInstance, true);
                if (null != val) {
                    if (ret == null) {
                        ret = new HashMap<String, Object>();
                    }

                    // Switch to full name if found conflict name
                    String oldFullName = conflictChecker.getOrDefault(shortName, null);
                    if (oldFullName != null) {
                        if (!oldFullName.isEmpty()) {
                            Object oldValue = ret.getOrDefault(shortName, null);
                            if (oldValue != null) {
                                ret.put(oldFullName, oldValue);
                                ret.remove(shortName);
                            }

                            conflictChecker.replace(shortName, empty);
                        }

                        ret.put(extType.descriptor.getFullName(), val);
                    } else {
                        conflictChecker.put(shortName, extType.descriptor.getFullName());
                        ret.put(extType.descriptor.getName(), val);
                    }
                }
            } catch (InvalidProtocolBufferException | UnsupportedEncodingException e) {
                // Ignore error
            }
        }

        return ret;
    }
}
