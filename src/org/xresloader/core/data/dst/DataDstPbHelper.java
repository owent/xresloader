package org.xresloader.core.data.dst;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.DynamicMessage;
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

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.EnumValueDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("number", msg_desc.getNumber());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.EnumDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        // value
        HashMap<String, Object> child_message = null;
        for (Descriptors.EnumValueDescriptor sub_desc : msg_desc.getValues()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.OneofDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.FieldDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("type_name", msg_desc.getType().name());
            msg_root.put("number", msg_desc.getNumber());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.Descriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        // nested_type
        HashMap<String, Object> child_message = null;
        for (Descriptors.Descriptor sub_desc : msg_desc.getNestedTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.MethodDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        if (has_data) {
            msg_root.put("name", msg_desc.getName());
            msg_root.put("input_type", msg_desc.getInputType().getFullName());
            msg_root.put("output_type", msg_desc.getOutputType().getFullName());
            return msg_root;
        }

        return null;
    }

    static public HashMap<String, Object> dumpOptionsIntoHashMap(Descriptors.ServiceDescriptor msg_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = msg_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        // method
        HashMap<String, Object> child_message = null;
        for (Descriptors.MethodDescriptor sub_desc : msg_desc.getMethods()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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

    static public void dumpOptionsIntoHashMap(LinkedList<Object> parent, Descriptors.FileDescriptor file_desc) {
        HashMap<String, Object> msg_root = new HashMap<String, Object>();
        boolean has_data = false;

        Map<Descriptors.FieldDescriptor, Object> options_data = file_desc.getOptions().getAllFields();
        if (!options_data.isEmpty()) {
            HashMap<String, Object> options_root = new HashMap<String, Object>();
            has_data = true;
            msg_root.put("options", options_root);

            for (Map.Entry<Descriptors.FieldDescriptor, Object> data : options_data.entrySet()) {
                options_root.put(data.getKey().getName(), data.getValue());
            }
        }

        HashMap<String, Object> child_message = null;
        for (Descriptors.Descriptor sub_desc : file_desc.getMessageTypes()) {
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
            HashMap<String, Object> subset = dumpOptionsIntoHashMap(sub_desc);
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
}
