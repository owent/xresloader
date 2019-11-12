package org.xresloader.core.data.dst;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.ExtensionRegistry.ExtensionInfo;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.UninitializedMessageException;
import org.apache.commons.codec.binary.Hex;
import org.xresloader.Xresloader;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstChildrenNode;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstMessageDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyIntRange;
import org.xresloader.core.data.vfy.DataVerifyPbEnum;
import org.xresloader.core.data.vfy.DataVerifyPbMsg;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.pb.PbHeaderV3;
import org.xresloader.ue.XresloaderUe;

/**
 * Created by owent on 2019/11/08.
 */
public class DataDstPbHelper {
    // ==================== dump const ====================
    @SuppressWarnings("unchecked")
    static public void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent,
            Descriptors.EnumDescriptor enum_desc) {
        String enum_seg = enum_desc.getName();
        HashMap<String, Object> enum_root;
        if (parent.containsKey(enum_seg)) {
            Object node = parent.get(enum_seg);
            if (node instanceof HashMap) {
                enum_root = (HashMap<String, Object>) node;
            } else {
                ProgramOptions.getLoger().error("enum name %s.%s conflict.", package_name, enum_seg);
                return;
            }
        } else {
            enum_root = new HashMap<String, Object>();
            parent.put(enum_seg, enum_root);
        }

        // 写出所有常量值
        for (Descriptors.EnumValueDescriptor enum_val : enum_desc.getValues()) {
            enum_root.put(enum_val.getName(), enum_val.getNumber());
        }
    }

    @SuppressWarnings("unchecked")
    static public void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent,
            Descriptors.OneofDescriptor oneof_desc) {
        String oneof_seg = oneof_desc.getName();
        HashMap<String, Object> oneof_root;
        if (parent.containsKey(oneof_seg)) {
            Object node = parent.get(oneof_seg);
            if (node instanceof HashMap) {
                oneof_root = (HashMap<String, Object>) node;
            } else {
                ProgramOptions.getLoger().error("oneof name %s.%s conflict.", package_name, oneof_seg);
                return;
            }
        } else {
            oneof_root = new HashMap<String, Object>();
            parent.put(oneof_seg, oneof_root);
        }

        // 写出所有常量值
        for (Descriptors.FieldDescriptor oneof_option : oneof_desc.getFields()) {
            String field_name = oneof_option.getJsonName();
            if (field_name.length() > 0) {
                field_name = Character.toUpperCase(field_name.charAt(0)) + field_name.substring(1);
            }
            field_name = String.format("k%s", field_name);
            oneof_root.put(field_name, oneof_option.getNumber());
        }
    }

    @SuppressWarnings("unchecked")
    static public void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent,
            Descriptors.Descriptor msg_desc) {
        String msg_seg = msg_desc.getName();
        HashMap<String, Object> msg_root = null;
        Object msg_node = parent.getOrDefault(msg_seg, null);
        String msg_full_name = String.format("%s.%s", package_name, msg_seg);
        if (msg_node != null) {
            if (msg_node instanceof HashMap) {
                msg_root = (HashMap<String, Object>) msg_node;
            } else {
                ProgramOptions.getLoger().error("message name %s conflict.", msg_full_name);
                return;
            }
        }

        // enum in message.
        for (Descriptors.EnumDescriptor enum_desc : msg_desc.getEnumTypes()) {
            if (null == msg_root) {
                msg_root = new HashMap<String, Object>();
                parent.put(msg_seg, msg_root);
            }

            dumpConstIntoHashMap(msg_full_name, msg_root, enum_desc);
        }

        // if has oneof in message, dump all fields's number.
        for (Descriptors.OneofDescriptor oneof_desc : msg_desc.getOneofs()) {
            if (oneof_desc.getFieldCount() <= 0) {
                continue;
            }
            if (null == msg_root) {
                msg_root = new HashMap<String, Object>();
                parent.put(msg_seg, msg_root);
            }

            dumpConstIntoHashMap(msg_full_name, msg_root, oneof_desc);
        }

        // nested message
        for (Descriptors.Descriptor sub_msg_desc : msg_desc.getNestedTypes()) {
            if (null == msg_root) {
                msg_root = new HashMap<String, Object>();
                parent.put(msg_seg, msg_root);
            }

            dumpConstIntoHashMap(msg_full_name, msg_root, sub_msg_desc);
        }
    }

    // ==================== dump options ====================

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.EnumValueDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("number", msg_desc.getNumber());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.EnumDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        // value
        HashMap<String, Object> child_message = null;
        for (Descriptors.EnumValueDescriptor sub_desc : msg_desc.getValues()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("value", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.OneofDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.FieldDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("type_name", msg_desc.getType().name());
            msg_root.put("number", msg_desc.getNumber());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.Descriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        // nested_type
        HashMap<String, Object> child_message = null;
        for (Descriptors.Descriptor sub_desc : msg_desc.getNestedTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("enum_type", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // enum_type
        child_message = null;
        for (Descriptors.EnumDescriptor sub_desc : msg_desc.getEnumTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("nested_type", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // oneof_decl
        child_message = null;
        for (Descriptors.OneofDescriptor sub_desc : msg_desc.getOneofs()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("oneof_decl", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // field
        child_message = null;
        for (Descriptors.FieldDescriptor sub_desc : msg_desc.getFields()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("field", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.MethodDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("input_type", msg_desc.getInputType().getFullName());
            msg_root.put("output_type", msg_desc.getOutputType().getFullName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.ServiceDescriptor msg_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(msg_desc.getOptions(),
                msg_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        // method
        HashMap<String, Object> child_message = null;
        for (Descriptors.MethodDescriptor sub_desc : msg_desc.getMethods()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("method", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            return msg_root;
        }

        return null;
    }

    static public void dumpOptionsIntoHashMap(LinkedList<Object> parent, Descriptors.FileDescriptor file_desc,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        HashMap<String, Object> options_data = dumpMessageExtensions(file_desc.getOptions(),
                file_desc.getOptions().getAllFields(), custom_extensions);
        if (options_data != null && !options_data.isEmpty()) {
            has_data = true;
            msg_root.put("options", options_data);
        }

        HashMap<String, Object> child_message = null;
        for (Descriptors.Descriptor sub_desc : file_desc.getMessageTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("message_type", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // enum_type
        child_message = null;
        for (Descriptors.EnumDescriptor sub_desc : file_desc.getEnumTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("enum_type", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // service
        child_message = null;
        for (Descriptors.ServiceDescriptor sub_desc : file_desc.getServices()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("extension", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        // extensions
        child_message = null;
        for (Descriptors.FieldDescriptor sub_desc : file_desc.getExtensions()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc, custom_extensions);
            if (subset == null) {
                continue;
            }

            if (child_message == null) {
                child_message = new HashMap<String, Object>();
                msg_root.put("extension", child_message);
            }

            has_data = true;
            child_message.put(sub_desc.getName(), subset);
        }

        if (has_data) {
            msg_root.put("package", file_desc.getPackage());
            msg_root.put("path", file_desc.getName());
            msg_root.put("name", new File(file_desc.getName()).getName());

            parent.add(msg_root);
        }
    }

    private static HashMap<String, Object> convertMessageIntoInnerMap(Message msg) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        if (msg == null) {
            return ret;
        }

        for (Map.Entry<Descriptors.FieldDescriptor, Object> kv : msg.getAllFields().entrySet()) {
            switch (kv.getKey().getJavaType()) {
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case BYTE_STRING: {
                ret.put(kv.getKey().getName(), kv.getValue());
                break;
            }
            case ENUM: {
                if (kv.getValue() instanceof List<?>) {
                    ArrayList<Integer> res = new ArrayList<Integer>();
                    res.ensureCapacity(((List<?>) kv.getValue()).size());

                    for (Object enum_value_desc : (List<?>) kv.getValue()) {
                        if (enum_value_desc instanceof EnumValueDescriptor) {
                            res.add(((EnumValueDescriptor) enum_value_desc).getNumber());
                        }
                    }
                    ret.put(kv.getKey().getName(), res);
                } else if (kv.getValue() instanceof EnumValueDescriptor) {
                    ret.put(kv.getKey().getName(), ((EnumValueDescriptor) kv.getValue()).getNumber());
                }
                break;
            }
            case MESSAGE: {
                if (kv.getValue() instanceof List<?>) {
                    ArrayList<HashMap<String, Object>> res = new ArrayList<HashMap<String, Object>>();
                    res.ensureCapacity(((List<?>) kv.getValue()).size());

                    for (Object submsg : (List<?>) kv.getValue()) {
                        if (submsg instanceof Message) {
                            res.add(convertMessageIntoInnerMap((Message) submsg));
                        }
                    }
                    ret.put(kv.getKey().getName(), res);
                } else if (kv.getValue() instanceof Message) {
                    ret.put(kv.getKey().getName(), convertMessageIntoInnerMap((Message) kv.getValue()));
                }
                break;
            }
            default:
                break;
            }
        }

        return ret;
    }

    private static Object pickMessageField(com.google.protobuf.UnknownFieldSet.Field field_data,
            FieldDescriptor field_desc, Message defaultInstance)
            throws InvalidProtocolBufferException, UnsupportedEncodingException {
        if (null == field_desc || null == field_data) {
            return null;
        }

        switch (field_desc.getType()) {
        case DOUBLE: {
            List<Long> raw_data = field_data.getFixed64List();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Double> ret = new ArrayList<Double>();
                ret.ensureCapacity(raw_data.size());
                for (Long v : raw_data) {
                    ret.add(Double.longBitsToDouble(v));
                }
                return ret;
            } else {
                return Double.longBitsToDouble(raw_data.get(0));
            }
        }
        case FLOAT: {
            List<Integer> raw_data = field_data.getFixed32List();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Float> ret = new ArrayList<Float>();
                ret.ensureCapacity(raw_data.size());
                for (Integer v : raw_data) {
                    ret.add(Float.intBitsToFloat(v));
                }
                return ret;
            } else {
                return Float.intBitsToFloat(raw_data.get(0));
            }
        }
        case INT64:
        case UINT64:
        case INT32:
        case UINT32: {
            List<Long> raw_data = field_data.getVarintList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                return raw_data;
            } else {
                return raw_data.get(0);
            }
        }

        case SINT64: {
            List<Long> raw_data = field_data.getVarintList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Long> ret = new ArrayList<Long>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    ret.add((raw_data.get(i) << 63) ^ (raw_data.get(i) >> 63));
                }
                return raw_data;
            } else {
                return (raw_data.get(0) << 63) ^ (raw_data.get(0) >> 63);
            }
        }
        case SINT32: {
            List<Long> raw_data = field_data.getVarintList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Integer> ret = new ArrayList<Integer>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    ret.add((raw_data.get(i).intValue() << 31) ^ (raw_data.get(i).intValue() >> 31));
                }
                return raw_data;
            } else {
                return (raw_data.get(0).intValue() << 31) ^ (raw_data.get(0).intValue() >> 31);
            }
        }

        case ENUM: {
            List<Long> raw_data = field_data.getVarintList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Integer> ret = new ArrayList<Integer>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    EnumValueDescriptor enum_value_desc = field_desc.getEnumType()
                            .findValueByNumber(raw_data.get(0).intValue());
                    if (null == enum_value_desc) {
                        continue;
                    }
                    ret.add(enum_value_desc.getNumber());
                }
                return ret;
            } else {
                EnumValueDescriptor enum_value_desc = field_desc.getEnumType()
                        .findValueByNumber(raw_data.get(0).intValue());
                if (null == enum_value_desc) {
                    return null;
                }
                return enum_value_desc.getNumber();
            }
        }
        case BOOL: {
            List<Long> raw_data = field_data.getVarintList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<Boolean> ret = new ArrayList<Boolean>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    ret.add(raw_data.get(i) != 0);
                }
                return ret;
            } else {
                return raw_data.get(0) != 0;
            }
        }
        case SFIXED64:
        case FIXED64: {
            List<Long> raw_data = field_data.getFixed64List();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                return raw_data;
            } else {
                return raw_data.get(0);
            }
        }
        case SFIXED32:
        case FIXED32: {
            List<Integer> raw_data = field_data.getFixed32List();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                return raw_data;
            } else {
                return raw_data.get(0);
            }
        }
        case STRING: {
            List<ByteString> raw_data = field_data.getLengthDelimitedList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<String> ret = new ArrayList<String>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    ret.add(new String(raw_data.get(i).toByteArray(), "UTF-8"));
                }
                return ret;
            } else {
                return new String(raw_data.get(0).toByteArray(), "UTF-8");
            }
        }
        case GROUP: // ignore & skip
            return null;
        case MESSAGE: {
            List<ByteString> raw_data = field_data.getLengthDelimitedList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                ArrayList<HashMap<String, Object>> ret = new ArrayList<HashMap<String, Object>>();
                ret.ensureCapacity(raw_data.size());
                for (int i = 0; i < raw_data.size(); ++i) {
                    Message.Builder builder = defaultInstance.newBuilderForType();
                    ret.add(convertMessageIntoInnerMap(builder.mergeFrom(raw_data.get(i)).build()));
                }
                return ret;
            } else {
                Message.Builder builder = defaultInstance.newBuilderForType();
                return convertMessageIntoInnerMap(builder.mergeFrom(raw_data.get(0)).build());
            }
        }
        case BYTES: {
            List<ByteString> raw_data = field_data.getLengthDelimitedList();
            if (raw_data == null || raw_data.isEmpty()) {
                return null;
            }

            if (field_desc.isRepeated()) {
                return raw_data;
            } else {
                return raw_data.get(0);
            }
        }
        default:
            return null;
        }
    }

    private static <T extends com.google.protobuf.GeneratedMessageV3.ExtendableMessage<T>> HashMap<String, Object> dumpMessageExtensions(
            T msg, Map<Descriptors.FieldDescriptor, Object> options,
            com.google.protobuf.ExtensionRegistry custom_extensions) {
        HashMap<String, Object> ret = null;
        if (null == custom_extensions) {
            return ret;
        }

        if (options != null && !options.isEmpty()) {
            ret = new HashMap<String, Object>();
            for (Map.Entry<Descriptors.FieldDescriptor, Object> kv : options.entrySet()) {
                ret.put(kv.getKey().getName(), kv.getValue());
            }
        }

        Map<Integer, com.google.protobuf.UnknownFieldSet.Field> all_unknown_fields = msg.getUnknownFields().asMap();
        if (all_unknown_fields.isEmpty()) {
            return ret;
        }

        Set<ExtensionInfo> all_ext_types = custom_extensions
                .getAllImmutableExtensionsByExtendedType(msg.getDescriptorForType().getFullName());

        if (all_ext_types.isEmpty()) {
            return ret;
        }

        for (ExtensionInfo ext_type : all_ext_types) {
            com.google.protobuf.UnknownFieldSet.Field field_data = all_unknown_fields
                    .getOrDefault(ext_type.descriptor.getNumber(), null);
            if (field_data == null) {
                continue;
            }

            if (ret != null && ret.containsKey(ext_type.descriptor.getName())) {
                continue;
            }

            try {
                Object val;
                val = pickMessageField(field_data, ext_type.descriptor, ext_type.defaultInstance);
                if (null != val) {
                    if (ret == null) {
                        ret = new HashMap<String, Object>();
                    }
                    ret.put(ext_type.descriptor.getName(), val);
                }
            } catch (InvalidProtocolBufferException | UnsupportedEncodingException e) {
                // Ignore error
            }
        }

        return ret;
    }
}
