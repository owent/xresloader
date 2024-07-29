package org.xresloader.core.data.dst;

import com.google.protobuf.*;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import org.apache.commons.codec.binary.Hex;
import org.xresloader.Xresloader;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstChildrenNode;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.et.DataETProcessor;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.*;
import org.xresloader.core.data.vfy.DataVerifyImpl.ValidatorTokens;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.pb.PbHeaderV3;
import org.xresloader.ue.XresloaderUe;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;

/**
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    static private class PbAliasNode<T> {
        public T element = null;
        LinkedList<String> names = null;
    };

    static private class ParseResult {
        public DynamicMessage value = null;
        public String origin = null;
    }

    /***
     * protobuf 的描述信息生成是按文件的，所以要缓存并先生成依赖，再生成需要的文件描述数据
     */
    static private class PbInfoSet {
        // =========================== pb 文件描述集 ==========================
        /*** 已加载文件集合，用于文件读取去重(.pb文件) ***/
        public HashSet<String> fileNames = new HashSet<String>();
        /*** 描述信息-已加载文件描述集，用于文件描述去重(.proto文件) ***/
        public HashMap<String, DescriptorProtos.FileDescriptorProto> files = new HashMap<String, DescriptorProtos.FileDescriptorProto>();
        /*** 描述信息-消息描述集合 ***/
        public HashMap<String, PbAliasNode<DescriptorProtos.DescriptorProto>> messages = new HashMap<String, PbAliasNode<DescriptorProtos.DescriptorProto>>();
        /*** 描述信息-枚举描述集合 ***/
        public HashMap<String, PbAliasNode<DescriptorProtos.EnumDescriptorProto>> enums = new HashMap<String, PbAliasNode<DescriptorProtos.EnumDescriptorProto>>();
        /*** 描述信息-oneof描述集合 ***/
        public HashMap<String, PbAliasNode<DescriptorProtos.OneofDescriptorProto>> oneofs = new HashMap<String, PbAliasNode<DescriptorProtos.OneofDescriptorProto>>();

        // ========================== 配置描述集 ==========================
        /*** 类型信息-文件描述器集合 ***/
        public HashMap<String, Descriptors.FileDescriptor> file_descs = new HashMap<String, Descriptors.FileDescriptor>();
        public HashSet<String> file_descs_failed = new HashSet<String>();
        /*** 类型信息-Message描述器集合 ***/
        public HashMap<String, PbAliasNode<Descriptors.Descriptor>> message_descs = new HashMap<String, PbAliasNode<Descriptors.Descriptor>>();
        public HashMap<String, HashMap<Integer, Descriptors.EnumValueDescriptor>> enum_values_descs = new HashMap<String, HashMap<Integer, Descriptors.EnumValueDescriptor>>();
        /*** 类型信息-快速整数别名集合 ***/
        public HashMap<String, Integer> quick_integer_values_alias = new HashMap<String, Integer>();
        public HashSet<String> quick_integer_values_container = new HashSet<String>();

        // ========================== 验证器 ==========================
        HashMap<String, DataVerifyImpl> validator = new HashMap<String, DataVerifyImpl>();
        HashMap<String, HashMap<String, DataVerifyImpl>> mixedCustomValidatorFiles = new HashMap<String, HashMap<String, DataVerifyImpl>>();
        HashMap<String, HashMap<String, DataVerifyImpl>> oneCustomValidatorFiles = new HashMap<String, HashMap<String, DataVerifyImpl>>();
        HashMap<String, DataVerifyImpl> stableValidator = new HashMap<String, DataVerifyImpl>();

        // ========================== 内建AST类型缓存 ==========================
        HashMap<String, DataDstTypeDescriptor> dataDstDescs = new HashMap<String, DataDstTypeDescriptor>();

        public PbInfoSet() {
        }
    }

    static private class SetupValidatorResult {
        public boolean success = true;
        public LinkedList<DataVerifyImpl> validator = null;
    }

    private Descriptors.Descriptor currentMsgDesc = null;
    static private com.google.protobuf.ExtensionRegistryLite pb_extensions = null;
    static private PbInfoSet cachePbs = new PbInfoSet();
    static private HashMap<String, Descriptors.FileDescriptor> inner_file_descs = null;

    static <T> void append_alias_list(String short_name, String full_name, HashMap<String, PbAliasNode<T>> hashmap,
            T ele) {
        while (full_name.length() > 0 && full_name.charAt(0) == '.') {
            full_name = full_name.substring(1);
        }

        if (!short_name.isEmpty() && !short_name.equals(full_name)) {
            PbAliasNode<T> ls = hashmap.getOrDefault(short_name, null);
            if (null == ls) {
                ls = new PbAliasNode<T>();
                hashmap.put(short_name, ls);
            }
            if (null == ls.names) {
                ls.names = new LinkedList<String>();
            }
            ls.element = ele;
            if (!full_name.isEmpty()) {
                ls.names.addLast(full_name);
            }
        }

        if (!full_name.isEmpty()) {
            PbAliasNode<T> ls = hashmap.getOrDefault(full_name, null);
            if (null == ls) {
                ls = new PbAliasNode<T>();
                hashmap.put(full_name, ls);
            }
            ls.element = ele;
        }
    }

    static <T> T get_alias_list_element(String name, HashMap<String, PbAliasNode<T>> hashmap, String type_name) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }
        PbAliasNode<T> ls = hashmap.getOrDefault(name, null);
        if (null == ls || null == ls.element) {
            return null;
        }

        if (null == ls.names || ls.names.size() <= 1) {
            return ls.element;
        }

        ProgramOptions.getLoger().error(
                "There is more than one %s \"%s\" matched, please use full name. available names:", type_name, name);
        for (String full_name : ls.names) {
            ProgramOptions.getLoger().error("\t%s", full_name);
        }

        return null;
    }

    static <T> String get_alias_list_element_full_name(String name, HashMap<String, PbAliasNode<T>> hashmap,
            String type_name) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }
        PbAliasNode<T> ls = hashmap.getOrDefault(name, null);
        if (null == ls || null == ls.element) {
            return name;
        }

        if (null != ls.names && ls.names.size() > 1) {
            ProgramOptions.getLoger().error(
                    "There is more than one %s \"%s\" matched, please use full name. available names:", type_name,
                    name);
            for (String full_name : ls.names) {
                ProgramOptions.getLoger().error("\t%s", full_name);
            }
        }

        if (null != ls.names && ls.names.size() == 1) {
            return ls.names.get(0);
        }

        return name;
    }

    static private com.google.protobuf.ExtensionRegistryLite get_extension_registry() {
        if (null != pb_extensions) {
            return pb_extensions;
        }

        pb_extensions = com.google.protobuf.ExtensionRegistryLite.newInstance();

        Xresloader.registerAllExtensions(pb_extensions);
        XresloaderUe.registerAllExtensions(pb_extensions);

        return pb_extensions;
    }

    static private Integer get_quick_integer_value(PbInfoSet pbs, String name) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }

        int dotPos = name.lastIndexOf('.');
        if (dotPos < 0) {
            return null;
        }

        var ret = pbs.quick_integer_values_alias.getOrDefault(name, null);
        if (ret != null) {
            return ret;
        }

        String container = name.substring(0, dotPos);
        if (pbs.quick_integer_values_container.contains(container)) {
            return null;
        }
        pbs.quick_integer_values_container.add(container);

        // Try enum
        var enumDesc = get_alias_list_element(container, pbs.enums, "enum type");
        if (enumDesc != null) {
            for (var enumValue : enumDesc.getValueList()) {
                pbs.quick_integer_values_alias.put(String.format("%s.%s", container, enumValue.getName()),
                        Integer.valueOf(enumValue.getNumber()));
            }
            return pbs.quick_integer_values_alias.getOrDefault(name, null);
        }

        // Try message
        var msgDesc = get_alias_list_element(container, pbs.messages, "message type");
        if (msgDesc != null) {
            for (var fieldDesc : msgDesc.getFieldList()) {
                pbs.quick_integer_values_alias.put(String.format("%s.%s", container, fieldDesc.getName()),
                        Integer.valueOf(fieldDesc.getNumber()));
            }
            return pbs.quick_integer_values_alias.getOrDefault(name, null);
        }

        return null;
    }

    static private Integer try_parse_quick_integer_value(PbInfoSet pbs, String name) {
        name = name.trim();
        if (name.isEmpty()) {
            return null;
        }

        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            try {
                return Integer.valueOf(name);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return get_quick_integer_value(pbs, name);
    }

    static void load_pb_message(PbInfoSet pbs, DescriptorProtos.DescriptorProto mdp, String package_name,
            HashMap<String, PbAliasNode<DescriptorProtos.DescriptorProto>> hashmap) {
        String full_name = String.format("%s.%s", package_name, mdp.getName());
        append_alias_list(mdp.getName(), full_name, pbs.messages, mdp);

        // nest messages
        for (DescriptorProtos.DescriptorProto sub_mdp : mdp.getNestedTypeList()) {
            load_pb_message(pbs, sub_mdp, full_name, hashmap);
        }

        // enums
        for (DescriptorProtos.EnumDescriptorProto edp : mdp.getEnumTypeList()) {
            append_alias_list(edp.getName(), String.format("%s.%s", full_name, edp.getName()), pbs.enums, edp);
        }

        // oneof
        for (DescriptorProtos.OneofDescriptorProto oneof_desc : mdp.getOneofDeclList()) {
            append_alias_list(oneof_desc.getName(),
                    String.format("%s.%s.%s", package_name, mdp.getName(), oneof_desc.getName()), pbs.oneofs,
                    oneof_desc);
        }
    }

    static boolean load_pb_file(PbInfoSet pbs, String file_path, boolean build_msg, boolean allow_unknown_dependencies,
            com.google.protobuf.ExtensionRegistryLite exts) {
        if (pbs.fileNames.contains(file_path)) {
            return true;
        }

        try {
            if (exts == null) {
                exts = get_extension_registry();
            }

            // 文件描述集读取
            InputStream fis = new FileInputStream(file_path);
            DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis, exts);
            pbs.fileNames.add(file_path);
            // 保存文件名和文件描述Proto的关系
            for (DescriptorProtos.FileDescriptorProto fdp : fds.getFileList()) {
                if (pbs.files.containsKey(fdp.getName())) {
                    continue;
                }
                pbs.files.put(fdp.getName(), fdp);

                if (build_msg) {
                    for (DescriptorProtos.EnumDescriptorProto edp : fdp.getEnumTypeList()) {
                        append_alias_list(edp.getName(), String.format("%s.%s", fdp.getPackage(), edp.getName()),
                                pbs.enums, edp);
                    }

                    for (DescriptorProtos.DescriptorProto mdp : fdp.getMessageTypeList()) {
                        load_pb_message(pbs, mdp, fdp.getPackage(), pbs.messages);
                    }
                }
            }

            // 初始化
            if (build_msg) {
                for (HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fme : pbs.files.entrySet()) {
                    init_pb_files(pbs, fme.getKey(), allow_unknown_dependencies);
                }
            }

        } catch (FileNotFoundException e) {
            ProgramOptions.getLoger().error("Read protocol file \"%s\" failed. %s",
                    String.join(",", ProgramOptions.getInstance().protocolFile), e.getMessage());
            return false;
        } catch (IOException e) {
            ProgramOptions.getLoger().error("Parse protocol file \"%s\" failed. %s",
                    String.join(",", ProgramOptions.getInstance().protocolFile), e.getMessage());
            return false;
        }

        // 载入完成,swap
        return true;
    }

    static Descriptors.Descriptor get_message_proto(PbInfoSet pbs, String proto_name) {
        return get_alias_list_element(proto_name, pbs.message_descs, "protocol message");
    }

    static Descriptors.EnumValueDescriptor get_enum_value(PbInfoSet pbs, Descriptors.EnumDescriptor enum_desc,
            Integer val) {
        String name = enum_desc.getFullName();
        while (!name.isEmpty() && '.' == name.charAt(0)) {
            name = name.substring(1);
        }
        HashMap<Integer, Descriptors.EnumValueDescriptor> cache_set = pbs.enum_values_descs.getOrDefault(name, null);
        if (cache_set == null) {
            cache_set = new HashMap<Integer, Descriptors.EnumValueDescriptor>();
            pbs.enum_values_descs.put(name, cache_set);
            for (Descriptors.EnumValueDescriptor enum_val : enum_desc.getValues()) {
                cache_set.put(Integer.valueOf(enum_val.getNumber()), enum_val);
            }
        }

        return cache_set.getOrDefault(val, null);
    }

    static Descriptors.FileDescriptor try_get_inner_detail_desc(String name) {
        if (inner_file_descs != null) {
            return inner_file_descs.getOrDefault(name.replace('\\', '/').toLowerCase(), null);
        }

        Descriptors.FileDescriptor[] inner_descs = new Descriptors.FileDescriptor[] { Xresloader.getDescriptor(),
                XresloaderUe.getDescriptor(), PbHeaderV3.getDescriptor(), DescriptorProtos.getDescriptor(),
                DurationProto.getDescriptor(), TimestampProto.getDescriptor(), AnyProto.getDescriptor(),
                ApiProto.getDescriptor(), EmptyProto.getDescriptor(), FieldMaskProto.getDescriptor(),
                StructProto.getDescriptor(), TypeProto.getDescriptor(), WrappersProto.getDescriptor(),
                SourceContextProto.getDescriptor(), };

        inner_file_descs = new HashMap<String, Descriptors.FileDescriptor>();
        for (Descriptors.FileDescriptor innerFileDesc : inner_descs) {
            inner_file_descs.put(innerFileDesc.getName().replace('\\', '/').toLowerCase(), innerFileDesc);
        }

        return inner_file_descs.getOrDefault(name.toLowerCase(), null);
    }

    static Descriptors.FileDescriptor init_pb_files(PbInfoSet pbs, String name, boolean allow_unknown_dependencies) {
        Descriptors.FileDescriptor ret = pbs.file_descs.getOrDefault(name, null);
        if (null != ret) {
            return ret;
        }

        if (pbs.file_descs_failed.contains(name)) {
            return null;
        }

        DescriptorProtos.FileDescriptorProto fdp = pbs.files.getOrDefault(name, null);
        if (null == fdp) {
            // Inner proto files
            String standardName = name.replace('\\', '/');
            Descriptors.FileDescriptor innerFileDesc = try_get_inner_detail_desc(standardName);
            if (null != innerFileDesc) {
                return innerFileDesc;
            }

            if (standardName.equalsIgnoreCase("pb_header.proto")) {
                return PbHeaderV3.getDescriptor();
            }

            if (allow_unknown_dependencies) {
                ProgramOptions.getLoger().warn("Protocol file descriptor %s not found.", name);
            } else {
                ProgramOptions.getLoger().error("Protocol file descriptor %s not found.", name);
            }

            pbs.file_descs_failed.add(name);
            return null;
        }

        ArrayList<Descriptors.FileDescriptor> deps = new ArrayList<Descriptors.FileDescriptor>();
        ArrayList<String> failed_deps = new ArrayList<String>();
        deps.ensureCapacity(fdp.getDependencyCount());
        failed_deps.ensureCapacity(fdp.getDependencyCount());
        for (int i = 0; i < fdp.getDependencyCount(); ++i) {
            Descriptors.FileDescriptor dep = init_pb_files(pbs, fdp.getDependency(i), allow_unknown_dependencies);
            if (null == dep) {
                if (allow_unknown_dependencies) {
                    failed_deps.add(fdp.getDependency(i));
                } else {
                    ProgramOptions.getLoger().error("Initialize protocol file descriptor %s failed. dependency %s",
                            name, fdp.getDependency(i));
                    return null;
                }
            } else {
                deps.add(dep);
            }
        }

        if (!failed_deps.isEmpty()) {
            ProgramOptions.getLoger().warn(
                    "Initialize protocol file descriptor %s without dependency %s, maybe missing some descriptor(s).",
                    name, String.join(",", failed_deps));
        }

        try {
            Descriptors.FileDescriptor[] a = new Descriptors.FileDescriptor[deps.size()];
            ret = Descriptors.FileDescriptor.buildFrom(fdp, deps.toArray(a), allow_unknown_dependencies);
            pbs.file_descs.put(name, ret);
            for (Descriptors.Descriptor md : ret.getMessageTypes()) {
                append_alias_list(md.getName(), String.format("%s.%s", fdp.getPackage(), md.getName()),
                        pbs.message_descs, md);
            }

            return ret;
        } catch (Descriptors.DescriptorValidationException e) {
            ProgramOptions.getLoger().error("Initialize protocol file descriptor %s failed. %s", name, e.getMessage());
            return null;
        }
    }

    static private boolean setup_extension(DataDstTypeDescriptor parent_message, DataDstFieldDescriptor child_field,
            Descriptors.FieldDescriptor fd) {
        String verifierExpr = null;
        if (fd.getOptions().hasExtension(Xresloader.validator)) {
            verifierExpr = fd.getOptions().getExtension(Xresloader.validator);
        }
        // 兼容旧版本
        if (fd.getOptions().hasExtension(Xresloader.verifier)) {
            if (verifierExpr == null) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.verifier);
            } else {
                verifierExpr = String.join("|", verifierExpr, fd.getOptions().getExtension(Xresloader.verifier));
            }
        }
        if (verifierExpr != null && !verifierExpr.isEmpty()) {
            child_field.mutableExtension().validator = verifierExpr;
        }
        boolean ret = true;
        var gen = setup_validator(null, verifierExpr, fd);
        if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
            for (DataVerifyImpl vfy : gen.validator) {
                child_field.addValidator(vfy);
            }
        } else {
            child_field.resetValidator();
        }

        do {
            if (!fd.isMapField()) {
                break;
            }
            if (!fd.getOptions().hasExtension(Xresloader.mapKeyValidator) &&
                    !fd.getOptions().hasExtension(Xresloader.mapValueValidator)) {
                break;
            }

            DataDstTypeDescriptor mapTypeDesc = child_field.getTypeDescriptor();
            if (null == mapTypeDesc) {
                break;
            }

            DataDstFieldDescriptor mapKeyField = null;
            DataDstFieldDescriptor mapValueField = null;
            for (DataDstFieldDescriptor mapField : mapTypeDesc.getSortedFields()) {
                if (mapField.getName().equalsIgnoreCase("key")) {
                    mapKeyField = mapField;
                } else if (mapField.getName().equalsIgnoreCase("value")) {
                    mapValueField = mapField;
                }
            }
            if (mapKeyField == null || mapValueField == null) {
                break;
            }

            if (fd.getOptions().hasExtension(Xresloader.mapKeyValidator)) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.mapKeyValidator);
                if (verifierExpr != null && !verifierExpr.isEmpty()) {
                    mapKeyField.mutableExtension().validator = verifierExpr;
                    gen = setup_validator(null, verifierExpr, null);
                    if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
                        for (DataVerifyImpl vfy : gen.validator) {
                            mapKeyField.addValidator(vfy);
                        }
                    } else {
                        mapKeyField.resetValidator();
                    }
                }
            }

            if (fd.getOptions().hasExtension(Xresloader.mapValueValidator)) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.mapValueValidator);
                if (verifierExpr != null && !verifierExpr.isEmpty()) {
                    mapValueField.mutableExtension().validator = verifierExpr;
                    gen = setup_validator(null, verifierExpr, null);
                    if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
                        for (DataVerifyImpl vfy : gen.validator) {
                            mapValueField.addValidator(vfy);
                        }
                    } else {
                        mapValueField.resetValidator();
                    }
                }
            }
        } while (false);

        if (fd.getOptions().hasExtension(Xresloader.fieldDescription)) {
            child_field.mutableExtension().description = fd.getOptions().getExtension(Xresloader.fieldDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldRatio)) {
            child_field.mutableExtension().ratio = fd.getOptions().getExtension(Xresloader.fieldRatio);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldSeparator)) {
            child_field.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.fieldSeparator);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldNotNull)) {
            child_field.mutableExtension().notNull = fd.getOptions().getExtension(Xresloader.fieldNotNull);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldAllowMissingInPlainMode)) {
            child_field.mutableExtension().allowMissingInPlainMode = fd.getOptions()
                    .getExtension(Xresloader.fieldAllowMissingInPlainMode);
        }

        if (fd.getOptions().getExtensionCount(Xresloader.fieldUniqueTag) > 0) {
            var ext = child_field.mutableExtension();
            ext.uniqueTags = new ArrayList<>();
            ext.uniqueTags.ensureCapacity(fd.getOptions().getExtensionCount(Xresloader.fieldUniqueTag));
            for (String tag : fd.getOptions().getExtension(Xresloader.fieldUniqueTag)) {
                ext.uniqueTags.add(tag);
            }
        }

        // origin refer
        if (fd.getOptions().hasExtension(Xresloader.fieldOriginValue)) {
            String originValue = fd.getOptions().getExtension(Xresloader.fieldOriginValue);
            DataDstFieldDescriptor refer = parent_message.fields.getOrDefault(originValue, null);
            do {
                if (refer == null) {
                    ProgramOptions.getLoger().error(
                            "field_origin_value \"%s\" of \"%s\" not found, we will ignore this plugin", originValue,
                            fd.getFullName());
                    break;
                }
                if (refer.getType() != DataDstWriterNode.JAVA_TYPE.STRING) {
                    ProgramOptions.getLoger().warn(
                            "field_origin_value \"%s\" of \"%s\" is not a string, we will ignore this plugin",
                            originValue, fd.getFullName());
                    break;
                }

                if (child_field.isList() && !refer.isList()) {
                    ProgramOptions.getLoger().warn(
                            "\"%s\" is repeated but field_origin_value \"%s\" is not, we will ignore this plugin",
                            fd.getFullName(), originValue);
                    break;
                }

                if (!child_field.isList() && refer.isList()) {
                    ProgramOptions.getLoger().warn(
                            "\"%s\" is not repeated but field_origin_value \"%s\" is a repeated field, we will ignore this plugin",
                            fd.getFullName(), originValue);
                    break;
                }

                child_field.setReferOriginField(refer);
            } while (false);
        }

        // setup UE extension
        if (fd.getOptions().hasExtension(XresloaderUe.keyTag)) {
            child_field.mutableExtension().mutableUE().keyTag = fd.getOptions().getExtension(XresloaderUe.keyTag);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueTypeName)) {
            child_field.mutableExtension().mutableUE().ueTypeName = fd.getOptions()
                    .getExtension(XresloaderUe.ueTypeName);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueTypeIsClass)) {
            child_field.mutableExtension().mutableUE().ueTypeIsClass = fd.getOptions()
                    .getExtension(XresloaderUe.ueTypeIsClass);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueOriginTypeName)) {
            child_field.mutableExtension().mutableUE().ueOriginTypeName = fd.getOptions()
                    .getExtension(XresloaderUe.ueOriginTypeName);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueOriginTypeDefaultValue)) {
            child_field.mutableExtension().mutableUE().ueOriginTypeDefaultValue = fd.getOptions()
                    .getExtension(XresloaderUe.ueOriginTypeDefaultValue);
        }

        // setup list extension
        if (fd.isRepeated()) {
            if (fd.getOptions().hasExtension(Xresloader.fieldListStripOption)) {
                switch (fd.getOptions().getExtension(Xresloader.fieldListStripOption).getNumber()) {
                    case Xresloader.ListStripOption.LIST_STRIP_DEFAULT_VALUE: {
                        child_field.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.DEFAULT;
                        break;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_NOTHING_VALUE: {
                        child_field.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_NOTHING;
                        break;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_TAIL_VALUE: {
                        child_field.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_TAIL;
                        break;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_ALL_VALUE: {
                        child_field.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_ALL;
                        break;
                    }
                }
            }

            if (fd.getOptions().hasExtension(Xresloader.fieldListMinSize)) {
                var intValue = try_parse_quick_integer_value(cachePbs,
                        fd.getOptions().getExtension(Xresloader.fieldListMinSize));
                if (null == intValue) {
                    ProgramOptions.getLoger().error(
                            "\"%s\" has invalid extension settings field_list_min_size (%s)",
                            fd.getFullName(),
                            fd.getOptions().getExtension(Xresloader.fieldListMinSize));
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                } else {
                    child_field.mutableExtension()
                            .mutableList().minSize = intValue.intValue();
                }
            }
            if (fd.getOptions().hasExtension(Xresloader.fieldListMaxSize)) {
                var intValue = try_parse_quick_integer_value(cachePbs,
                        fd.getOptions().getExtension(Xresloader.fieldListMaxSize));
                if (null == intValue) {
                    ProgramOptions.getLoger().error(
                            "\"%s\" has invalid extension settings field_list_max_size (%s)",
                            fd.getFullName(),
                            fd.getOptions().getExtension(Xresloader.fieldListMaxSize));
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                } else {
                    child_field.mutableExtension()
                            .mutableList().maxSize = intValue.intValue();
                }

                if (child_field.mutableExtension()
                        .mutableList().maxSize != 0
                        && child_field.mutableExtension()
                                .mutableList().maxSize < child_field.mutableExtension()
                                        .mutableList().minSize) {
                    ProgramOptions.getLoger().error(
                            "\"%s\" has invalid extension settings(field_list_max_size (%d) is smaller than field_list_min_size (%d)",
                            fd.getFullName(),
                            child_field.mutableExtension()
                                    .mutableList().maxSize,
                            child_field.mutableExtension()
                                    .mutableList().minSize);
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                }
            }
            if (fd.getOptions().hasExtension(Xresloader.fieldListStrictSize)) {
                child_field.mutableExtension()
                        .mutableList().strictSize = fd.getOptions().getExtension(Xresloader.fieldListStrictSize);
            }
        }

        if (gen == null) {
            return ret;
        }
        return gen.success;
    }

    static private boolean setup_extension(DataDstTypeDescriptor parent_message, DataDstOneofDescriptor child_field,
            Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        var gen = setup_validator(null, container, fd);
        if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
            for (DataVerifyImpl vfy : gen.validator) {
                child_field.addValidator(vfy);
            }
        } else {
            child_field.resetValidator();
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofDescription)) {
            child_field.mutableExtension().description = fd.getOptions().getExtension(Xresloader.oneofDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofSeparator)) {
            child_field.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.oneofSeparator);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofNotNull)) {
            child_field.mutableExtension().notNull = fd.getOptions().getExtension(Xresloader.oneofNotNull);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofAllowMissingInPlainMode)) {
            child_field.mutableExtension().allowMissingInPlainMode = fd.getOptions()
                    .getExtension(Xresloader.oneofAllowMissingInPlainMode);
        }

        if (gen == null) {
            return true;
        }
        return gen.success;
    }

    static private DataVerifyImpl getValidatorFromCache(String name) {
        DataVerifyImpl vfy = cachePbs.validator.getOrDefault(name, null);
        if (null != vfy) {
            return vfy;
        }

        vfy = cachePbs.stableValidator.getOrDefault(name, null);
        if (null != vfy) {
            cachePbs.validator.put(vfy.getName(), vfy);
            return vfy;
        }
        return vfy;
    }

    static private void setValidatorStableCache(String name, DataVerifyImpl vfy) {
        if (vfy == null) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }

        cachePbs.validator.put(name, vfy);
        cachePbs.stableValidator.put(name, vfy);
    }

    static private HashMap<String, DataVerifyImpl> buildCustomValidator(String filePath,
            HashMap<String, HashMap<String, DataVerifyImpl>> cache) {
        HashMap<String, DataVerifyImpl> result = cache.getOrDefault(filePath, null);

        if (result != null) {
            return result;
        }

        result = DataVerifyCustomRule.loadFromFile(filePath);

        if (result != null) {
            cache.put(filePath, result);
        }
        return result;
    }

    static private SetupValidatorResult setup_validator(LinkedList<DataVerifyImpl> result,
            Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        SetupValidatorResult ret = new SetupValidatorResult();

        if (result == null) {
            result = new LinkedList<DataVerifyImpl>();
        }
        ret.success = true;
        ret.validator = result;

        String rule = String.format("%s.%s.%s", container.getFile().getPackage(), container.getName(), fd.getName());
        if (rule.length() > 0 && rule.charAt(0) == '.') {
            rule = rule.substring(1);
        }
        {
            DataVerifyImpl vfy = getValidatorFromCache(rule);
            // 命中缓存
            if (null != vfy) {
                result.add(vfy);
                return ret;
            }
        }

        DataVerifyPbOneof new_vfy = new DataVerifyPbOneof(fd);
        setValidatorStableCache(rule, new_vfy);
        result.add(new_vfy);

        return ret;
    }

    static private DataVerifyImpl createValidator(DataVerifyImpl.ValidatorTokens ruleObject) {
        if (ruleObject == null) {
            return null;
        }

        // 第一优先级，函数验证器
        if (ruleObject.parameters.size() > 1) {
            if (ruleObject.parameters.get(0).equalsIgnoreCase("InText")) {
                DataVerifyInText vfyInText = new DataVerifyInText(ruleObject);
                if (vfyInText.isValid()) {
                    return vfyInText;
                } else {
                    ProgramOptions.getLoger().error("Validator %s(DataVerifyInText) is invalid",
                            ruleObject.name);
                }
                return null;
            }

            if (ruleObject.parameters.get(0).equalsIgnoreCase("InTableColumn")) {
                DataVerifyInTableColumn vfyInTableColumn = new DataVerifyInTableColumn(ruleObject);
                if (vfyInTableColumn.isValid()) {
                    return vfyInTableColumn;
                } else {
                    ProgramOptions.getLoger().error("Validator %s(DataVerifyInTableColumn) is invalid",
                            ruleObject.name);
                }
                return null;
            }

            return null;
        }

        // 第二优先级，范围验证器
        if (ruleObject.name.charAt(0) == '-' || (ruleObject.name.charAt(0) >= '0' && ruleObject.name.charAt(0) <= '9')
                || (ruleObject.name.charAt(0) == '>' || ruleObject.name.charAt(0) == '<')) {
            DataVerifyIntRange vfyRange = new DataVerifyIntRange(ruleObject.name);
            if (vfyRange.isValid()) {
                return vfyRange;
            } else {
                ProgramOptions.getLoger().error("Validator %s(DataVerifyIntRange) is invalid",
                        ruleObject.name);
            }
            return null;
        }

        // 第三优先级，协议验证器
        DescriptorProtos.EnumDescriptorProto enum_desc = get_alias_list_element(ruleObject.name, cachePbs.enums,
                "enum type");
        if (enum_desc != null) {
            return new DataVerifyPbEnum(enum_desc);
        }

        DescriptorProtos.DescriptorProto msg_desc = get_alias_list_element(ruleObject.name, cachePbs.messages,
                "message type");
        if (msg_desc != null) {
            return new DataVerifyPbMsgField(msg_desc);
        }

        DescriptorProtos.OneofDescriptorProto oneof_desc = get_alias_list_element(ruleObject.name,
                cachePbs.oneofs, "oneof type");
        if (oneof_desc != null) {
            int message_bound = ruleObject.name.lastIndexOf('.');
            if (message_bound > 0 && message_bound < ruleObject.name.length()) {
                msg_desc = get_alias_list_element(ruleObject.name.substring(0, message_bound),
                        cachePbs.messages, "message type");
            } else {
                String oneof_full_name = get_alias_list_element_full_name(ruleObject.name, cachePbs.oneofs,
                        "oneof type");
                message_bound = oneof_full_name.lastIndexOf('.');
                if (message_bound > 0 && message_bound < oneof_full_name.length()) {
                    msg_desc = get_alias_list_element(oneof_full_name.substring(0, message_bound),
                            cachePbs.messages, "message type");
                }
            }

            if (oneof_desc != null && msg_desc != null) {
                return new DataVerifyPbOneof(oneof_desc, msg_desc);
            }
        }

        return null;
    }

    static private SetupValidatorResult setup_validator(LinkedList<DataVerifyImpl> result, String verifier,
            Descriptors.FieldDescriptor fd) {
        if (verifier == null) {
            verifier = "";
        } else {
            verifier = verifier.trim();
        }

        String autoValidatorRule = null;
        if (fd != null && fd.getJavaType() == JavaType.ENUM) {
            autoValidatorRule = fd.getEnumType().getFullName();
            if (autoValidatorRule.length() > 0 && autoValidatorRule.charAt(0) == '.') {
                autoValidatorRule = autoValidatorRule.substring(1);
            }
        }

        if (verifier.isEmpty() && autoValidatorRule == null) {
            return null;
        }

        SetupValidatorResult ret = new SetupValidatorResult();
        if (result == null) {
            result = new LinkedList<DataVerifyImpl>();
        }
        ret.success = true;
        ret.validator = result;

        boolean containsAutoValidator = false;
        if (verifier != null && !verifier.isEmpty()) {
            var allRules = DataVerifyImpl.buildValidators(verifier);
            for (DataVerifyImpl.ValidatorTokens ruleObject : allRules) {
                DataVerifyImpl vfy = getValidatorFromCache(ruleObject.name);
                boolean isCached = false;
                if (null != vfy) {
                    isCached = true;
                } else {
                    vfy = createValidator(ruleObject);
                }

                if (vfy != null) {
                    result.add(vfy);

                    if (autoValidatorRule != null && ruleObject.name.equals(autoValidatorRule)) {
                        containsAutoValidator = true;
                    }

                    if (!isCached) {
                        setValidatorStableCache(ruleObject.name, vfy);
                    }
                } else {
                    ProgramOptions.getLoger().error("Unknown validator %s",
                            ruleObject.name);
                    ret.success = false;
                }
            }
        }

        // auto verifier for enum
        if (autoValidatorRule != null && !containsAutoValidator) {
            DataVerifyImpl vfy = getValidatorFromCache(autoValidatorRule);
            if (null == vfy) {
                DescriptorProtos.EnumDescriptorProto enum_desc = get_alias_list_element(autoValidatorRule,
                        cachePbs.enums,
                        "enum type");
                if (enum_desc != null) {
                    vfy = new DataVerifyPbEnum(enum_desc);
                }

                if (null != vfy) {
                    setValidatorStableCache(autoValidatorRule, vfy);
                } else {
                    ProgramOptions.getLoger().error("Enum verifier \"%s\" setup error, please report this bug to %s",
                            autoValidatorRule, ProgramOptions.getReportUrl());
                    ret.success = false;
                }
            }

            if (vfy != null) {
                result.add(vfy);
            }
        }

        return ret;
    }

    private void setup_node_identify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.FieldDescriptor fd) {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetValidator();

        if (null != identify.dataSourceFieldValidator && !identify.dataSourceFieldValidator.isEmpty()) {
            SetupValidatorResult gen = setup_validator(null, identify.dataSourceFieldValidator, fd);
            if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
                for (DataVerifyImpl vfy : gen.validator) {
                    identify.addValidator(vfy);
                }
            } else {
                identify.resetValidator();
            }
        }

        // merge verifier from field descriptor
        if (child.innerFieldDesc != null && child.innerFieldDesc.hasValidator()) {
            for (DataVerifyImpl vfy : child.innerFieldDesc.getValidator()) {
                identify.addValidator(vfy);
            }
        }
    }

    private void setup_node_identify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.OneofDescriptor fd) {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetValidator();

        // Data source field verifier is ignored in oneof descriptor

        // merge verifier from oneof descriptor
        if (child.innerOneofDesc != null && child.innerOneofDesc.hasValidator()) {
            for (DataVerifyImpl vfy : child.innerOneofDesc.getValidator()) {
                identify.addValidator(vfy);
            }
        }
    }

    @Override
    public boolean init() {
        for (String pbsFile : ProgramOptions.getInstance().protocolFile) {
            if (false == load_pb_file(cachePbs, pbsFile, true,
                    ProgramOptions.getInstance().protocolIgnoreUnknownDependency, null)) {
                return false;
            }
        }

        currentMsgDesc = get_message_proto(cachePbs, SchemeConf.getInstance().getProtoName());
        if (null == currentMsgDesc) {
            this.logErrorMessage("Can not find protocol message %s", SchemeConf.getInstance().getProtoName());
            return false;
        }

        // reset validator set
        cachePbs.validator.clear();

        // Setup custom validators
        String[] customValidatorRules = ProgramOptions.getInstance().customValidatorRules;
        boolean oneFileCustomValidatorMode = false;
        if (customValidatorRules != null) {
            if (customValidatorRules.length == 1) {
                oneFileCustomValidatorMode = true;
            }

            for (String ruleFilePath : customValidatorRules) {
                HashMap<String, DataVerifyImpl> validatorSet = buildCustomValidator(ruleFilePath,
                        oneFileCustomValidatorMode ? cachePbs.oneCustomValidatorFiles
                                : cachePbs.mixedCustomValidatorFiles);
                if (null == validatorSet) {
                    this.logErrorMessage("Can not build custom validators from file \"%s\"",
                            ruleFilePath);
                    return false;
                }

                for (var vfyPair : validatorSet.entrySet()) {
                    if (null != cachePbs.validator.put(vfyPair.getKey(), vfyPair.getValue())) {
                        ProgramOptions.getLoger().warn(
                                "Load custom validator with more than one rule with name \"%s\", we will use the last one.",
                                vfyPair.getKey());
                    }
                }
            }
        }
        ArrayList<DataVerifyCustomRule> customValidators = new ArrayList<>();
        customValidators.ensureCapacity(cachePbs.validator.size());
        for (DataVerifyImpl vfy : cachePbs.validator.values()) {
            if (vfy instanceof DataVerifyCustomRule) {
                if (oneFileCustomValidatorMode && ((DataVerifyCustomRule) vfy).hasChecked()) {
                    continue;
                }
                customValidators.add((DataVerifyCustomRule) vfy);
            }
        }

        boolean ret = true;
        for (DataVerifyCustomRule vfy : customValidators) {
            var rules = vfy.getRules();
            ArrayList<DataVerifyImpl> deps = new ArrayList<DataVerifyImpl>();
            deps.ensureCapacity(rules.size());
            for (int i = 0; i < rules.size(); ++i) {
                DataVerifyImpl findDep = getValidatorFromCache(rules.get(i));
                if (null != findDep) {
                    deps.add(findDep);
                    continue;
                }

                LinkedList<ValidatorTokens> tokensList = DataVerifyImpl.buildValidators(rules.get(i));
                for (ValidatorTokens tokens : tokensList) {
                    findDep = createValidator(tokens);
                    if (null == findDep) {
                        ProgramOptions.getLoger().error("Unknown validator %s", tokens.name);
                        ret = false;
                    } else {
                        deps.add(findDep);
                        continue;
                    }
                }
            }
            vfy.setup(deps);
        }

        return ret;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "protobuf";
    }

    static private boolean checkFieldIsRequired(Descriptors.FieldDescriptor field) {
        if (field == null) {
            return false;
        }

        if (field.isRequired()) {
            return true;
        }

        if (field.getOptions().hasExtension(Xresloader.fieldRequired)) {
            return field.getOptions().getExtension(Xresloader.fieldRequired);
        }

        return false;
    }

    static private void buildDataDstDescriptorMessage(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstTypeDescriptor innerDesc) throws ConvException {
        if (null == pbDesc || null == innerDesc) {
            return;
        }

        innerDesc.fields = new HashMap<String, DataDstFieldDescriptor>();
        innerDesc.oneofs = new HashMap<String, DataDstOneofDescriptor>();

        for (Descriptors.FieldDescriptor field : pbDesc.getFields()) {
            Descriptors.Descriptor fieldPbDesc = null;
            if (field.getJavaType() == JavaType.MESSAGE) {
                fieldPbDesc = field.getMessageType();
            }

            DataDstWriterNode.FIELD_LABEL_TYPE inner_label = DataDstWriterNode.FIELD_LABEL_TYPE.OPTIONAL;
            if (field.isRepeated()) {
                inner_label = DataDstWriterNode.FIELD_LABEL_TYPE.LIST;
            } else if (checkFieldIsRequired(field)) {
                inner_label = DataDstWriterNode.FIELD_LABEL_TYPE.REQUIRED;
            }
            DataDstFieldDescriptor innerField = new DataDstFieldDescriptor(
                    mutableDataDstDescriptor(pbs, fieldPbDesc, pbTypeToInnerType(field.getType()),
                            pbTypeToTypeLimit(field.getType())),
                    field.getNumber(),
                    field.getName(), inner_label, field);
            innerDesc.fields.put(field.getName(), innerField);
        }

        for (Descriptors.OneofDescriptor oneof : pbDesc.getOneofs()) {
            HashMap<String, DataDstFieldDescriptor> fields = new HashMap<String, DataDstFieldDescriptor>();
            for (Descriptors.FieldDescriptor field : oneof.getFields()) {
                DataDstFieldDescriptor find_field = innerDesc.fields.getOrDefault(field.getName(), null);
                if (find_field != null) {
                    fields.put(field.getName(), find_field);
                }
            }
            DataDstOneofDescriptor innerField = new DataDstOneofDescriptor(innerDesc, fields, oneof.getIndex(),
                    oneof.getName(), oneof);
            innerDesc.oneofs.put(oneof.getName(), innerField);
        }

        // 最后设置插件
        for (Descriptors.FieldDescriptor field : pbDesc.getFields()) {
            DataDstFieldDescriptor innerField = innerDesc.fields.getOrDefault(field.getName(), null);
            if (null != innerField) {
                if (!setup_extension(innerDesc, innerField, field)) {
                    throw new ConvException(
                            String.format("Setup extension failed, field name: %s", field.getFullName()));
                }
            }
        }

        for (Descriptors.OneofDescriptor oneof : pbDesc.getOneofs()) {
            DataDstOneofDescriptor innerField = innerDesc.oneofs.getOrDefault(oneof.getName(), null);
            if (null != innerField) {
                if (!setup_extension(innerDesc, innerField, pbDesc, oneof)) {
                    throw new ConvException(
                            String.format("Setup extension failed, field name: %s", oneof.getFullName()));
                }
            }
        }
    }

    static private DataDstTypeDescriptor mutableDataDstDescriptor(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type, DataDstWriterNode.SPECIAL_TYPE_LIMIT typeLimit) throws ConvException {
        String key = null;
        if (null == pbDesc) {
            key = type.toString();
        } else {
            key = pbDesc.getFullName();
        }
        key = String.format("%s:%s", key, typeLimit.toString());
        DataDstTypeDescriptor ret = pbs.dataDstDescs.getOrDefault(key, null);
        if (ret != null) {
            return ret;
        }

        if (pbDesc == null) {
            ret = DataDstWriterNode.getDefaultMessageDescriptor(type, typeLimit);
        } else {
            DataDstWriterNode.SPECIAL_MESSAGE_TYPE smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.NONE;
            if (pbDesc.getOptions().getMapEntry()) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.MAP;
            } else if (pbDesc.getFullName().equals(Timestamp.getDescriptor().getFullName())) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT;
            } else if (pbDesc.getFullName().equals(Duration.getDescriptor().getFullName())) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION;
            }
            ret = new DataDstTypeDescriptor(type, pbDesc.getFile().getPackage(), pbDesc.getName(), pbDesc, smt,
                    typeLimit);
        }
        pbs.dataDstDescs.put(key, ret);

        if (null == pbDesc) {
            return ret;
        }

        // extensions
        if (pbDesc.getOptions().hasExtension(Xresloader.msgDescription)) {
            ret.mutableExtension().description = pbDesc.getOptions().getExtension(Xresloader.msgDescription);
        }

        if (pbDesc.getOptions().hasExtension(Xresloader.msgSeparator)) {
            ret.mutableExtension().plainSeparator = pbDesc.getOptions().getExtension(Xresloader.msgSeparator);
        }

        // extensions for UE
        if (pbDesc.getOptions().hasExtension(XresloaderUe.helper)) {
            ret.mutableExtension().mutableUE().helper = pbDesc.getOptions().getExtension(XresloaderUe.helper);
        }

        if (pbDesc.getOptions().hasExtension(XresloaderUe.notDataTable)) {
            ret.mutableExtension().mutableUE().notDataTable = pbDesc.getOptions()
                    .getExtension(XresloaderUe.notDataTable);
        }

        if (pbDesc.getOptions().hasExtension(XresloaderUe.defaultLoader)) {
            XresloaderUe.loader_mode mode = pbDesc.getOptions().getExtension(XresloaderUe.defaultLoader);
            if (mode == XresloaderUe.loader_mode.EN_LOADER_MODE_ENABLE) {
                ret.mutableExtension().mutableUE().enableDefaultLoader = true;
            } else if (mode == XresloaderUe.loader_mode.EN_LOADER_MODE_DISABLE) {
                ret.mutableExtension().mutableUE().enableDefaultLoader = false;
            }
        }

        if (pbDesc.getOptions().getExtensionCount(XresloaderUe.includeHeader) > 0) {
            ret.mutableExtension().mutableUE().includeHeader = pbDesc.getOptions()
                    .getExtension(XresloaderUe.includeHeader);
        }

        buildDataDstDescriptorMessage(pbs, pbDesc, ret);
        return ret;
    }

    static private DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, DataDstWriterNode.JAVA_TYPE type,
            DataDstWriterNode.SPECIAL_TYPE_LIMIT typeLimit,
            int listIndex) throws ConvException {
        return DataDstWriterNode.create(null, mutableDataDstDescriptor(pbs, null, type, typeLimit), listIndex);
    }

    static private DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type, DataDstWriterNode.SPECIAL_TYPE_LIMIT typeLimit, int listIndex)
            throws ConvException {
        if (null == pbDesc) {
            return createMessageWriterNode(pbs, type, typeLimit, listIndex);
        }

        return DataDstWriterNode.create(pbDesc, mutableDataDstDescriptor(pbs, pbDesc, type, typeLimit), listIndex);
    }

    static private DataDstWriterNode createOneofWriterNode(PbInfoSet pbs, DataDstOneofDescriptor oneofDesc) {
        if (null == oneofDesc) {
            return null;
        }

        // DataDstWriterNode.JAVA_TYPE.ONEOF
        return DataDstWriterNode.create(oneofDesc.getRawDescriptor(), oneofDesc);
    }

    @Override
    public final DataDstWriterNode compile() throws ConvException {
        DataDstWriterNode ret = createMessageWriterNode(cachePbs, currentMsgDesc, DataDstWriterNode.JAVA_TYPE.MESSAGE,
                DataDstWriterNode.SPECIAL_TYPE_LIMIT.NONE,
                -1);
        if (test(ret, new LinkedList<String>())) {
            return ret;
        }

        throw new ConvException(String.format(
                "Protocol %s compile mapping relationship failed, maybe can not find any message's field in KeyRow",
                name()));
    }

    @Override
    public final byte[] build(DataDstImpl src) throws ConvException {
        // 初始化header
        PbHeaderV3.xresloader_datablocks.Builder blocks = PbHeaderV3.xresloader_datablocks.newBuilder();
        PbHeaderV3.xresloader_header.Builder header = blocks.getHeaderBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getDataVersion());
        header.setHashCode("");
        ArrayList<String> descriptionList = new ArrayList<String>();

        // 校验码
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            this.logErrorMessage("failed to find sha-256 algorithm.");
            header.setHashCode("");
        }

        DataTableContext tableContext = new DataTableContext();
        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().nextTable()) {
            // 生成描述集
            DataDstWriterNode desc = src.compile();
            int previousRowNum = blocks.getDataBlockCount();

            int tolerateContinueEmptyRows = ProgramOptions.getInstance().tolerateContinueEmptyRows;
            int currentContinueEmptyRows = 0;
            while (DataSrcImpl.getOurInstance().nextRow()) {
                DataRowContext rowContext = new DataRowContext(DataSrcImpl.getOurInstance().getCurrentFileName(),
                        DataSrcImpl.getOurInstance().getCurrentTableName(),
                        DataSrcImpl.getOurInstance().getCurrentRowNum());

                ByteString data = convData(desc, tableContext, rowContext);
                // Empty ByteString is allowed because maybe all fields are default value.
                if (null != data && !rowContext.shouldIgnore()) {
                    ++count;
                    blocks.addDataBlock(data);

                    if (null != sha256) {
                        sha256.update(data.toByteArray());
                    }

                    tableContext.addUniqueCache(rowContext);
                    currentContinueEmptyRows = 0;
                } else {
                    currentContinueEmptyRows++;
                    if (currentContinueEmptyRows > tolerateContinueEmptyRows) {
                        throw new ConvException(String.format(
                                "Too many empty row detected, maybe some cells in file \"%s\" , sheet \"%s\" is set by mistake.Or you can use --tolerate-max-empty-rows to change the bound if it's not a mistake.",
                                DataSrcImpl.getOurInstance().getCurrentFileName(),
                                DataSrcImpl.getOurInstance().getCurrentTableName()));
                    }
                }
            }

            if (desc.getMessageExtension().description != null) {
                descriptionList.add(desc.getMessageExtension().description);
            }

            blocks.setDataMessageType(desc.getFullName());

            PbHeaderV3.xresloader_data_source.Builder data_source = header.addDataSourceBuilder();
            data_source.setFile(DataSrcImpl.getOurInstance().getCurrentFileName());
            data_source.setSheet(DataSrcImpl.getOurInstance().getCurrentTableName());
            data_source.setCount(blocks.getDataBlockCount() - previousRowNum);
        }

        String validateResult = tableContext.checkUnique();
        if (validateResult != null && !validateResult.isEmpty()) {
            throw new ConvException(validateResult);
        }

        header.setCount(count);
        if (null != sha256) {
            header.setHashCode("sha256:" + Hex.encodeHexString(sha256.digest()));
        }

        if (!descriptionList.isEmpty()) {
            header.setDescription(String.join(getSystemEndl(), descriptionList));
        }

        // 写出
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        try {
            blocks.build().writeTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
            this.logErrorMessage("try to serialize protobuf data failed. %s", e.getMessage());
            ProgramOptions.getLoger().error("%s", blocks.getInitializationErrorString());
        }
        return writer.toByteArray();
    }

    static DataDstWriterNode.JAVA_TYPE pbTypeToInnerType(Descriptors.FieldDescriptor.Type t) {
        switch (t) {
            case DOUBLE:
                return DataDstWriterNode.JAVA_TYPE.DOUBLE;
            case FLOAT:
                return DataDstWriterNode.JAVA_TYPE.FLOAT;
            case INT32:
            case FIXED32:
            case UINT32:
            case SFIXED32:
            case SINT32:
                return DataDstWriterNode.JAVA_TYPE.INT;
            case INT64:
            case UINT64:
            case FIXED64:
            case SFIXED64:
            case SINT64:
                return DataDstWriterNode.JAVA_TYPE.LONG;
            case BOOL:
                return DataDstWriterNode.JAVA_TYPE.BOOLEAN;
            case STRING:
                return DataDstWriterNode.JAVA_TYPE.STRING;
            case GROUP:
            case BYTES:
                return DataDstWriterNode.JAVA_TYPE.BYTES;
            case MESSAGE:
                return DataDstWriterNode.JAVA_TYPE.MESSAGE;
            case ENUM:
                return DataDstWriterNode.JAVA_TYPE.INT;

            default:
                return DataDstWriterNode.JAVA_TYPE.INT;
        }
    }

    static DataDstWriterNode.SPECIAL_TYPE_LIMIT pbTypeToTypeLimit(Descriptors.FieldDescriptor.Type t) {
        switch (t) {
            case INT32:
            case FIXED32:
            case SFIXED32:
            case SINT32:
                return DataDstWriterNode.SPECIAL_TYPE_LIMIT.INT32;
            case UINT32:
                return DataDstWriterNode.SPECIAL_TYPE_LIMIT.UINT32;
            default:
                return DataDstWriterNode.SPECIAL_TYPE_LIMIT.NONE;
        }
    }

    private void filterMissingFields(LinkedList<String> missingFields, HashMap<String, String> oneofField,
            Descriptors.FieldDescriptor fd, boolean isMissing) throws ConvException {
        if (missingFields == null && oneofField == null) {
            return;
        }

        Descriptors.OneofDescriptor oneof = fd.getContainingOneof();
        if (isMissing && oneof == null && missingFields != null) {
            missingFields.push(fd.getName());
        }

        if (!isMissing && oneof != null && oneofField.containsKey(oneof.getFullName())) {
            String old_field = oneofField.get(oneof.getFullName());
            if (old_field != null) {
                this.logErrorMessage(
                        "Field \"%s\" in oneof descriptor \"%s\" already exists, can not add another field \"%s\" with the same oneof descriptor",
                        old_field, oneof.getFullName(), fd.getName());
                throw new ConvException(getLastErrorMessage());
            }
            oneofField.replace(oneof.getFullName(), fd.getName());
        }
    }

    /**
     * 测试并生成数据结构
     *
     * @param node      待填充的节点
     * @param name_list 当前命名列表
     * @return 查找到对应的数据源映射关系并非空则返回true，否则返回false
     */
    private boolean test(DataDstWriterNode node, LinkedList<String> name_list) throws ConvException {
        return testMessage(node, name_list);
    }

    private boolean testMessage(DataDstWriterNode node, LinkedList<String> name_list) throws ConvException {
        String prefix = String.join(".", name_list);
        boolean ret = false;
        Descriptors.Descriptor desc = (Descriptors.Descriptor) node.privateData;
        if (null == desc) {
            return ret;
        }

        LinkedList<String> missingFields = null;
        HashMap<String, String> oneofField = new HashMap<String, String>();

        boolean require_mapping_all_fields = ProgramOptions.getInstance().requireMappingAllFields
                || (desc.getOptions().hasExtension(Xresloader.msgRequireMappingAll)
                        && desc.getOptions().getExtension(Xresloader.msgRequireMappingAll));
        if (require_mapping_all_fields) {
            missingFields = new LinkedList<String>();
        }

        for (Descriptors.OneofDescriptor oneof : desc.getOneofs()) {
            oneofField.put(oneof.getFullName(), null);
        }
        boolean enable_alias_mapping = ProgramOptions.getInstance().enableAliasMapping;

        DataSrcImpl data_src = DataSrcImpl.getOurInstance();
        for (Descriptors.FieldDescriptor fd : desc.getFields()) {
            DataDstChildrenNode child = null;
            ArrayList<String> field_alias = null;
            if (enable_alias_mapping && fd.getOptions().getExtensionCount(Xresloader.fieldAlias) > 0) {
                field_alias = new ArrayList<>();
                field_alias.ensureCapacity(fd.getOptions().getExtensionCount(Xresloader.fieldAlias));
                for (String alias_name : fd.getOptions().getExtension(Xresloader.fieldAlias)) {
                    String alias_name_striped = alias_name.strip();
                    if (!alias_name_striped.isEmpty()) {
                        field_alias.add(alias_name_striped);
                    }
                }
            }
            switch (fd.getType()) {
                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;
                        name_list.addLast("");
                        for (;; ++count) {
                            DataDstWriterNode c = createMessageWriterNode(cachePbs, fd.getMessageType(),
                                    DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd.getType()), count);
                            boolean test_passed = false;

                            name_list.removeLast();
                            // 检测使用的名字，message不允许混合别名。以防别名组合指数级膨胀
                            String select_name = "";
                            if (data_src.containsIdentifyMappingPrefix(
                                    DataDstWriterNode.makeChildPath(prefix, fd.getName(),
                                            count))) {
                                select_name = fd.getName();
                            } else if (null != field_alias) {
                                for (String alias_name : field_alias) {
                                    String test_field_name = alias_name.strip();
                                    if (test_field_name.isEmpty()) {
                                        continue;
                                    }

                                    if (data_src.containsIdentifyMappingPrefix(
                                            DataDstWriterNode.makeChildPath(prefix, test_field_name,
                                                    count))) {
                                        select_name = test_field_name;
                                        break;
                                    }
                                }
                            }
                            name_list.addLast(DataDstWriterNode.makeNodeName(select_name, count));
                            if (!select_name.isEmpty() && test(c, name_list)) {
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                ret = true;
                                test_passed = true;
                            }

                            if (!test_passed) {
                                // try plain mode - array item
                                String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                                IdentifyDescriptor col = data_src.getColumnByName(real_name);
                                if (null == col && null != field_alias) {
                                    for (String alias_name : field_alias) {
                                        String test_field_name = alias_name.strip();
                                        if (test_field_name.isEmpty()) {
                                            continue;
                                        }
                                        String alias_full_name = DataDstWriterNode.makeChildPath(prefix,
                                                test_field_name,
                                                count);
                                        col = data_src.getColumnByName(alias_full_name);
                                        if (col != null) {
                                            break;
                                        }
                                    }
                                }

                                if (null != col) {
                                    child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                    setup_node_identify(c, child, col, fd);
                                    ret = true;
                                } else {
                                    break;
                                }
                            }
                        }
                        name_list.removeLast();

                        if (count > 0) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            if (child != null && child.innerFieldDesc != null) {
                                int minSize = child.innerFieldDesc.mutableExtension().mutableList().minSize;
                                int maxSize = child.innerFieldDesc.mutableExtension().mutableList().maxSize;
                                if (minSize > 0 && child.nodes.size() < minSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at least %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), minSize, child.nodes.size()));
                                }
                                if (maxSize > 0 && child.nodes.size() > maxSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at most %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), maxSize, child.nodes.size()));
                                }
                            }
                        } else {
                            // try plain mode - the whole array
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);

                            if (null == col && null != field_alias) {
                                for (String alias_name : field_alias) {
                                    String test_field_name = alias_name.strip();
                                    if (test_field_name.isEmpty()) {
                                        continue;
                                    }
                                    String alias_full_name = DataDstWriterNode.makeChildPath(prefix,
                                            test_field_name);
                                    col = data_src.getColumnByName(alias_full_name);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, fd.getMessageType(),
                                        DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd.getType()), -1);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setup_node_identify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);
                            }
                        }
                    } else {
                        DataDstWriterNode c = createMessageWriterNode(cachePbs, fd.getMessageType(),
                                DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd.getType()), -1);
                        boolean test_passed = false;

                        // 检测使用的名字，message不允许混合别名。以防别名组合指数级膨胀
                        String select_name = "";
                        if (data_src.containsIdentifyMappingPrefix(
                                DataDstWriterNode.makeChildPath(prefix, fd.getName()))) {
                            select_name = fd.getName();
                        } else if (null != field_alias) {
                            for (String alias_name : field_alias) {
                                String test_field_name = alias_name.strip();
                                if (test_field_name.isEmpty()) {
                                    continue;
                                }

                                if (data_src.containsIdentifyMappingPrefix(
                                        DataDstWriterNode.makeChildPath(prefix, test_field_name))) {
                                    select_name = test_field_name;
                                    break;
                                }
                            }
                        }
                        name_list.addLast(DataDstWriterNode.makeNodeName(select_name));
                        if (!select_name.isEmpty() && test(c, name_list)) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            ret = true;
                            test_passed = true;
                        }

                        if (!test_passed) {
                            // try plain mode
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && null != field_alias) {
                                for (String alias_name : field_alias) {
                                    String test_field_name = alias_name.strip();
                                    if (test_field_name.isEmpty()) {
                                        continue;
                                    }
                                    String alias_full_name = DataDstWriterNode.makeChildPath(prefix, test_field_name);
                                    col = data_src.getColumnByName(alias_full_name);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setup_node_identify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);

                                if (checkFieldIsRequired(fd)) {
                                    // required 字段要dump默认数据
                                    child = node.addChild(fd.getName(), c, fd,
                                            DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                }
                            }
                        }
                        name_list.removeLast();
                    }
                    break;
                default: {
                    // list 类型
                    DataDstWriterNode.JAVA_TYPE inner_type = pbTypeToInnerType(fd.getType());
                    if (fd.isRepeated()) {
                        int count = 0;
                        for (;; ++count) {
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && null != field_alias) {
                                for (String alias_name : field_alias) {
                                    String test_field_name = alias_name.strip();
                                    if (test_field_name.isEmpty()) {
                                        continue;
                                    }
                                    String alias_full_name = DataDstWriterNode.makeChildPath(prefix, test_field_name,
                                            count);
                                    col = data_src.getColumnByName(alias_full_name);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type,
                                        pbTypeToTypeLimit(fd.getType()), count);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                setup_node_identify(c, child, col, fd);
                                ret = true;
                            } else {
                                break;
                            }
                        }

                        if (count > 0) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            if (child != null && child.innerFieldDesc != null) {
                                int minSize = child.innerFieldDesc.mutableExtension().mutableList().minSize;
                                int maxSize = child.innerFieldDesc.mutableExtension().mutableList().maxSize;
                                if (minSize > 0 && child.nodes.size() < minSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at least %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), minSize, child.nodes.size()));
                                }
                                if (maxSize > 0 && child.nodes.size() > maxSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at most %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), maxSize, child.nodes.size()));
                                }
                            }
                        } else {
                            // try plain mode
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && null != field_alias) {
                                for (String alias_name : field_alias) {
                                    String test_field_name = alias_name.strip();
                                    if (test_field_name.isEmpty()) {
                                        continue;
                                    }
                                    String alias_full_name = DataDstWriterNode.makeChildPath(prefix, test_field_name);
                                    col = data_src.getColumnByName(alias_full_name);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type,
                                        pbTypeToTypeLimit(fd.getType()), -1);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setup_node_identify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);
                            }
                        }
                    } else {
                        // 非 list 类型
                        String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                        IdentifyDescriptor col = data_src.getColumnByName(real_name);
                        if (null == col && null != field_alias) {
                            for (String alias_name : field_alias) {
                                String test_field_name = alias_name.strip();
                                if (test_field_name.isEmpty()) {
                                    continue;
                                }
                                String alias_full_name = DataDstWriterNode.makeChildPath(prefix, test_field_name);
                                col = data_src.getColumnByName(alias_full_name);
                                if (null != col) {
                                    break;
                                }
                            }
                        }

                        if (null != col) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type,
                                    pbTypeToTypeLimit(fd.getType()), -1);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            setup_node_identify(c, child, col, fd);
                            ret = true;
                        } else {
                            filterMissingFields(missingFields, oneofField, fd, true);
                            if (checkFieldIsRequired(fd)) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type,
                                        pbTypeToTypeLimit(fd.getType()), -1);
                                // required 字段要dump默认数据
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 索引oneof
        for (Descriptors.OneofDescriptor fd : desc.getOneofs()) {
            if (node.getTypeDescriptor() == null) {
                this.logErrorMessage(
                        "Type descriptor \"%s\" not found, it's probably a BUG, please report to %s, current version: %s",
                        node.getFullName(), ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstOneofDescriptor oneof_inner_desc = node.getTypeDescriptor().oneofs.getOrDefault(fd.getName(), null);
            if (oneof_inner_desc == null) {
                this.logErrorMessage(
                        "Oneof descriptor \"%s\" not found in type descriptor \"%s\", it's probably a BUG, please report to %s, current version: %s",
                        fd.getFullName(), node.getFullName(), ProgramOptions.getReportUrl(),
                        ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstChildrenNode child = null;
            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
            IdentifyDescriptor col = data_src.getColumnByName(real_name);

            if (null == col) {
                continue;
            }

            String old_field = oneofField.getOrDefault(fd.getFullName(), null);
            if (old_field != null) {
                this.logErrorMessage(
                        "Field \"%s\" in oneof descriptor \"%s\" already exists, can not add the oneof writer again",
                        old_field, fd.getFullName());
                throw new ConvException(getLastErrorMessage());
            }

            oneofField.replace(fd.getFullName(), fd.getName());

            DataDstWriterNode c = createOneofWriterNode(cachePbs, oneof_inner_desc);
            // oneof field must be a plain field
            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
            setup_node_identify(c, child, col, fd);
            ret = true;
        }

        if (require_mapping_all_fields) {
            String missingFieldDesc = "";
            if (missingFields != null && !missingFields.isEmpty()) {
                missingFieldDesc = String.format(" fields %s", String.join(",", missingFields));
            }

            String missingOneofDesc = "";
            ArrayList<String> missingOneofs = new ArrayList<String>();
            missingOneofs.ensureCapacity(oneofField.size());
            for (Map.Entry<String, String> oneofEntry : oneofField.entrySet()) {
                if (oneofEntry.getValue() == null) {
                    missingOneofs.add(oneofEntry.getKey());
                }
            }
            if (!missingOneofs.isEmpty()) {
                missingOneofDesc = String.format(" oneof %s", String.join(",", missingOneofs));
            }

            if (!missingFieldDesc.isEmpty() || !missingOneofDesc.isEmpty()) {
                if (prefix.isEmpty()) {
                    this.logErrorMessage("Message %s can not find%s%s in data source", desc.getFullName(),
                            missingFieldDesc, missingOneofDesc);
                } else {
                    this.logErrorMessage("Message %s in %s can not find%s%s in data source", desc.getFullName(), prefix,
                            missingFieldDesc, missingOneofDesc);
                }
                throw new ConvException(getLastErrorMessage());
            }
        }

        return ret;
    }

    private ByteString convData(DataDstWriterNode node, DataTableContext tableContext, DataRowContext rowContext)
            throws ConvException {
        // Descriptors.Descriptor msg_desc = (Descriptors.Descriptor) node.privateData;
        DynamicMessage.Builder root = null;
        boolean valid_data = false;
        if (SchemeConf.getInstance().getCallbackScriptPath().isEmpty()) {
            root = DynamicMessage.newBuilder(currentMsgDesc);
            valid_data = dumpMessage(root, node, rowContext, currentMsgDesc.getName());
        } else {
            root = DataETProcessor.getInstance().dumpPbMessage(currentMsgDesc, node, rowContext,
                    currentMsgDesc.getName());
            valid_data = (root != null);
        }
        // 过滤空项
        if (!valid_data || root == null) {
            return null;
        }

        if (rowContext.shouldIgnore()) {
            ProgramOptions.getLoger().warn(
                    "File: %s, Sheet: %s, Row: %d%s",
                    rowContext.fileName, rowContext.tableName, rowContext.row,
                    rowContext.buildIgnoreIgnoreMessage(4));

            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            this.logErrorMessage("Serialize failed. %s", e.getMessage());
            return null;
        }
    }

    private Object getDefault(DynamicMessage.Builder builder, DataDstFieldDescriptor field, int listIndex)
            throws ConvException {
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        Object val = null;
        if (field.isList() && field.getListExtension() != null
                && field.getListExtension().strictSize && field.getListExtension().minSize >= listIndex + 1) {
            this.logErrorMessage(
                    "Field \"%s\" in \"%s\" has set field_list_min_size %d, which is not allowed to be auto filled with default value.",
                    field.getName(), fd.getContainingType().getFullName(),
                    field.getListExtension().minSize);
            throw new ConvException(getLastErrorMessage());
        }
        switch (fd.getType()) {
            case DOUBLE:
                val = Double.valueOf(0.0);
                break;
            case FLOAT:
                val = Float.valueOf(0);
                break;
            case INT32:
            case FIXED32:
            case UINT32:
            case SFIXED32:
            case SINT32:
                val = Integer.valueOf(0);
                break;
            case INT64:
            case UINT64:
            case FIXED64:
            case SFIXED64:
            case SINT64:
                val = Long.valueOf(0);
                break;
            case ENUM:
                val = fd.getEnumType().findValueByNumber(0);
                break;
            case BOOL:
                val = false;
                break;
            case STRING:
                val = "";
                break;
            case GROUP:
                val = new byte[0];
                break;
            case MESSAGE: {
                if (null != field.getTypeDescriptor()) {
                    DynamicMessage.Builder subnode = DynamicMessage.newBuilder(fd.getMessageType());

                    // 仅仅Required需要导出默认值
                    for (DataDstFieldDescriptor subField : field.getTypeDescriptor().getSortedFields()) {
                        if (subField.isRequired()) {
                            dumpDefault(subnode, subField, 0);
                        } else if (subField.isList() && subField.getListExtension() != null
                                && subField.getListExtension().strictSize && subField.getListExtension().minSize > 0) {
                            this.logErrorMessage(
                                    "Field \"%s\" in \"%s\" has set field_list_min_size %d, which is not allowed to be auto filled with default value.",
                                    subField.getName(), fd.getFullName(),
                                    subField.getListExtension().minSize);
                            throw new ConvException(getLastErrorMessage());
                        }
                    }

                    val = subnode.build();
                }
                break;
            }
            case BYTES:
                val = new byte[0];
                break;
        }
        return val;
    }

    private Object getValueFromDataSource(DataDstWriterNode desc, DataDstFieldDescriptor field,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        Object val = null;
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();

        switch (fd.getJavaType()) {
            case INT: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
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
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
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
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
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
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
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
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
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
                    String validateErrorMessage = field.validateTypeLimit(ret.value);
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = ret.value;
                }
                break;
            }

            case BYTE_STRING: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != res && res.valid) {
                    String validateErrorMessage = field.validateTypeLimit(res.value);
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
            case ENUM: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    String validateErrorMessage = field.validateTypeLimit(ret.value.intValue());
                    if (null != validateErrorMessage) {
                        throw new ConvException(validateErrorMessage);
                    }

                    val = fd.getEnumType().findValueByNumber(ret.value.intValue());
                }

                break;
            }

            case MESSAGE: {
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                if (dumpMessage(node, desc, rowContext, fieldPath) || checkFieldIsRequired(fd)) {
                    try {
                        val = node.build();
                    } catch (UninitializedMessageException e) {
                        this.logErrorMessage("serialize %s(%s) failed. %s", fd.getFullName(),
                                fd.getMessageType().getName(), e.getMessage());
                    }
                }
                break;
            }

            default:
                break;
        }
        return val;
    }

    private void dumpValue(DynamicMessage.Builder builder, DataDstFieldDescriptor field, Object val, int index,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        DataDstWriterNode.ListStripRule stripListRule = field.getListStripRule();
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();

        if (fd.isRepeated()
                && (stripListRule == DataDstWriterNode.ListStripRule.STRIP_NOTHING
                        || stripListRule == DataDstWriterNode.ListStripRule.STRIP_TAIL)) {
            int fill_default_size = builder.getRepeatedFieldCount(fd);
            for (int i = fill_default_size; i < index; ++i) {
                Object defaultVal = getDefault(builder, field, i);
                if (defaultVal != null) {
                    builder.addRepeatedField(fd, defaultVal);
                }
            }
        }

        if (JavaType.ENUM == fd.getJavaType()) {
            Descriptors.EnumValueDescriptor enum_val = null;
            if (val instanceof Descriptors.EnumValueDescriptor) {
                enum_val = (Descriptors.EnumValueDescriptor) val;
            } else {
                enum_val = get_enum_value(cachePbs, fd.getEnumType(), (Integer) val);
            }

            if (null == enum_val) {
                return;
            }

            if (fd.isRepeated()) {
                if (index > 0 && builder.getRepeatedFieldCount(fd) > index) {
                    builder.setRepeatedField(fd, index, enum_val);
                } else {
                    builder.addRepeatedField(fd, enum_val);
                }

                if (null != rowContext && null != field.getUniqueTags()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, String.format("%s.%s", fieldPath, index),
                                enum_val.getNumber());
                    }
                }
            } else {
                builder.setField(fd, enum_val);

                if (null != rowContext && null != field.getUniqueTags()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath,
                                enum_val.getNumber());
                    }
                }
            }
        } else {
            if (fd.isRepeated()) {
                if (index > 0 && builder.getRepeatedFieldCount(fd) > index) {
                    builder.setRepeatedField(fd, index, val);
                } else {
                    builder.addRepeatedField(fd, val);
                }

                if (null != rowContext && null != field.getUniqueTags() && JavaType.MESSAGE != fd.getJavaType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, String.format("%s.%s", fieldPath, index),
                                val);
                    }
                }
            } else {
                builder.setField(fd, val);

                if (null != rowContext && null != field.getUniqueTags() && JavaType.MESSAGE != fd.getJavaType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, val);
                    }
                }
            }
        }
    }

    private void dumpDefault(DynamicMessage.Builder builder, DataDstOneofDescriptor oneof) {
        builder.clearOneof((Descriptors.OneofDescriptor) oneof.getRawDescriptor());
    }

    private void dumpDefault(DynamicMessage.Builder builder, DataDstFieldDescriptor field, int index)
            throws ConvException {
        Object val = getDefault(builder, field, index);
        if (val != null) {
            dumpValue(builder, field, val, index, null, "");
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
    private boolean dumpMessage(DynamicMessage.Builder builder, DataDstWriterNode node, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            if (c.getValue().isOneof()) {
                // dump oneof data
                boolean fieldHasValue = false;
                String subFieldPath = String.format("%s.%s", fieldPath, c.getKey());
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), child, rowContext,
                            fieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }
                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("oneof %s is empty but set not null, we will ignore this row",
                                    subFieldPath));
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                boolean fieldHasValue = false;

                if (null == c.getValue().innerFieldDesc) {
                    // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                    continue;
                }

                for (int i = 0; i < c.getValue().nodes.size(); i++) {
                    DataDstWriterNode child = c.getValue().nodes.get(i);
                    String subFieldPath;
                    if (c.getValue().isList()) {
                        subFieldPath = String.format("%s.%s.%d", fieldPath, c.getKey(), i);
                    } else {
                        subFieldPath = String.format("%s.%s", fieldPath, c.getKey());
                    }

                    if (dumpStandardField(builder, child, c.getValue().innerFieldDesc, rowContext, subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }

                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
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

                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("Field %s is empty but set not null, we will ignore this row",
                                    subFieldPath));
                }
            }

            // Check list size
            if (c.getValue().isList() && c.getValue().innerFieldDesc != null) {
                var listExt = c.getValue().innerFieldDesc.getListExtension();
                if (listExt != null && c.getValue().innerFieldDesc != null) {
                    Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) c.getValue().innerFieldDesc
                            .getRawDescriptor();
                    if (listExt.minSize > 0 && listExt.minSize > builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at least %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.minSize, builder.getRepeatedFieldCount(fd)));
                    }
                    if (listExt.maxSize > 0 && listExt.maxSize < builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at most %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.maxSize, builder.getRepeatedFieldCount(fd)));
                    }
                }
            }
        }

        return ret;
    }

    private boolean dumpStandardField(DynamicMessage.Builder builder, DataDstWriterNode desc,
            DataDstFieldDescriptor field, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (null == desc.identify && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
            // required 空字段填充默认值
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, desc.getListIndex());
            }

            return false;
        }

        Object val = getValueFromDataSource(desc, field, rowContext, fieldPath);

        if (null == val) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, desc.getListIndex());
            }

            return false;
        }

        dumpValue(builder, field, val, desc.getListIndex(), rowContext, fieldPath);
        return true;
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (field == null) {
            return false;
        }

        if (null == ident) {
            if (DataDstWriterNode.getDefaultListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
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
                return false;
            }

            sub_field = (DataDstWriterNode.DataDstFieldDescriptor) res[0];
            if (sub_field == null) {
                break;
            }

            if (res.length == 1) {
                dumpDefault(builder, sub_field, 0);
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

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (null == ident) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, 0);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, 0);
            }
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input,
            DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return false;
        }

        if ((null != maybeFromNode && null != maybeFromNode.identify && !field.isList())
                && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }
        if (field.isList()) {
            String[] groups;
            if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                groups = new String[] { input.trim() };
            } else {
                groups = splitPlainGroups(input.trim(), getPlainFieldSeparator(field));
            }
            Object parsedDatas = null;

            boolean ret = true;
            switch (field.getType()) {
                case INT: {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<Object>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        for (Long v : values) {
                            if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                                Descriptors.EnumValueDescriptor enum_val = get_enum_value(cachePbs, fd.getEnumType(),
                                        v.intValue());
                                if (enum_val != null) {
                                    tmp.add(enum_val);
                                }
                            } else {
                                String validateErrorMessage = field.validateTypeLimit(v);
                                if (null != validateErrorMessage) {
                                    throw new ConvException(validateErrorMessage);
                                }
                                tmp.add(v.intValue());
                            }
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
                    ArrayList<DynamicMessage> tmp = new ArrayList<DynamicMessage>();
                    tmp.ensureCapacity(groups.length);
                    DataDstFieldDescriptor referOriginField = null;
                    Descriptors.FieldDescriptor referOriginFd = null;
                    if (null != field.getReferOriginField()) {
                        referOriginField = field
                                .getReferOriginField();
                        referOriginFd = (Descriptors.FieldDescriptor) referOriginField.getRawDescriptor();
                    }

                    for (int i = 0; i < groups.length; ++i) {
                        String v = groups[i];
                        String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                        String subFieldPath = String.format("%s.%d", fieldPath, i);
                        ParseResult res = parsePlainDataMessage(subGroups, ident, field, rowContext,
                                subFieldPath);
                        if (res != null && res.value != null) {
                            tmp.add(res.value);
                            if (res.origin != null && referOriginField != null) {
                                while (builder.getRepeatedFieldCount(referOriginFd) < i) {
                                    builder.addRepeatedField(referOriginFd, "");
                                }
                                dumpValue(builder, referOriginField, res.origin, i, rowContext, subFieldPath);
                            }
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                    break;
                }

                default:
                    // oneof can not be repeated
                    break;
            }

            if (parsedDatas != null) {
                if (!(parsedDatas instanceof ArrayList<?>)) {
                    return false;
                }
                ArrayList<?> values = (ArrayList<?>) parsedDatas;

                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (values.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s.%d failed, too many element(s)(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), values.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    DataDstWriterNode.ListStripRule stripListRule = field.getListStripRule();
                    if (stripListRule == DataDstWriterNode.ListStripRule.STRIP_NOTHING
                            || stripListRule == DataDstWriterNode.ListStripRule.STRIP_TAIL) {
                        while (builder.getRepeatedFieldCount(fd) < index) {
                            builder.addRepeatedField(fd, getDefault(builder, field, builder.getRepeatedFieldCount(fd)));
                        }
                    }

                    if (index >= 0 && builder.getRepeatedFieldCount(fd) > index) {
                        builder.setRepeatedField(fd, index, values.get(0));
                    } else {
                        builder.addRepeatedField(fd, values.get(0));
                    }
                } else {
                    for (int i = 0; i < values.size(); ++i) {
                        dumpValue(builder, field, values.get(i), i, rowContext, String.format("%s.%d", fieldPath, i));
                    }
                }

                if (null != rowContext && null != field.getUniqueTags()
                        && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType() && !values.isEmpty()) {
                    // Convert EnumValueDescriptor into Integer
                    if (values.get(0) instanceof Descriptors.EnumValueDescriptor) {
                        ArrayList<Integer> enumValues = new ArrayList<>();
                        enumValues.ensureCapacity(values.size());
                        for (Object valObj : values) {
                            enumValues.add(((Descriptors.EnumValueDescriptor) valObj).getNumber());
                        }

                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, fieldPath, enumValues);
                        }
                    } else {
                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, fieldPath, values);
                        }
                    }
                }
            } else if (field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    int index = maybeFromNode.getListIndex();
                    while (builder.getRepeatedFieldCount(fd) <= index) {
                        builder.addRepeatedField(fd, getDefault(builder, field, builder.getRepeatedFieldCount(fd)));
                    }
                }
            } else {
                ret = false;
            }

            // Check list size
            {
                var listExt = field.getListExtension();
                if (listExt != null) {
                    if (listExt.minSize > 0 && listExt.minSize > builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at least %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.minSize, builder.getRepeatedFieldCount(fd)));
                    }
                    if (listExt.maxSize > 0 && listExt.maxSize < builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at most %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.maxSize, builder.getRepeatedFieldCount(fd)));
                    }
                }
            }

            return ret;
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
                            dumpValue(builder,
                                    field.getReferOriginField(),
                                    res.origin, 0, rowContext, fieldPath);
                        }
                    } else if (field.isRequired()) {
                        dumpDefault(builder, field, 0);
                    }
                    break;
                }

                default:
                    break;
            }

            if (val == null) {
                return false;
            }

            if (fd.isRepeated() && val instanceof ArrayList<?>) {
                ArrayList<?> values = (ArrayList<?>) val;
                for (int i = 0; i < values.size(); ++i) {
                    String subFieldPath = String.format("%s.%d", fieldPath, i);

                    dumpValue(builder, field, values.get(i), i, rowContext, subFieldPath);
                    if (null != rowContext && null != field.getUniqueTags()
                            && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, subFieldPath, values.get(i));
                        }
                    }
                }
            } else {
                dumpValue(builder, field, val, 0, rowContext, fieldPath);

                if (null != rowContext && null != field.getUniqueTags()
                        && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, val);
                    }
                }
            }

            return true;
        }
    }

    public ParseResult parsePlainDataMessage(String[] inputs, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd || fd.getJavaType() != MESSAGE) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return null;
        }

        ArrayList<DataDstWriterNode.DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(fd.getMessageType());
        ParseResult ret = new ParseResult();

        // 几种特殊模式
        if (inputs.length == 1) {
            if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDatetime(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    Descriptors.FieldDescriptor subfd = (Descriptors.FieldDescriptor) children.get(i)
                            .getRawDescriptor();
                    if (subfd.getName().equalsIgnoreCase("seconds")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.toEpochMilli() / 1000);
                    } else if (subfd.getName().equalsIgnoreCase("nanos")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.getNano());
                    }
                }

                ret.value = builder.build();
                ret.origin = inputs[0];
                return ret;
            } else if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDuration(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    Descriptors.FieldDescriptor subfd = (Descriptors.FieldDescriptor) children.get(i)
                            .getRawDescriptor();
                    if (subfd.getName().equalsIgnoreCase("seconds")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.toEpochMilli() / 1000);
                    } else if (subfd.getName().equalsIgnoreCase("nanos")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.getNano());
                    }
                }
                ret.value = builder.build();
                ret.origin = inputs[0];
                return ret;
            }
        }

        boolean hasData = false;
        HashSet<String> dumpedOneof = null;
        if (field.getTypeDescriptor().getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
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

            if (child.getReferOneof() != null) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            child.getName(), field.getTypeDescriptor().getFullName()));
                }
                if (dumpedOneof.contains(child.getReferOneof().getFullName())) {
                    continue;
                }

                if (usedInputIdx < inputs.length
                        && dumpPlainField(builder, null, child.getReferOneof(), null, inputs[usedInputIdx],
                                rowContext, fieldPath)) {
                    hasData = true;
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

                ++usedInputIdx;
                ++fieldSize;
            } else {
                String subFieldPath = String.format("%s.%s", fieldPath, child.getName());
                if (usedInputIdx < inputs.length
                        && dumpPlainField(builder, null, child, null, inputs[usedInputIdx], rowContext,
                                subFieldPath)) {
                    hasData = true;

                    missingFields.remove(child.getIndex());
                } else {
                    if (child.isNotNull()) {
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

        if (!hasData) {
            return null;
        }

        ret.value = builder.build();
        return ret;
    }

    /**
     * 生成常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> buildConst() {
        for (String pbsFile : ProgramOptions.getInstance().protocolFile) {
            if (false == load_pb_file(cachePbs, pbsFile, true, true, null)) {
                return null;
            }
        }

        if (null == cachePbs.enums) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : cachePbs.file_descs.entrySet()) {
            if (fdp.getValue().getPackage().equals("google.protobuf")) {
                continue;
            }

            String[] names = null;
            HashMap<String, Object> fd_root = ret;

            if (false == fdp.getValue().getPackage().isEmpty()) {
                names = fdp.getValue().getPackage().split("\\.");
            }

            if (null != names) {
                for (String seg : names) {
                    if (seg.isEmpty()) {
                        continue;
                    }
                    if (fd_root.containsKey(seg)) {
                        Object node = fd_root.get(seg);
                        if (node instanceof HashMap) {
                            fd_root = (HashMap<String, Object>) node;
                        } else {
                            this.logErrorMessage("package name %s conflict(failed in %s).", fdp.getValue().getPackage(),
                                    seg);
                            break;
                        }
                    } else {
                        HashMap<String, Object> node = new HashMap<String, Object>();
                        fd_root.put(seg, node);
                        fd_root = node;
                    }
                }
            }

            // dump oneof and enum in message
            for (Descriptors.Descriptor msg_desc : fdp.getValue().getMessageTypes()) {
                DataDstPbHelper.dumpConstIntoHashMap(fdp.getValue().getPackage(), fd_root, msg_desc);
            }

            for (Descriptors.EnumDescriptor enum_desc : fdp.getValue().getEnumTypes()) {
                DataDstPbHelper.dumpConstIntoHashMap(fdp.getValue().getPackage(), fd_root, enum_desc);
            }
        }

        return ret;
    }

    /**
     * 转储常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        // protobuf的常量输出直接复制描述文件就好了
        if (ProgramOptions.getInstance().protocolFile.length == 1
                && ProgramOptions.getInstance().protocolFile[0].equals(ProgramOptions.getInstance().protoDumpFile)) {
            return null;
        }

        try {
            DescriptorProtos.FileDescriptorSet.Builder fdsBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
            for (var fdp : cachePbs.files.entrySet()) {
                if (fdp.getValue().getPackage().equals("google.protobuf")) {
                    continue;
                }

                fdsBuilder.addFile(fdp.getValue());
            }

            return fdsBuilder.build().toByteArray();
        } catch (Exception e) {
            this.logErrorMessage("Serialize FileDescriptorSet failed: %s.", e.getMessage());
        }

        return null;
    }

    /**
     * 生成选项数据
     *
     * @return 选项数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildOptions(ProgramOptions.ProtoDumpType dumpType) {
        for (String pbsFile : ProgramOptions.getInstance().protocolFile) {
            if (false == load_pb_file(cachePbs, pbsFile, true, true, null)) {
                return null;
            }
        }

        com.google.protobuf.ExtensionRegistry custom_extensions = com.google.protobuf.ExtensionRegistry.newInstance();

        Xresloader.registerAllExtensions(custom_extensions);
        XresloaderUe.registerAllExtensions(custom_extensions);

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : cachePbs.file_descs.entrySet()) {
            for (Descriptors.FieldDescriptor sub_desc : fdp.getValue().getExtensions()) {
                if (sub_desc.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                    custom_extensions.add(sub_desc, DynamicMessage.newBuilder(sub_desc.getMessageType()).build());
                } else {
                    custom_extensions.add(sub_desc);
                }
            }
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();
        LinkedList<Object> files = new LinkedList<Object>();

        ArrayList<HashMap.Entry<String, Descriptors.FileDescriptor>> sorted_array = new ArrayList<HashMap.Entry<String, Descriptors.FileDescriptor>>();
        sorted_array.ensureCapacity(cachePbs.file_descs.size());
        sorted_array.addAll(cachePbs.file_descs.entrySet());
        sorted_array.sort((l, r) -> {
            return l.getValue().getFullName().compareTo(r.getValue().getFullName());
        });

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : sorted_array) {
            if (fdp.getValue().getPackage().equals("google.protobuf")) {
                continue;
            }
            DataDstPbHelper.dumpOptionsIntoHashMap(dumpType, files, fdp.getValue(), custom_extensions);
        }

        ret.put("files", files);

        return ret;
    }
}
