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
import com.google.protobuf.DurationProto;
import com.google.protobuf.TimestampProto;
import com.google.protobuf.AnyProto;
import com.google.protobuf.ApiProto;
import com.google.protobuf.EmptyProto;
import com.google.protobuf.FieldMaskProto;
import com.google.protobuf.StructProto;
import com.google.protobuf.TypeProto;
import com.google.protobuf.WrappersProto;
import com.google.protobuf.SourceContextProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Duration;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.UninitializedMessageException;
import org.apache.commons.codec.binary.Hex;
import org.xresloader.Xresloader;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstChildrenNode;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyIntRange;
import org.xresloader.core.data.vfy.DataVerifyPbEnum;
import org.xresloader.core.data.vfy.DataVerifyPbMsgField;
import org.xresloader.core.data.vfy.DataVerifyPbOneof;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.pb.PbHeaderV3;
import org.xresloader.ue.XresloaderUe;

/**
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    static private class PbAliasNode<T> {
        public T element = null;
        LinkedList<String> names = null;
    };

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

        // ========================== 验证器 ==========================
        HashMap<String, DataVerifyImpl> identifiers = new HashMap<String, DataVerifyImpl>();

        // ========================== 内建AST类型缓存 ==========================
        HashMap<String, DataDstTypeDescriptor> dataDstDescs = new HashMap<String, DataDstTypeDescriptor>();

        public PbInfoSet() {
        }
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

        if (!short_name.isEmpty()) {
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
                    ProgramOptions.getInstance().protocolFile, e.toString());
            return false;
        } catch (IOException e) {
            ProgramOptions.getLoger().error("Parse protocol file \"%s\" failed. %s",
                    ProgramOptions.getInstance().protocolFile, e.toString());
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

    static private void setup_extension(DataDstFieldDescriptor child_field, Descriptors.FieldDescriptor fd) {
        String verifierExpr = null;
        if (fd.getOptions().hasExtension(Xresloader.verifier)) {
            verifierExpr = fd.getOptions().getExtension(Xresloader.verifier);
            child_field.mutableExtension().verifier = verifierExpr;
        }
        LinkedList<DataVerifyImpl> gen = setup_verifier(verifierExpr, fd);
        if (gen != null && !gen.isEmpty()) {
            for (DataVerifyImpl vfy : gen) {
                child_field.addVerifier(vfy);
            }
        } else {
            child_field.resetVerifier();
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldDescription)) {
            child_field.mutableExtension().description = fd.getOptions().getExtension(Xresloader.fieldDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldRatio)) {
            child_field.mutableExtension().ratio = fd.getOptions().getExtension(Xresloader.fieldRatio);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldSeparator)) {
            child_field.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.fieldSeparator);
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
    }

    static private void setup_extension(DataDstOneofDescriptor child_field, Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        LinkedList<DataVerifyImpl> gen = setup_verifier(container, fd);
        if (gen != null && !gen.isEmpty()) {
            for (DataVerifyImpl vfy : gen) {
                child_field.addVerifier(vfy);
            }
        } else {
            child_field.resetVerifier();
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofDescription)) {
            child_field.mutableExtension().description = fd.getOptions().getExtension(Xresloader.oneofDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofSeparator)) {
            child_field.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.oneofSeparator);
        }
    }

    static private LinkedList<DataVerifyImpl> setup_verifier(Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        LinkedList<DataVerifyImpl> ret = new LinkedList<DataVerifyImpl>();

        String rule = String.format("%s.%s.%s", container.getFile().getPackage(), container.getName(), fd.getName());
        if (rule.length() > 0 && rule.charAt(0) == '.') {
            rule = rule.substring(1);
        }
        {
            DataVerifyImpl vfy = cachePbs.identifiers.getOrDefault(rule, null);
            // 命中缓存
            if (null != vfy) {
                ret.add(vfy);
                return ret;
            }
        }

        DataVerifyPbOneof new_vfy = new DataVerifyPbOneof(fd);
        cachePbs.identifiers.put(rule, new_vfy);
        ret.add(new_vfy);

        return ret;
    }

    static private LinkedList<DataVerifyImpl> setup_verifier(String verifier, Descriptors.FieldDescriptor fd) {
        if (!(verifier != null && !verifier.trim().isEmpty()) && !(fd != null && fd.getJavaType() == JavaType.ENUM)) {
            return null;
        }

        LinkedList<DataVerifyImpl> ret = new LinkedList<DataVerifyImpl>();
        if (verifier != null && !verifier.trim().isEmpty()) {
            String[] all_verify_rules = verifier.split("\\|");
            for (String vfy_rule : all_verify_rules) {
                String rule = vfy_rule.trim();
                if (rule.isEmpty()) {
                    continue;
                }

                if (rule.charAt(0) == '-' || (rule.charAt(0) >= '0' && rule.charAt(0) <= '9')) {
                    DataVerifyIntRange vfy = new DataVerifyIntRange(rule);
                    if (vfy.isValid()) {
                        ret.add(vfy);
                    } else {
                        ProgramOptions.getLoger().error("Try to add DataVerifyIntRange(%s) in %s failed", rule,
                                DataSrcImpl.getOurInstance().getCurrentTableName());
                    }

                    continue;
                } else {
                    // 协议验证器
                    DataVerifyImpl vfy = cachePbs.identifiers.getOrDefault(rule, null);
                    if (null == vfy) {
                        DescriptorProtos.EnumDescriptorProto enum_desc = get_alias_list_element(rule, cachePbs.enums,
                                "enum type");
                        if (enum_desc != null) {
                            vfy = new DataVerifyPbEnum(enum_desc);
                        }

                        if (null == vfy) {
                            DescriptorProtos.DescriptorProto msg_desc = get_alias_list_element(rule, cachePbs.messages,
                                    "message type");
                            if (msg_desc != null) {
                                vfy = new DataVerifyPbMsgField(msg_desc);
                            }
                        }

                        if (null == vfy) {
                            DescriptorProtos.OneofDescriptorProto oneof_desc = get_alias_list_element(rule,
                                    cachePbs.oneofs, "oneof type");
                            if (oneof_desc != null) {
                                DescriptorProtos.DescriptorProto msg_desc = null;
                                int message_bound = rule.lastIndexOf('.');
                                if (message_bound > 0 && message_bound < rule.length()) {
                                    msg_desc = get_alias_list_element(rule.substring(0, message_bound),
                                            cachePbs.messages, "message type");
                                } else {
                                    String oneof_full_name = get_alias_list_element_full_name(rule, cachePbs.oneofs,
                                            "oneof type");
                                    message_bound = oneof_full_name.lastIndexOf('.');
                                    if (message_bound > 0 && message_bound < oneof_full_name.length()) {
                                        msg_desc = get_alias_list_element(oneof_full_name.substring(0, message_bound),
                                                cachePbs.messages, "message type");
                                    }
                                }

                                if (oneof_desc != null && msg_desc != null) {
                                    vfy = new DataVerifyPbOneof(oneof_desc, msg_desc);
                                }
                            }
                        }

                        if (null != vfy) {
                            cachePbs.identifiers.put(rule, vfy);
                        } else {
                            ProgramOptions.getLoger().error("Enum, oneof or message \"%s\" not found", rule);
                        }
                    }

                    if (vfy != null) {
                        ret.add(vfy);
                    } else {
                        ProgramOptions.getLoger().error("Try to add data verifier %s in %s failed", rule,
                                DataSrcImpl.getOurInstance().getCurrentTableName());
                    }
                }
            }
        }

        // auto verifier for enum
        if (fd != null && fd.getJavaType() == JavaType.ENUM) {
            String rule = fd.getEnumType().getFullName();
            if (rule.length() > 0 && rule.charAt(0) == '.') {
                rule = rule.substring(1);
            }
            DataVerifyImpl vfy = cachePbs.identifiers.getOrDefault(rule, null);
            if (null == vfy) {
                DescriptorProtos.EnumDescriptorProto enum_desc = get_alias_list_element(rule, cachePbs.enums,
                        "enum type");
                if (enum_desc != null) {
                    vfy = new DataVerifyPbEnum(enum_desc);
                }

                if (null != vfy) {
                    cachePbs.identifiers.put(rule, vfy);
                } else {
                    ProgramOptions.getLoger().error("Enum verifier \"%s\" setup error, please report this bug to %s",
                            rule, ProgramOptions.getReportUrl());
                }
            }

            if (vfy != null) {
                ret.add(vfy);
            }
        }

        return ret;
    }

    private void setup_node_identify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.FieldDescriptor fd) {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetVerifier();

        if (null != identify.dataSourceFieldVerifier && !identify.dataSourceFieldVerifier.isEmpty()) {
            LinkedList<DataVerifyImpl> gen = setup_verifier(identify.dataSourceFieldVerifier, fd);
            if (gen != null && !gen.isEmpty()) {
                for (DataVerifyImpl vfy : gen) {
                    identify.addVerifier(vfy);
                }
            } else {
                identify.resetVerifier();
            }
        }

        // merge verifier from field descriptor
        if (child.innerFieldDesc != null && child.innerFieldDesc.hasVerifier()) {
            for (DataVerifyImpl vfy : child.innerFieldDesc.getVerifier()) {
                identify.addVerifier(vfy);
            }
        }
    }

    private void setup_node_identify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.OneofDescriptor fd) {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetVerifier();

        // Data source field verifier is ignored in oneof descriptor

        // merge verifier from oneof descriptor
        if (child.innerOneofDesc != null && child.innerOneofDesc.hasVerifier()) {
            for (DataVerifyImpl vfy : child.innerOneofDesc.getVerifier()) {
                identify.addVerifier(vfy);
            }
        }
    }

    @Override
    public boolean init() {
        if (false == load_pb_file(cachePbs, ProgramOptions.getInstance().protocolFile, true,
                ProgramOptions.getInstance().protocolIgnoreUnknownDependency, null)) {
            return false;
        }

        currentMsgDesc = get_message_proto(cachePbs, SchemeConf.getInstance().getProtoName());
        if (null == currentMsgDesc) {
            this.setLastErrorMessage("can not find protocol message %s", SchemeConf.getInstance().getProtoName());
            return false;
        }

        return true;
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
            DataDstTypeDescriptor innerDesc) {
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
                    mutableDataDstDescriptor(pbs, fieldPbDesc, pbTypeToInnerType(field.getType())), field.getNumber(),
                    field.getName(), inner_label, field);
            innerDesc.fields.put(field.getName(), innerField);

            setup_extension(innerField, field);
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

            setup_extension(innerField, pbDesc, oneof);
        }
    }

    static private DataDstTypeDescriptor mutableDataDstDescriptor(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type) {
        String key = null;
        if (null == pbDesc) {
            key = type.toString();
        } else {
            key = pbDesc.getFullName();
        }
        DataDstTypeDescriptor ret = pbs.dataDstDescs.getOrDefault(key, null);
        if (ret != null) {
            return ret;
        }

        if (pbDesc == null) {
            ret = DataDstWriterNode.getDefaultMessageDescriptor(type);
        } else {
            DataDstWriterNode.SPECIAL_MESSAGE_TYPE smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.NONE;
            if (pbDesc.getOptions().getMapEntry()) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.MAP;
            } else if (pbDesc.getFullName() == Timestamp.getDescriptor().getFullName()) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT;
            } else if (pbDesc.getFullName() == Duration.getDescriptor().getFullName()) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION;
            }
            ret = new DataDstTypeDescriptor(type, pbDesc.getFile().getPackage(), pbDesc.getName(), pbDesc, smt);
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

        buildDataDstDescriptorMessage(pbs, pbDesc, ret);
        return ret;
    }

    static private DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, DataDstWriterNode.JAVA_TYPE type,
            int listIndex) {
        return DataDstWriterNode.create(null, mutableDataDstDescriptor(pbs, null, type), listIndex);
    }

    static private DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type, int listIndex) {
        if (null == pbDesc) {
            return createMessageWriterNode(pbs, type, listIndex);
        }

        return DataDstWriterNode.create(pbDesc, mutableDataDstDescriptor(pbs, pbDesc, type), listIndex);
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
                -1);
        if (test(ret, new LinkedList<String>())) {
            return ret;
        }

        throw new ConvException(String.format("protocol %s compile mapping relationship failed", name()));
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

        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().nextTable()) {
            // 生成描述集
            DataDstWriterNode desc = src.compile();

            while (DataSrcImpl.getOurInstance().nextRow()) {
                ByteString data = convData(desc);
                if (null != data && !data.isEmpty()) {
                    ++count;
                    blocks.addDataBlock(data);

                    if (null != sha256) {
                        sha256.update(data.toByteArray());
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
            this.logErrorMessage("try to serialize protobuf data failed. %s", e.toString());
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
                setLastErrorMessage(
                        "field \"%s\" in oneof descriptor \"%s\" already exists, can not add another field \"%s\" with the same oneof descriptor",
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
            String field_alias = null;
            if (enable_alias_mapping && fd.getOptions().hasExtension(Xresloader.fieldAlias)
                    && !fd.getOptions().getExtension(Xresloader.fieldAlias).isEmpty()) {
                field_alias = fd.getOptions().getExtension(Xresloader.fieldAlias);
            }
            switch (fd.getType()) {
                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;
                        String repeated_test_name = null;

                        name_list.addLast("");
                        for (;; ++count) {
                            if (0 == count) {
                                repeated_test_name = fd.getName();
                            }

                            DataDstWriterNode c = createMessageWriterNode(cachePbs, fd.getMessageType(),
                                    DataDstWriterNode.JAVA_TYPE.MESSAGE, count);
                            boolean test_passed = false;

                            name_list.removeLast();
                            name_list.addLast(DataDstWriterNode.makeNodeName(repeated_test_name, count));
                            if (test(c, name_list)) {
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                ret = true;
                                test_passed = true;
                            } else if (0 == count && null != field_alias) {
                                repeated_test_name = field_alias;
                                name_list.removeLast();
                                name_list.addLast(DataDstWriterNode.makeNodeName(repeated_test_name, count));
                                if (test(c, name_list)) {
                                    child = node.addChild(fd.getName(), c, fd,
                                            DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                    ret = true;
                                    test_passed = true;
                                }
                            }

                            if (!test_passed) {
                                // Reset to field name when test first element
                                if (0 == count) {
                                    repeated_test_name = fd.getName();
                                }
                                // try plain mode - array item
                                String real_name = DataDstWriterNode.makeChildPath(prefix, repeated_test_name, count);
                                IdentifyDescriptor col = data_src.getColumnByName(real_name);
                                if (null == col && 0 == count && null != field_alias) {
                                    repeated_test_name = field_alias;
                                    String alias_name = DataDstWriterNode.makeChildPath(prefix, repeated_test_name,
                                            count);
                                    col = data_src.getColumnByName(alias_name);
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
                        } else {
                            // try plain mode - the whole array
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);

                            if (null == col && null != field_alias) {
                                String alias_name = DataDstWriterNode.makeChildPath(prefix, field_alias);
                                col = data_src.getColumnByName(alias_name);
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, fd.getMessageType(),
                                        DataDstWriterNode.JAVA_TYPE.MESSAGE, -1);
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
                                DataDstWriterNode.JAVA_TYPE.MESSAGE, -1);
                        boolean test_passed = false;

                        name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName()));
                        if (test(c, name_list)) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            ret = true;
                            test_passed = true;
                        } else if (null != field_alias) {
                            name_list.removeLast();
                            name_list.addLast(DataDstWriterNode.makeNodeName(field_alias));
                            if (test(c, name_list)) {
                                filterMissingFields(missingFields, oneofField, fd, false);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                ret = true;
                                test_passed = true;
                            }
                        }

                        if (!test_passed) {
                            filterMissingFields(missingFields, oneofField, fd, true);

                            // try plain mode
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && null != field_alias) {
                                String alias_name = DataDstWriterNode.makeChildPath(prefix, field_alias);
                                col = data_src.getColumnByName(alias_name);
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
                        String repeated_test_name = null;
                        for (;; ++count) {
                            if (0 == count) {
                                repeated_test_name = fd.getName();
                            }

                            String real_name = DataDstWriterNode.makeChildPath(prefix, repeated_test_name, count);
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && 0 == count && null != field_alias) {
                                repeated_test_name = field_alias;
                                String alias_name = DataDstWriterNode.makeChildPath(prefix, repeated_test_name, count);
                                col = data_src.getColumnByName(alias_name);
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type, count);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                setup_node_identify(c, child, col, fd);
                                ret = true;
                            } else {
                                break;
                            }
                        }

                        if (count > 0) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                        } else {
                            // try plain mode
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null == col && null != field_alias) {
                                String alias_name = DataDstWriterNode.makeChildPath(prefix, field_alias);
                                col = data_src.getColumnByName(alias_name);
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type, -1);
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
                            String alias_name = DataDstWriterNode.makeChildPath(prefix, field_alias);
                            col = data_src.getColumnByName(alias_name);
                        }

                        if (null != col) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type, -1);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            setup_node_identify(c, child, col, fd);
                            ret = true;
                        } else {
                            filterMissingFields(missingFields, oneofField, fd, true);
                            if (checkFieldIsRequired(fd)) {
                                DataDstWriterNode c = createMessageWriterNode(cachePbs, inner_type, -1);
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
                setLastErrorMessage(
                        "type descriptor \"%s\" not found, it's probably a BUG, please report to %s, current version: %s",
                        node.getFullName(), ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstOneofDescriptor oneof_inner_desc = node.getTypeDescriptor().oneofs.getOrDefault(fd.getName(), null);
            if (oneof_inner_desc == null) {
                setLastErrorMessage(
                        "oneof descriptor \"%s\" not found in type descriptor \"%s\", it's probably a BUG, please report to %s, current version: %s",
                        fd.getFullName(), node.getFullName(), ProgramOptions.getReportUrl(),
                        ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstChildrenNode child = null;
            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
            IdentifyDescriptor col = data_src.getColumnByName(real_name);

            // if (null == col && enable_alias_mapping) {
            // if (fd.getOptions().hasExtension(Xresloader.oneofAlias)
            // && !fd.getOptions().getExtension(Xresloader.oneofAlias).isEmpty()) {
            // String alias_name = DataDstWriterNode.makeChildPath(prefix,
            // fd.getOptions().getExtension(Xresloader.oneofAlias));
            // col = data_src.getColumnByName(alias_name);
            // }
            // }

            if (null == col) {
                continue;
            }

            String old_field = oneofField.getOrDefault(fd.getFullName(), null);
            if (old_field != null) {
                setLastErrorMessage(
                        "field \"%s\" in oneof descriptor \"%s\" already exists, can not add the oneof writer again",
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
            String missingFliedDesc = "";
            if (missingFields != null && !missingFields.isEmpty()) {
                missingFliedDesc = String.format(" fields %s", String.join(",", missingFields));
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

            if (!missingFliedDesc.isEmpty() || !missingOneofDesc.isEmpty()) {
                setLastErrorMessage("message %s in %s can not find%s%s in data source", desc.getFullName(), prefix,
                        missingFliedDesc, missingOneofDesc);
                throw new ConvException(getLastErrorMessage());
            }
        }

        return ret;
    }

    private ByteString convData(DataDstWriterNode node) throws ConvException {
        // Descriptors.Descriptor msg_desc = (Descriptors.Descriptor) node.privateData;

        DynamicMessage.Builder root = DynamicMessage.newBuilder(currentMsgDesc);
        boolean valid_data = dumpMessage(root, node);
        // 过滤空项
        if (!valid_data) {
            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            this.logErrorMessage("serialize failed. %s", e.getMessage());
            return null;
        }
    }

    private Object getDefault(DynamicMessage.Builder builder, Descriptors.FieldDescriptor fd) {
        Object val = null;
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
                DynamicMessage.Builder subnode = DynamicMessage.newBuilder(fd.getMessageType());

                // 仅仅Required需要导出默认值
                for (Descriptors.FieldDescriptor sub_fd : fd.getMessageType().getFields()) {
                    if (checkFieldIsRequired(sub_fd)) {
                        dumpDefault(subnode, sub_fd, 0);
                    }
                }

                val = subnode.build();
                break;
            }
            case BYTES:
                val = new byte[0];
                break;
        }
        return val;
    }

    private Object getValueFromDataSource(DataDstWriterNode desc, Descriptors.FieldDescriptor fd) throws ConvException {
        Object val = null;

        switch (fd.getJavaType()) {
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

            case BYTE_STRING: {
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
            case ENUM: {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = fd.getEnumType().findValueByNumber(ret.value.intValue());
                }

                break;
            }

            case MESSAGE: {
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                if (dumpMessage(node, desc) || checkFieldIsRequired(fd)) {
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

    private void dumpValue(DynamicMessage.Builder builder, Descriptors.FieldDescriptor fd, Object val, int index) {
        ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
        if (fd.isRepeated()
                && (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL
                        || stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL)) {
            int fill_default_size = builder.getRepeatedFieldCount(fd);
            for (int i = fill_default_size; i < index; ++i) {
                Object defaultVal = getDefault(builder, fd);
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
                val = get_enum_value(cachePbs, fd.getEnumType(), (Integer) val);
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
            } else {
                builder.setField(fd, enum_val);
            }
        } else {
            if (fd.isRepeated()) {
                if (index > 0 && builder.getRepeatedFieldCount(fd) > index) {
                    builder.setRepeatedField(fd, index, val);
                } else {
                    builder.addRepeatedField(fd, val);
                }
            } else {
                builder.setField(fd, val);
            }
        }
    }

    private void dumpDefault(DynamicMessage.Builder builder, Descriptors.OneofDescriptor fd) {
        builder.clearOneof(fd);
    }

    private void dumpDefault(DynamicMessage.Builder builder, Descriptors.FieldDescriptor fd, int index) {
        Object val = getDefault(builder, fd);
        if (val != null) {
            dumpValue(builder, fd, val, index);
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
    private boolean dumpMessage(DynamicMessage.Builder builder, DataDstWriterNode node) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            if (c.getValue().isOneof()) {
                // dump oneof data
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), child)) {
                        ret = true;
                    }
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) c.getValue().rawDescriptor;
                if (null == fd) {
                    // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                    continue;
                }

                for (int i = 0; i < c.getValue().nodes.size(); i++) {
                    DataDstWriterNode child = c.getValue().nodes.get(i);
                    if (dumpStandardField(builder, child, fd)) {
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

    private boolean dumpStandardField(DynamicMessage.Builder builder, DataDstWriterNode desc,
            Descriptors.FieldDescriptor fd) throws ConvException {
        if (null == desc.identify && MESSAGE != fd.getJavaType()) {
            // required 空字段填充默认值
            if (checkFieldIsRequired(fd)
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, fd, desc.getListIndex());
            }
            return false;
        }

        Object val = getValueFromDataSource(desc, fd);

        if (null == val) {
            if (checkFieldIsRequired(fd)
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, fd, desc.getListIndex());
            }

            return false;
        }

        dumpValue(builder, fd, val, desc.getListIndex());
        return true;
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
        if (field == null) {
            return false;
        }

        if (null == ident) {
            if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                Descriptors.OneofDescriptor fd = (Descriptors.OneofDescriptor) field.getRawDescriptor();
                if (null == fd) {
                    // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                    return false;
                }
                dumpDefault(builder, fd);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
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
            dumpDefault(builder, (Descriptors.FieldDescriptor) sub_field.getRawDescriptor(), 0);
            return true;
        }

        // 非顶层，不用验证类型
        return dumpPlainField(builder, null, sub_field, null, (String) res[1]);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode) throws ConvException {
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return false;
        }

        if (null == ident) {
            if (checkFieldIsRequired(fd)
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, fd, 0);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            if (checkFieldIsRequired(fd)
                    || ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                dumpDefault(builder, fd, 0);
            }
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input)
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
                    ArrayList<DynamicMessage> tmp = new ArrayList<DynamicMessage>();
                    tmp.ensureCapacity(groups.length);
                    for (String v : groups) {
                        String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                        DynamicMessage msg = parsePlainDataMessage(subGroups, ident, field);
                        if (msg != null) {
                            tmp.add(msg);
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
                                String.format("Try to convert %s.%s[%d] failed, too many elements(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), values.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    ProgramOptions.ListStripRule stripListRule = ProgramOptions.getInstance().stripListRule;
                    if (stripListRule == ProgramOptions.ListStripRule.KEEP_ALL
                            || stripListRule == ProgramOptions.ListStripRule.STRIP_EMPTY_TAIL) {
                        while (builder.getRepeatedFieldCount(fd) < index) {
                            builder.addRepeatedField(fd, getDefault(builder, fd));
                        }
                    }

                    if (index >= 0 && builder.getRepeatedFieldCount(fd) > index) {
                        builder.setRepeatedField(fd, index, values.get(0));
                    } else {
                        builder.addRepeatedField(fd, values.get(0));
                    }
                } else {
                    for (int i = 0; i < values.size(); ++i) {
                        dumpValue(builder, fd, values.get(i), i);
                    }
                }
            } else if (ProgramOptions.getInstance().stripListRule == ProgramOptions.ListStripRule.KEEP_ALL) {
                builder.addRepeatedField(fd, getDefault(builder, fd));
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
                        dumpDefault(builder, fd, 0);
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
                    dumpValue(builder, fd, values.get(i), i);
                }
            } else {
                dumpValue(builder, fd, val, 0);
            }

            return true;
        }
    }

    public DynamicMessage parsePlainDataMessage(String[] inputs, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd || fd.getJavaType() != MESSAGE) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return null;
        }

        ArrayList<DataDstWriterNode.DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();
        DynamicMessage.Builder ret = DynamicMessage.newBuilder(fd.getMessageType());
        boolean hasData = false;
        HashSet<String> dumpedOneof = null;
        if (field.getTypeDescriptor().getSortedOneofs().size() > 0) {
            dumpedOneof = new HashSet<String>();
        }

        int usedInputIdx = 0;
        for (int i = 0; i < children.size(); ++i) {
            if (children.get(i).getReferOneof() != null) {
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
                    hasData = true;
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

                if (dumpPlainField(ret, null, children.get(i), null, inputs[usedInputIdx])) {
                    hasData = true;
                }

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

        if (!hasData) {
            return null;
        }

        return ret.build();
    }

    /**
     * 生成常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> buildConst() {
        if (false == load_pb_file(cachePbs, ProgramOptions.getInstance().protocolFile, true, true, null)) {
            return null;
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
        if (ProgramOptions.getInstance().protocolFile.equals(ProgramOptions.getInstance().protoDumpFile)) {
            return null;
        }

        try {
            File f = new File(ProgramOptions.getInstance().protocolFile);

            FileInputStream fin = new FileInputStream(ProgramOptions.getInstance().protocolFile);
            byte[] all_buffer = new byte[(int) f.length()];
            fin.read(all_buffer);
            fin.close();

            return all_buffer;
        } catch (FileNotFoundException e) {
            this.logErrorMessage("protocol file %s not found.", ProgramOptions.getInstance().protocolFile);
        }

        return null;
    }

    /**
     * 生成选项数据
     *
     * @return 选项数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildOptions(ProgramOptions.ProtoDumpType dumpType) {
        if (false == load_pb_file(cachePbs, ProgramOptions.getInstance().protocolFile, true, true, null)) {
            return null;
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
