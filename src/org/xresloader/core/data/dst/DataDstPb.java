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
import com.google.protobuf.Descriptors.FieldDescriptor;
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
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private Descriptors.Descriptor currentMsgDesc = null;

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

        // ========================== 配置描述集 ==========================
        /*** 类型信息-文件描述器集合 ***/
        public HashMap<String, Descriptors.FileDescriptor> file_descs = new HashMap<String, Descriptors.FileDescriptor>();
        public HashSet<String> file_descs_failed = new HashSet<String>();
        /*** 类型信息-Message描述器集合 ***/
        public HashMap<String, PbAliasNode<Descriptors.Descriptor>> message_descs = new HashMap<String, PbAliasNode<Descriptors.Descriptor>>();

        // ========================== 验证器 ==========================
        HashMap<String, DataVerifyImpl> identifiers = new HashMap<String, DataVerifyImpl>();

        // ========================== 内建AST类型缓存 ==========================
        HashMap<String, DataDstMessageDescriptor> dataDstDescs = new HashMap<String, DataDstMessageDescriptor>();

        public PbInfoSet() {
        }
    }

    static <T> void append_alias_list(String short_name, String full_name, HashMap<String, PbAliasNode<T>> hashmap,
            T ele) {
        if (!short_name.isEmpty()) {
            PbAliasNode<T> ls = hashmap.getOrDefault(short_name, null);
            if (null == ls) {
                ls = new PbAliasNode<T>();
                ls.names = new LinkedList<String>();
                hashmap.put(short_name, ls);
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
        PbAliasNode<T> ls = hashmap.getOrDefault(name, null);
        if (null == ls || null == ls.element) {
            return null;
        }

        if (null == ls.names || ls.names.size() <= 1) {
            return ls.element;
        }

        ProgramOptions.getLoger().error(
                "there is more than one %s \"%s\" matched, please use full name. available names:", type_name, name);
        for (String full_name : ls.names) {
            ProgramOptions.getLoger().error("\t%s", full_name);
        }

        return null;
    }

    static private com.google.protobuf.ExtensionRegistryLite pb_extensions = null;

    static private com.google.protobuf.ExtensionRegistryLite get_extension_registry() {
        if (null != pb_extensions) {
            return pb_extensions;
        }

        pb_extensions = com.google.protobuf.ExtensionRegistryLite.newInstance();

        Xresloader.registerAllExtensions(pb_extensions);
        XresloaderUe.registerAllExtensions(pb_extensions);

        return pb_extensions;
    }

    static private PbInfoSet pbs = new PbInfoSet();

    static void load_pb_message(DescriptorProtos.DescriptorProto mdp, String package_name,
            HashMap<String, PbAliasNode<DescriptorProtos.DescriptorProto>> hashmap) {
        String full_name = String.format("%s.%s", package_name, mdp.getName());
        append_alias_list(mdp.getName(), full_name, pbs.messages, mdp);
        for (DescriptorProtos.DescriptorProto sub_mdp : mdp.getNestedTypeList()) {
            load_pb_message(sub_mdp, full_name, hashmap);
        }
    }

    static boolean load_pb_file(String file_path, boolean build_msg, boolean allow_unknown_dependencies) {
        if (pbs.fileNames.contains(file_path)) {
            return true;
        }

        try {
            // 文件描述集读取
            InputStream fis = new FileInputStream(file_path);
            DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis,
                    get_extension_registry());
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
                        load_pb_message(mdp, fdp.getPackage(), pbs.messages);
                    }
                }
            }

            // 初始化
            if (build_msg) {
                for (HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fme : pbs.files.entrySet()) {
                    init_pb_files(fme.getKey(), allow_unknown_dependencies);
                }
            }

        } catch (FileNotFoundException e) {
            ProgramOptions.getLoger().error("read protocol file \"%s\" failed. %s",
                    ProgramOptions.getInstance().protocolFile, e.toString());
            return false;
        } catch (IOException e) {
            ProgramOptions.getLoger().error("parse protocol file \"%s\" failed. %s",
                    ProgramOptions.getInstance().protocolFile, e.toString());
            return false;
        }

        // 载入完成,swap
        return true;
    }

    static Descriptors.Descriptor get_message_proto(String proto_name) {
        return get_alias_list_element(proto_name, pbs.message_descs, "protocol message");
    }

    static Descriptors.FileDescriptor init_pb_files(String name, boolean allow_unknown_dependencies) {
        Descriptors.FileDescriptor ret = pbs.file_descs.getOrDefault(name, null);
        if (null != ret) {
            return ret;
        }

        if (pbs.file_descs_failed.contains(name)) {
            return null;
        }

        DescriptorProtos.FileDescriptorProto fdp = pbs.files.getOrDefault(name, null);
        if (null == fdp) {
            if (allow_unknown_dependencies) {
                ProgramOptions.getLoger().warn("protocol file descriptor %s not found.", name);
            } else {
                ProgramOptions.getLoger().error("protocol file descriptor %s not found.", name);
            }

            pbs.file_descs_failed.add(name);
            return null;
        }

        ArrayList<Descriptors.FileDescriptor> deps = new ArrayList<Descriptors.FileDescriptor>();
        ArrayList<String> failed_deps = new ArrayList<String>();
        deps.ensureCapacity(fdp.getDependencyCount());
        failed_deps.ensureCapacity(fdp.getDependencyCount());
        for (int i = 0; i < fdp.getDependencyCount(); ++i) {
            Descriptors.FileDescriptor dep = init_pb_files(fdp.getDependency(i), allow_unknown_dependencies);
            if (null == dep) {
                if (allow_unknown_dependencies){
                    failed_deps.add(fdp.getDependency(i));
                } else {
                    ProgramOptions.getLoger().error("initialize protocol file descriptor %s failed. dependency %s", name, fdp.getDependency(i));
                    return null;
                }
            } else {
                deps.add(dep);
            }
        }

        if (!failed_deps.isEmpty()) {
            ProgramOptions.getLoger().warn("initialize protocol file descriptor %s without dependency %s, maybe missing some descriptor(s).", 
                name, String.join(",", failed_deps)
            );
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
            ProgramOptions.getLoger().error("initialize protocol file descriptor %s failed. %s", name, e.getMessage());
            return null;
        }
    }

    static private void setup_extension(DataDstFieldDescriptor child_field, Descriptors.FieldDescriptor fd) {
        if (fd.getOptions().hasExtension(Xresloader.verifier)) {
            child_field.mutableExtension().verifier = fd.getOptions().getExtension(Xresloader.verifier);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldDescription)) {
            child_field.mutableExtension().description = fd.getOptions().getExtension(Xresloader.fieldDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldRatio)) {
            child_field.mutableExtension().ratio = fd.getOptions().getExtension(Xresloader.fieldRatio);
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

    private void setup_node_identify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.FieldDescriptor fd) {
        node.identify = identify;

        String verifier = child.innerDesc.mutableExtension().verifier;
        identify.ratio = child.innerDesc.mutableExtension().ratio;

        if (null != identify.dataSourceFieldVerifier && !identify.dataSourceFieldVerifier.isEmpty()) {
            if (verifier == null || verifier.isEmpty()) {
                verifier = identify.dataSourceFieldVerifier;
            } else {
                verifier = verifier + "|" + identify.dataSourceFieldVerifier;
            }
        }

        // setup identifier
        if (verifier == null || verifier.isEmpty()) {
            identify.resetVerifier();
        } else {
            String[] all_verify_rules = verifier.split("\\|");
            for (String vfy_rule : all_verify_rules) {
                String rule = vfy_rule.trim();
                if (rule.isEmpty()) {
                    continue;
                }

                if (rule.charAt(0) == '-' || (rule.charAt(0) >= '0' && rule.charAt(0) <= '9')) {
                    DataVerifyIntRange vfy = new DataVerifyIntRange(rule);
                    if (vfy.isValid()) {
                        identify.addVerifier(vfy);
                    } else {
                        ProgramOptions.getLoger().error(
                                "try to add DataVerifyIntRange(%s) for %s at column %d in %s failed", rule,
                                identify.name, identify.index + 1, DataSrcImpl.getOurInstance().getCurrentTableName());
                    }

                    continue;
                } else {
                    // 协议验证器
                    DataVerifyImpl vfy = pbs.identifiers.getOrDefault(rule, null);
                    if (null == vfy) {
                        DescriptorProtos.EnumDescriptorProto enum_desc = get_alias_list_element(rule, pbs.enums,
                                "enum type");
                        if (enum_desc != null) {
                            vfy = new DataVerifyPbEnum(enum_desc);
                        } else {
                            DescriptorProtos.DescriptorProto msg_desc = get_alias_list_element(rule, pbs.messages,
                                    "message type");
                            if (msg_desc != null) {
                                vfy = new DataVerifyPbMsg(msg_desc);
                            }
                        }

                        if (null != vfy) {
                            pbs.identifiers.put(rule, vfy);
                        } else {
                            ProgramOptions.getLoger().error("enum or message \"%s\" not found", rule);
                        }
                    }

                    if (vfy != null) {
                        identify.addVerifier(vfy);
                    } else {
                        ProgramOptions.getLoger().error("try to add DataVerifyPb(%s) for %s at column %d in %s failed",
                                rule, identify.name, identify.index + 1,
                                DataSrcImpl.getOurInstance().getCurrentTableName());
                    }
                }
            }
        }

    }

    @Override
    public boolean init() {
        if (false == load_pb_file(ProgramOptions.getInstance().protocolFile, true, false)) {
            return false;
        }

        currentMsgDesc = get_message_proto(SchemeConf.getInstance().getProtoName());
        return null != currentMsgDesc;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "protobuf";
    }

    static private void buildDataDstDescriptorMessage(Descriptors.Descriptor pbDesc,
            DataDstMessageDescriptor innerDesc) {
        if (null == pbDesc || null == innerDesc) {
            return;
        }

        innerDesc.fields = new HashMap<String, DataDstFieldDescriptor>();
        for (Descriptors.FieldDescriptor field : pbDesc.getFields()) {
            Descriptors.Descriptor fieldPbDesc = null;
            if (field.getJavaType() == JavaType.MESSAGE) {
                fieldPbDesc = field.getMessageType();
            }
            DataDstFieldDescriptor innerField = new DataDstFieldDescriptor(
                    mutableDataDstDescriptor(fieldPbDesc, pbTypeToInnerType(field.getType())), field.getNumber(),
                    field.getName(), field.isRepeated());
            innerDesc.fields.put(field.getName(), innerField);

            setup_extension(innerField, field);
        }
    }

    static private DataDstMessageDescriptor mutableDataDstDescriptor(Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type) {
        String key = null;
        if (null == pbDesc) {
            key = type.toString();
        } else {
            key = pbDesc.getFullName();
        }
        DataDstMessageDescriptor ret = pbs.dataDstDescs.getOrDefault(key, null);
        if (ret != null) {
            return ret;
        }

        if (pbDesc == null) {
            ret = DataDstWriterNode.getDefaultMessageDescriptor(type);
        } else {
            ret = new DataDstMessageDescriptor(type, pbDesc.getFile().getPackage(), pbDesc.getName());
        }
        pbs.dataDstDescs.put(key, ret);
        buildDataDstDescriptorMessage(pbDesc, ret);
        return ret;
    }

    static private DataDstWriterNode createMessageWriterNode(Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type) {
        if (null == pbDesc) {
            return DataDstWriterNode.create(null, mutableDataDstDescriptor(pbDesc, type));
        }

        DataDstWriterNode ret = DataDstWriterNode.create(pbDesc, mutableDataDstDescriptor(pbDesc, type));

        // extensions
        if (pbDesc.getOptions().hasExtension(Xresloader.msgDescription)) {
            ret.getMessageExtension().description = pbDesc.getOptions().getExtension(Xresloader.msgDescription);
        }

        // if (pbDesc.getOptions().getExtensionCount(Xresloader.kvIndex) > 0) {
        //     ret.getMessageExtension().kvIndex = pbDesc.getOptions().getExtension(Xresloader.kvIndex);
        // }

        // if (pbDesc.getOptions().getExtensionCount(Xresloader.klIndex) > 0) {
        //     ret.getMessageExtension().klIndex = pbDesc.getOptions().getExtension(Xresloader.klIndex);
        // }

        // extensions for UE
        if (pbDesc.getOptions().hasExtension(XresloaderUe.helper)) {
            ret.getMessageExtension().mutableUE().helper = pbDesc.getOptions().getExtension(XresloaderUe.helper);
        }

        if (pbDesc.getOptions().hasExtension(XresloaderUe.notDataTable)) {
            ret.getMessageExtension().mutableUE().notDataTable = pbDesc.getOptions()
                    .getExtension(XresloaderUe.notDataTable);
        }

        return ret;
    }

    @Override
    public final DataDstWriterNode compile() throws ConvException {
        DataDstWriterNode ret = createMessageWriterNode(currentMsgDesc, DataDstWriterNode.JAVA_TYPE.MESSAGE);
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
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            ProgramOptions.getLoger().error("failed to find md5 algorithm.");
            header.setHashCode("");
        }

        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().next_table()) {
            // 生成描述集
            DataDstWriterNode desc = src.compile();

            while (DataSrcImpl.getOurInstance().next_row()) {
                ByteString data = convData(desc);
                if (null != data && !data.isEmpty()) {
                    ++count;
                    blocks.addDataBlock(data);

                    if (null != md5) {
                        md5.update(data.toByteArray());
                    }
                }
            }

            if (desc.getMessageExtension().description != null) {
                descriptionList.add(desc.getMessageExtension().description);
            }
        }

        header.setCount(count);
        if (null != md5) {
            header.setHashCode("md5:" + Hex.encodeHexString(md5.digest()));
        }

        if (!descriptionList.isEmpty()) {
            header.setDescription(String.join(getSystemEndl(), descriptionList));
        }

        // 写出
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        try {
            PbHeaderV3.xresloader_header builtHeader = header.build();
            blocks.build().writeTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
            ProgramOptions.getLoger().error("try to serialize protobuf header failed. %s", e.toString());
            ProgramOptions.getLoger().error("%s", header.build().getInitializationErrorString());
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

    /**
     * 测试并生成数据结构
     *
     * @param node      待填充的节点
     * @param name_list 当前命名列表
     * @return 查找到对应的数据源映射关系并非空则返回true，否则返回false
     */
    private boolean test(DataDstWriterNode node, LinkedList<String> name_list) throws ConvException {
        String prefix = String.join(".", name_list);
        boolean ret = false;
        Descriptors.Descriptor desc = (Descriptors.Descriptor) node.privateData;
        if (null == desc) {
            return ret;
        }

        DataSrcImpl data_src = DataSrcImpl.getOurInstance();
        for (Descriptors.FieldDescriptor fd : desc.getFields()) {
            DataDstChildrenNode child = null;
            switch (fd.getType()) {
            // 复杂类型还需要检测子节点
            case MESSAGE:
                if (fd.isRepeated()) {
                    int count = 0;

                    name_list.addLast("");
                    for (;; ++count) {
                        DataDstWriterNode c = createMessageWriterNode(fd.getMessageType(),
                                DataDstWriterNode.JAVA_TYPE.MESSAGE);
                        name_list.removeLast();
                        name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName(), count));
                        if (test(c, name_list)) {
                            child = node.addChild(fd.getName(), c, fd, true, false);
                            ret = true;
                        } else {
                            break;
                        }
                    }
                    name_list.removeLast();
                } else {
                    DataDstWriterNode c = createMessageWriterNode(fd.getMessageType(),
                            DataDstWriterNode.JAVA_TYPE.MESSAGE);
                    name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName()));
                    if (test(c, name_list)) {
                        child = node.addChild(fd.getName(), c, fd, false, false);
                        ret = true;
                    } else if (fd.isRequired()) {
                        // required 字段要dump默认数据
                        child = node.addChild(fd.getName(), c, fd, false, true);
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
                        if (null != col) {
                            DataDstWriterNode c = createMessageWriterNode(null, inner_type);
                            child = node.addChild(fd.getName(), c, fd, true, false);
                            setup_node_identify(c, child, col, fd);
                            ret = true;
                        } else {
                            break;
                        }
                    }
                } else {
                    // 非 list 类型
                    String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                    IdentifyDescriptor col = data_src.getColumnByName(real_name);
                    if (null != col) {
                        DataDstWriterNode c = createMessageWriterNode(null, inner_type);
                        child = node.addChild(fd.getName(), c, fd, false, false);
                        setup_node_identify(c, child, col, fd);
                        ret = true;
                    } else if (fd.isRequired()) {
                        DataDstWriterNode c = createMessageWriterNode(null, inner_type);
                        // required 字段要dump默认数据
                        child = node.addChild(fd.getName(), c, fd, false, true);
                    }
                }
                break;
            }
            }
        }

        return ret;
    }

    private ByteString convData(DataDstWriterNode node) throws ConvException {
        Descriptors.Descriptor msg_desc = (Descriptors.Descriptor) node.privateData;

        DynamicMessage.Builder root = DynamicMessage.newBuilder(currentMsgDesc);
        boolean valid_data = dumpMessage(root, node);
        // 过滤空项
        if (!valid_data) {
            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            ProgramOptions.getLoger().error("serialize failed. %s", e.getMessage());
            return null;
        }
    }

    private void dumpDefault(DynamicMessage.Builder builder, Descriptors.FieldDescriptor fd) {
        switch (fd.getType()) {
        case DOUBLE:
            builder.setField(fd, Double.valueOf(0.0));
            break;
        case FLOAT:
            builder.setField(fd, Float.valueOf(0));
            break;
        case INT64:
        case UINT64:
        case INT32:
        case FIXED64:
        case FIXED32:
        case UINT32:
        case SFIXED32:
        case SFIXED64:
        case SINT32:
        case SINT64:
            builder.setField(fd, 0);
            break;
        case ENUM:
            builder.setField(fd, fd.getEnumType().findValueByNumber(0));
            break;
        case BOOL:
            builder.setField(fd, false);
            break;
        case STRING:
            builder.setField(fd, "");
            break;
        case GROUP:
            builder.setField(fd, new byte[0]);
            break;
        case MESSAGE: {
            DynamicMessage.Builder subnode = DynamicMessage.newBuilder(fd.getMessageType());

            // fill required
            for (Descriptors.FieldDescriptor sub_fd : fd.getMessageType().getFields()) {
                if (sub_fd.isRequired() || ProgramOptions.getInstance().enbleEmptyList) {
                    dumpDefault(subnode, sub_fd);
                }
            }

            builder.setField(fd, subnode.build());
            break;
        }
        case BYTES:
            builder.setField(fd, new byte[0]);
            break;
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
            Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) c.getValue().fieldDescriptor;
            if (null == fd) {
                // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                continue;
            }

            for (DataDstWriterNode child : c.getValue().nodes) {
                if (dumpField(builder, child, fd)) {
                    ret = true;
                }
            }
        }

        return ret;
    }

    private boolean dumpField(DynamicMessage.Builder builder, DataDstWriterNode desc, Descriptors.FieldDescriptor fd)
            throws ConvException {
        if (null == desc.identify && MESSAGE != fd.getJavaType()) {
            // required 空字段填充默认值
            if (fd.isRepeated() || ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, fd);
            }
            return false;
        }

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
            if (dumpMessage(node, desc) || fd.isRequired()) {
                try {
                    val = node.build();
                } catch (UninitializedMessageException e) {
                    ProgramOptions.getLoger().error("serialize %s(%s) failed. %s", fd.getFullName(),
                            fd.getMessageType().getName(), e.getMessage());
                }
            }
            break;
        }

        default:
            break;
        }

        if (null == val) {
            if (fd.isRequired() || ProgramOptions.getInstance().enbleEmptyList) {
                dumpDefault(builder, fd);
            }

            return false;
        }

        if (fd.isRepeated()) {
            builder.addRepeatedField(fd, val);
        } else {
            builder.setField(fd, val);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    static private void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent, Descriptors.EnumDescriptor enum_desc) {
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
    static private void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent, Descriptors.OneofDescriptor oneof_desc) {
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
    static private void dumpConstIntoHashMap(String package_name, HashMap<String, Object> parent, Descriptors.Descriptor msg_desc) {
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

    /**
     * 生成常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> buildConst() {
        if (false == load_pb_file(ProgramOptions.getInstance().protocolFile, true, true)) {
            return null;
        }

        if (null == pbs.enums) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : pbs.file_descs.entrySet()) {
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
                            ProgramOptions.getLoger().error("package name %s conflict(failed in %s).",
                                    fdp.getValue().getPackage(), seg);
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
                dumpConstIntoHashMap(fdp.getValue().getPackage(), fd_root, msg_desc);
            }

            for (Descriptors.EnumDescriptor enum_desc : fdp.getValue().getEnumTypes()) {
                dumpConstIntoHashMap(fdp.getValue().getPackage(), fd_root, enum_desc);
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
        if (ProgramOptions.getInstance().protocolFile.equals(ProgramOptions.getInstance().constPrint)) {
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
            ProgramOptions.getLoger().error("protocol file %s not found.", ProgramOptions.getInstance().protocolFile);
        }

        return null;
    }
}
