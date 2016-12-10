package com.owent.xresloader.data.dst;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.data.src.DataContainer;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.data.vfy.DataVerifyImpl;
import com.owent.xresloader.data.vfy.DataVerifyPbEnum;
import com.owent.xresloader.data.vfy.DataVerifyPbMsg;
import com.owent.xresloader.engine.IdentifyDescriptor;
import com.owent.xresloader.pb.PbHeaderV3;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;

/**
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private Descriptors.Descriptor currentMsgDesc = null;

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
        public HashMap<String, DescriptorProtos.DescriptorProto> messages = new HashMap<String, DescriptorProtos.DescriptorProto>();
        /*** 描述信息-枚举描述集合 ***/
        public HashMap<String, DescriptorProtos.EnumDescriptorProto> enums = new HashMap<String, DescriptorProtos.EnumDescriptorProto>();

        // ========================== 配置描述集 ==========================
        /*** 类型信息-文件描述器集合 ***/
        public HashMap<String, Descriptors.FileDescriptor> file_descs = new HashMap<String, Descriptors.FileDescriptor>();
        /*** 类型信息-Message描述器集合 ***/
        public HashMap<String, Descriptors.Descriptor> message_descs = new HashMap<String, Descriptors.Descriptor>();

        // ========================== 验证器 ==========================
        HashMap<String, DataVerifyImpl> identifiers = new HashMap<String, DataVerifyImpl>();
        public PbInfoSet(){}
    }

    static private PbInfoSet pbs = new PbInfoSet();
    static boolean load_pb_file(String file_path, boolean build_msg) {
        if (pbs.fileNames.contains(file_path)) {
            return true;
        }
        String package_name = "";
        try {
            // 文件描述集读取
            InputStream fis = new FileInputStream(file_path);
            DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis);
            pbs.fileNames.add(file_path);
            // 保存文件名和文件描述Proto的关系
            for (DescriptorProtos.FileDescriptorProto fdp : fds.getFileList()) {
                if (pbs.files.containsKey(fdp.getName())) {
                    continue;
                }
                pbs.files.put(fdp.getName(), fdp);

                if (package_name.isEmpty()) {
                    package_name = fdp.getPackage();
                } else if (!package_name.equals(fdp.getPackage())) {
                    ProgramOptions.getLoger().warn("configure protocol namespace change from %s to %s, more than one package may lead to type coverage",
                            package_name,
                            fdp.getPackage()
                    );
                    package_name = fdp.getPackage();
                }

                for(DescriptorProtos.EnumDescriptorProto edp: fdp.getEnumTypeList()) {
                    pbs.enums.put(edp.getName(), edp);
                }

                for(DescriptorProtos.DescriptorProto mdp: fdp.getMessageTypeList()) {
                    pbs.messages.put(mdp.getName(), mdp);
                }
            }

            // 初始化
            if (build_msg) {
                for (HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fme : pbs.files.entrySet()) {
                    init_pb_files(fme.getKey());
                }
            }

        } catch (FileNotFoundException e) {
            ProgramOptions.getLoger().error("read protocol file \"%s\" failed. %s", ProgramOptions.getInstance().protocolFile, e.toString());
            return false;
        } catch (IOException e) {
            ProgramOptions.getLoger().error("parse protocol file \"%s\" failed. %s", ProgramOptions.getInstance().protocolFile, e.toString());
            return false;
        }

        // 载入完成,swap
        return true;
    }

    static Descriptors.Descriptor get_message_proto(String proto_name) {
        Descriptors.Descriptor ret = pbs.message_descs.getOrDefault(proto_name, null);
        if (null == ret) {
            ProgramOptions.getLoger().error("protocol message %s not found.", proto_name);
            return null;
        }
        return ret;
    }

    static Descriptors.FileDescriptor init_pb_files(String name) {
        Descriptors.FileDescriptor ret = pbs.file_descs.getOrDefault(name, null);
        if (null != ret) {
            return ret;
        }

        DescriptorProtos.FileDescriptorProto fdp = pbs.files.getOrDefault(name, null);
        if (null == fdp) {
            ProgramOptions.getLoger().error("protocol file descriptor %s not found.", name);
            return null;
        }

        Descriptors.FileDescriptor[] deps = new Descriptors.FileDescriptor[fdp.getDependencyCount()];
        for(int i = 0; i < fdp.getDependencyCount(); ++ i) {
            deps[i] = init_pb_files(fdp.getDependency(i));
            if (null == deps[i]) {
                ProgramOptions.getLoger().error("initialize protocol file descriptor %s failed. dependency %s", name, fdp.getDependency(i));
                return null;
            }
        }

        try {
            ret = Descriptors.FileDescriptor.buildFrom(fdp, deps);
            pbs.file_descs.put(name, ret);
            for(Descriptors.Descriptor md: ret.getMessageTypes()) {
                pbs.message_descs.put(md.getName(), md);
            }

            return ret;
        } catch (Descriptors.DescriptorValidationException e) {
            ProgramOptions.getLoger().error("initialize protocol file descriptor %s failed. %s", name, e.getMessage());
            return null;
        }
    }

    private void setup_node_identify(DataDstWriterNode node, IdentifyDescriptor identify) {
        node.identify = identify;

        if (null == identify.verifier || identify.verifier.isEmpty()) {
            identify.verify_engine = null;
        } else {
            identify.verify_engine = pbs.identifiers.getOrDefault(identify.verifier, null);
            if (null == identify.verify_engine) {

                DescriptorProtos.EnumDescriptorProto enum_desc = pbs.enums.getOrDefault(identify.verifier, null);
                if (enum_desc != null) {
                    identify.verify_engine = new DataVerifyPbEnum(enum_desc);
                } else {
                    DescriptorProtos.DescriptorProto msg_desc = pbs.messages.getOrDefault(identify.verifier, null);
                    if (msg_desc != null) {
                        identify.verify_engine = new DataVerifyPbMsg(msg_desc);
                    }
                }

                if (null != identify.verify_engine) {
                    pbs.identifiers.put(identify.verifier, identify.verify_engine);
                }
            }
        }
    }

    @Override
    public boolean init() {
        if (false == load_pb_file(ProgramOptions.getInstance().protocolFile, true)) {
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

    @Override
    public final DataDstWriterNode compile() throws ConvException {
        DataDstWriterNode ret = DataDstWriterNode.create(currentMsgDesc, DataDstWriterNode.JAVA_TYPE.MESSAGE);
        if (test(ret, new LinkedList<String>())) {
            return ret;
        }

        throw new ConvException(String.format(
                "protocol %s compile mapping relationship failed", name()
        ));
    }

    @Override
    public final byte[] build(DataDstImpl src) throws ConvException {
        // 初始化header
        PbHeaderV3.xresloader_datablocks.Builder blocks = PbHeaderV3.xresloader_datablocks.newBuilder();
        PbHeaderV3.xresloader_header.Builder header = blocks.getHeaderBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getVersion());
        header.setHashCode("");

        // 校验码
        MessageDigest md5 = null ;
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

            while(DataSrcImpl.getOurInstance().next_row()) {
                ByteString data = convData(desc);
                if (null != data && !data.isEmpty()) {
                    ++count;
                    blocks.addDataBlock(data);

                    if (null != md5) {
                        md5.update(data.toByteArray());
                    }
                }
            }
        }
        header.setCount(count);
        if (null != md5) {
            header.setHashCode("md5:" + Hex.encodeHexString(md5.digest()));
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

    /**
     * 测试并生成数据结构
     *
     * @param node      待填充的节点
     * @param name_list 当前命名列表
     * @return 查找到对应的数据源映射关系并非空则返回true，否则返回false
     */
    private boolean test(DataDstWriterNode node, LinkedList<String> name_list) {
        String prefix = String.join(".", name_list);
        boolean ret = false;
        Descriptors.Descriptor desc = (Descriptors.Descriptor)node.descriptor;
        if (null == desc) {
            return ret;
        }

        DataSrcImpl data_src = DataSrcImpl.getOurInstance();
        for (Descriptors.FieldDescriptor fd : desc.getFields()) {
            switch (fd.getType()) {
                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;

                        name_list.addLast("");
                        for (; ; ++count) {
                            DataDstWriterNode c = DataDstWriterNode.create(fd.getMessageType(), DataDstWriterNode.JAVA_TYPE.MESSAGE);
                            name_list.removeLast();
                            name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName(), count));
                            if (test(c, name_list)) {
                                node.addChild(fd.getName(), c, fd,true);
                                ret = true;
                            } else {
                                break;
                            }
                        }
                        name_list.removeLast();
                    } else {
                        DataDstWriterNode c = DataDstWriterNode.create(fd.getMessageType(), DataDstWriterNode.JAVA_TYPE.MESSAGE);
                        name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName()));
                        if (test(c, name_list)) {
                            node.addChild(fd.getName(), c, fd,false);
                            ret = true;
                        }
                        name_list.removeLast();
                    }
                    break;
                default: {
                    // list 类型
                    DataDstWriterNode.JAVA_TYPE inner_type = DataDstWriterNode.JAVA_TYPE.INT;
                    switch (fd.getType()) {
                        case DOUBLE:
                            inner_type = DataDstWriterNode.JAVA_TYPE.DOUBLE;
                            break;
                        case FLOAT:
                            inner_type = DataDstWriterNode.JAVA_TYPE.FLOAT;
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
                            inner_type = DataDstWriterNode.JAVA_TYPE.LONG;
                            break;
                        case BOOL:
                            inner_type = DataDstWriterNode.JAVA_TYPE.BOOLEAN;
                            break;
                        case STRING:
                            inner_type = DataDstWriterNode.JAVA_TYPE.STRING;
                            break;
                        case GROUP:
                        case MESSAGE:
                        case BYTES:
                            inner_type = DataDstWriterNode.JAVA_TYPE.BYTES;
                            break;
                        case ENUM:
                            break;
                    }
                    if (fd.isRepeated()) {
                        int count = 0;
                        for (; ; ++count) {
                            DataDstWriterNode c = DataDstWriterNode.create(null, inner_type);
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                            IdentifyDescriptor col = data_src.getColumnByName(real_name);
                            if (null != col) {
                                setup_node_identify(c, col);
                                node.addChild(fd.getName(), c, fd,true);
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
                            DataDstWriterNode c = DataDstWriterNode.create(null, inner_type);
                            setup_node_identify(c, col);
                            node.addChild(fd.getName(), c, fd,false);
                            ret = true;
                        }
                    }
                    break;
                }
            }
        }

        return ret;
    }

    private ByteString convData(DataDstWriterNode node) throws ConvException {
        Descriptors.Descriptor msg_desc = (Descriptors.Descriptor)node.descriptor;

        DynamicMessage.Builder root = DynamicMessage.newBuilder(currentMsgDesc);
        boolean valid_data = dumpMessage(root, node);
        // 过滤空项
        if(!valid_data) {
            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            ProgramOptions.getLoger().error("serialize failed. %s", root.getInitializationErrorString());
            return null;
        }
    }


    /**
     * 转储数据到builder
     * @param builder 转储目标
     * @param node message的描述结构
     * @return 有数据则返回true
     * @throws ConvException
     */
    private boolean dumpMessage(DynamicMessage.Builder builder, DataDstWriterNode node) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor)c.getValue().fieldDescriptor;
            if (null == fd) {
                // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                continue;
            }

            for (DataDstWriterNode child: c.getValue().nodes) {
                if (dumpField(builder, child, fd)) {
                    ret = true;
                }
            }
        }

        return ret;
    }

    private boolean dumpField(DynamicMessage.Builder builder, DataDstWriterNode desc, Descriptors.FieldDescriptor fd) throws ConvException {
        if (null == desc.identify && MESSAGE != fd.getJavaType()) {
            return false;
        }

        Object val = null;

        switch (fd.getJavaType()) {
            case INT: {
                DataContainer<Integer> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Integer.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case LONG:{
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Long.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case FLOAT:{
                DataContainer<Float> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Float.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case DOUBLE:{
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Double.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case BOOLEAN:{
                DataContainer<Boolean> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Boolean.FALSE);
                if (null != ret && ret.valid) {
                    val = ret.value;
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
                if(null != res && res.valid) {
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
                DataContainer<Integer> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Integer.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }

                break;
            }

            case MESSAGE: {
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                if (dumpMessage(node, desc)) {
                    val = node.build();
                }
                break;
            }

            default:
                break;
        }

        if (null == val) {
            return false;
        }

        if (fd.isRepeated()) {
            builder.addRepeatedField(fd, val);
        } else {
            builder.setField(fd, val);
        }

        return true;
    }

    /**
     * 生成常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildConst() {
        if (false == load_pb_file(ProgramOptions.getInstance().protocolFile, false)) {
            return null;
        }

        if (null == pbs.enums) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();

        for(HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fdp : pbs.files.entrySet()) {
            String[] names = null;
            HashMap<String, Object> fd_root = ret;

            if(false == fdp.getValue().getPackage().isEmpty()) {
                names = fdp.getValue().getPackage().split("\\.");
            }

            if (null != names) {
                for(String seg: names) {
                    if (seg.isEmpty()) {
                        continue;
                    }
                    if(fd_root.containsKey(seg)) {
                        Object node = fd_root.get(seg);
                        if (node instanceof HashMap) {
                            fd_root = (HashMap<String, Object>)node;
                        } else {
                            ProgramOptions.getLoger().error("package name %s conflict(failed in %s).", fdp.getValue().getPackage(), seg);
                            break;
                        }
                    } else {
                        HashMap<String, Object> node = new HashMap<String, Object>();
                        fd_root.put(seg, node);
                        fd_root = node;
                    }
                }
            }

            for(DescriptorProtos.EnumDescriptorProto enum_desc : fdp.getValue().getEnumTypeList()) {
                String seg = enum_desc.getName();
                HashMap<String, Object> enum_root;
                if(fd_root.containsKey(seg)) {
                    Object node = fd_root.get(seg);
                    if (node instanceof HashMap) {
                        enum_root = (HashMap<String, Object>)node;
                    } else {
                        ProgramOptions.getLoger().error("enum name %s.%s conflict.", fdp.getValue().getPackage(), seg);
                        continue;
                    }
                } else {
                    enum_root = new HashMap<String, Object>();
                    fd_root.put(seg, enum_root);
                }

                // 写出所有常量值
                for(DescriptorProtos.EnumValueDescriptorProto enum_val : enum_desc.getValueList()) {
                    enum_root.put(enum_val.getName(), enum_val.getNumber());
                }
            }
        }

        return ret;
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) {
        // protobuf的常量输出直接复制描述文件就好了
        if(ProgramOptions.getInstance().protocolFile.equals(ProgramOptions.getInstance().constPrint)) {
            return null;
        }

        try {
            File f = new File(ProgramOptions.getInstance().protocolFile);

            FileInputStream fin = new FileInputStream(ProgramOptions.getInstance().protocolFile);
            byte[] all_buffer = new byte[(int)f.length()];
            fin.read(all_buffer);

            return all_buffer;
        } catch (FileNotFoundException e) {
            ProgramOptions.getLoger().error("protocol file %s not found.", ProgramOptions.getInstance().protocolFile);
        } catch (IOException e) {
            ProgramOptions.getLoger().error("read protocol file %s failed.", ProgramOptions.getInstance().protocolFile);
            e.printStackTrace();
        }

        return null;
    }
}
