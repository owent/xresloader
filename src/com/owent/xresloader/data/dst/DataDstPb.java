package com.owent.xresloader.data.dst;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.data.src.DataContainer;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.pb.PbHeader;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private Descriptors.Descriptor currentMsgDesc = null;

    private class DataEntry {
        public boolean valid =  false;
        public Object value = null;

        public <T> void set(DataContainer<T> v) {
            valid = v.valid;
            value = v.value;
        }
    }

    /***
     * protobuf 的描述信息生成是按文件的，所以要缓存并先生成依赖，再生成需要的文件描述数据
     */
    static private class PbInfoSet {
        public HashMap<String, Descriptors.FileDescriptor> desc_map = new HashMap<String, Descriptors.FileDescriptor>();
        public HashMap<String, DescriptorProtos.FileDescriptorProto> descp_map = new HashMap<String, DescriptorProtos.FileDescriptorProto>();
        public HashMap<String, String> proto_fd_map = new HashMap<String, String>();
        public String file_path = "";


        public Iterator<HashMap.Entry<String, DescriptorProtos.FileDescriptorProto>> descp_iter = null;
        public Iterator<DescriptorProtos.DescriptorProto> desc_iter = null;
        public String iter_file_name = "";
        public PbInfoSet(){}
    }

    static private PbInfoSet last_pb_data = null;
    static PbInfoSet load_pb_file(String file_path) {
        // 尽可能缓存已加载好的数据，防止重复加载
        if (null == last_pb_data || file_path != last_pb_data.file_path) {
            PbInfoSet new_pb_info = new PbInfoSet();
            new_pb_info.file_path = file_path;
            // 开始载入描述文件

            try {
                // 文件描述集读取
                InputStream fis = new FileInputStream(file_path);
                DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis);

                // 保存文件名和文件描述Proto的关系
                for (DescriptorProtos.FileDescriptorProto fdp : fds.getFileList()) {
                    new_pb_info.descp_map.put(fdp.getName(), fdp);
                }

                new_pb_info.descp_iter = new_pb_info.descp_map.entrySet().iterator();
            } catch (FileNotFoundException e) {
                System.err.println(String.format("[ERROR] read protocol file \"%s\" failed. %s", ProgramOptions.getInstance().protocolFile, e.toString()));
                return null;
            } catch (IOException e) {
                System.err.println(String.format("[ERROR] parse protocol file \"%s\" failed. %s", ProgramOptions.getInstance().protocolFile, e.toString()));
                return null;
            }

            // 载入完成,swap
            last_pb_data = new_pb_info;
        }

        return last_pb_data;
    }

    static Descriptors.Descriptor build_pb_file(String file_path, String proto_name) {
        // 尽可能缓存已加载好的数据，防止重复加载
        if (null == load_pb_file(file_path)) {
            return null;
        }

        // 缓存查不到，尝试走迭代器读取
        while (null != last_pb_data && !last_pb_data.proto_fd_map.containsKey(proto_name)) {
            boolean is_continue = true;

            while(is_continue) {
                if (null == last_pb_data.desc_iter) {
                    if (null == last_pb_data.descp_iter || !last_pb_data.descp_iter.hasNext()) {
                        is_continue = false;
                        break;
                    }

                    HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> iter_data = last_pb_data.descp_iter.next();
                    last_pb_data.iter_file_name = iter_data.getKey();
                    last_pb_data.desc_iter = iter_data.getValue().getMessageTypeList().iterator();
                }

                if (null == last_pb_data.desc_iter || !last_pb_data.desc_iter.hasNext()) {
                    last_pb_data.desc_iter = null;
                    continue;
                }

                DescriptorProtos.DescriptorProto dp = last_pb_data.desc_iter.next();
                last_pb_data.proto_fd_map.put(dp.getName(), last_pb_data.iter_file_name);

                // 找到则停止
                if(proto_name.equals(dp.getName())) {
                    is_continue = false;
                    break;
                }
            }

            break;
        }

        // 如果协议名称不存在与所有的文件描述中，直接退出
        if (null == last_pb_data || !last_pb_data.proto_fd_map.containsKey(proto_name)) {
            System.err.println(String.format("[ERROR] proto message name \"%s\" not found.", proto_name));
            return null;
        }

        Descriptors.FileDescriptor fd = build_fd(last_pb_data.proto_fd_map.get(proto_name), last_pb_data);
        if (null == fd) {
            System.err.println(String.format("[ERROR] build  protocol \"%s\" failed.", proto_name));
            return null;
        }

        return fd.findMessageTypeByName(SchemeConf.getInstance().getProtoName());
    }

    @Override
    public boolean init() {
        currentMsgDesc = build_pb_file(ProgramOptions.getInstance().protocolFile, SchemeConf.getInstance().getProtoName());
        return null != currentMsgDesc;
    }


    @Override
    public final DataDstWriterNode compile() {
        DataDstWriterNode ret = new DataDstWriterNode();
        if (test(ret, currentMsgDesc, new LinkedList<String>(), false))
            return ret;

        return null;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) throws ConvException {
        // 初始化header
        PbHeader.xresloader_datablocks.Builder blocks = PbHeader.xresloader_datablocks.newBuilder();
        PbHeader.xresloader_header.Builder header = blocks.getHeaderBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getVersion());
        header.setHashCode("");

        // 校验码
        MessageDigest md5 = null ;
        try {
            MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[ERROR] failed to find md5 algorithm.");
            header.setHashCode("");
        }

        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().next()) {
            ByteString data = convData(desc);
            if (null != data && !data.isEmpty()) {
                ++count;
                blocks.addDataBlock(data);

                if (null != md5) {
                    md5.update(data.toByteArray());
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
            PbHeader.xresloader_header builtHeader = header.build();
            blocks.build().writeTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(String.format("[ERROR] try to serialize protobuf header failed. %s", e.toString()));
            System.err.println(String.format("[ERROR] %s", header.build().getInitializationErrorString()));
        }
        return writer.toByteArray();
    }

    /***
     * 递归生成文件描述集
     * @param fdn 文件名
     * @param pb_info_set 数据来源和缓存目标
     * @return 需要的文件描述集
     */
    static private Descriptors.FileDescriptor build_fd(String fdn, PbInfoSet pb_info_set) {
        Descriptors.FileDescriptor ret = pb_info_set.desc_map.getOrDefault(fdn, null);
        if (null != ret)
            return ret;

        DescriptorProtos.FileDescriptorProto fdp = pb_info_set.descp_map.get(fdn);
        try {
            Descriptors.FileDescriptor[] deps = get_deps(fdp, pb_info_set);
            if (null == deps) {
                System.err.println(String.format("[ERROR] build protocol \"%s\" failed(dependency build failed).", fdn));
                return null;
            }

            ret = Descriptors.FileDescriptor.buildFrom(fdp, deps);
            pb_info_set.desc_map.put(fdn, ret); // 缓存所有的描述集

        } catch (Descriptors.DescriptorValidationException e) {
            e.printStackTrace();
            System.err.println(String.format("[ERROR] build protocol \"%s\" failed. %s", fdn, e.toString()));
            return null;
        }

        return ret;
    }

    static private Descriptors.FileDescriptor[] get_deps(DescriptorProtos.FileDescriptorProto fdp, PbInfoSet pb_info_set) {
        Descriptors.FileDescriptor[] ret = new Descriptors.FileDescriptor[fdp.getDependencyCount()];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = build_fd(fdp.getDependency(i), pb_info_set);
            if (null == ret[i])
                return null;
        }

        return ret;
    }


    /**
     * 测试并生成数据结构
     *
     * @param node      待填充的节点
     * @param desc      protobuf 结构描述信息
     * @param name_list 当前命名列表
     * @return 查找到对应的数据源映射关系并非空则返回true，否则返回false
     */
    private boolean test(DataDstWriterNode node, Descriptors.Descriptor desc, LinkedList<String> name_list, boolean is_list) {
        String prefix = String.join(".", name_list);
        boolean ret = true;
        boolean has_data = false;

        DataSrcImpl data_src = DataSrcImpl.getOurInstance();

        for (Descriptors.FieldDescriptor fd : desc.getFields()) {
            switch (fd.getJavaType()) {
                // 标准类型直接检测节点
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case STRING:
                case BYTE_STRING:
                case ENUM:
                    // list 类型
                    if (fd.isRepeated()) {
                        int count = 0;
                        for (; ; ++count) {
                            String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                            if (!data_src.checkName(real_name))
                                break;
                        }

                        has_data = has_data || count > 0;

                        if (has_data) {
                            DataDstWriterNode c = node.createChild(fd.getName());

                            if (count > c.getListCount()) {
                                c.setListCount(count);
                            }

                            c.setType(fd.getJavaType().toString());

                            node.addChild(fd.getName(), c);
                        }
                    } else {
                        // 非 list 类型
                        String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                        if (data_src.checkName(real_name)) {
                            DataDstWriterNode c = node.createChild(fd.getName());
                            c.setType(fd.getJavaType().toString());
                            node.addChild(fd.getName(), c);
                            has_data = true;

                        } else if (fd.isRequired()) {
                            // 非测试list长度的模式下才输出错误
                            if (false == is_list) {
                                System.err.println(String.format("[ERROR] required field \"%s\" not found", real_name));
                            }
                            ret = false;
                        }
                    }
                    break;


                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;
                        DataDstWriterNode c = node.createChild(fd.getName());

                        name_list.addLast("");
                        for (; ; ++count) {
                            name_list.removeLast();
                            name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName(), count));
                            if (!test(c, fd.getMessageType(), name_list, true))
                                break;
                        }
                        name_list.removeLast();
                        has_data = has_data || count > 0;

                        if (has_data) {
                            if (count > c.getListCount()) {
                                c.setListCount(count);
                            }

                            node.addChild(fd.getName(), c);
                        }
                    } else {
                        DataDstWriterNode c = node.createChild(fd.getName());
                        name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName()));
                        if (test(c, fd.getMessageType(), name_list, is_list)) {
                            node.addChild(fd.getName(), c);
                            has_data = true;
                        } else if (fd.isRequired()) {
                            // 非测试list长度的模式下才输出错误
                            if (false == is_list) {
                                System.err.println(String.format("[ERROR] required field \"%s\" not found", fd.getName()));
                            }
                            ret = false;
                        }
                        name_list.removeLast();
                    }
                    break;

                default:
                    if (fd.isRequired())
                        ret = false;
                    break;
            }
        }

        return ret && has_data;
    }


    private ByteString convData(DataDstWriterNode desc) throws ConvException {
        DynamicMessage.Builder root = DynamicMessage.newBuilder(currentMsgDesc);

        boolean valid_data = writeData(root, desc, currentMsgDesc, "");
        // 过滤空项
        if(!valid_data) {
            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            System.err.println(String.format("[ERROR] serialize failed. %s", root.getInitializationErrorString()));
            return null;
        }
    }


    private boolean writeData(DynamicMessage.Builder builder, DataDstWriterNode desc, Descriptors.Descriptor proto_desc, String prefix) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode> c : desc.getChildren().entrySet()) {
            String _name = DataDstWriterNode.makeNodeName(c.getKey());

            Descriptors.FieldDescriptor fd = proto_desc.findFieldByName(_name);
            if (null == fd) {
                // System.err.println("[WARNING] child name " + c.getKey() + " not found in protobuf description " + proto_desc.getFullName());
                // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                continue;
            }

            if (c.getValue().isList() && fd.isRepeated()) {
                for (int i = 0; i < c.getValue().getListCount(); ++i) {
                    String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey(), i);
                    DataEntry ele = writeOneData(c.getValue(), fd, new_prefix);
                    if (null != ele && (ele.valid || ProgramOptions.getInstance().enbleEmptyList)) {
                        builder.addRepeatedField(fd, ele.value);
                        ret = ret || ele.valid;
                    }
                }
            } else {
                String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey());
                DataEntry ele = writeOneData(c.getValue(), fd, new_prefix);

                if (null != ele && (ele.valid || fd.isRequired())) {
                    // 资源存在时要判定转换列表类型
                    if (c.getValue().isList() != fd.isRepeated()) {
                        throw new ConvException(
                            String.format(
                                "excel data %s%s is %s list but protocol description \"%s\" is %s repeated",
                                prefix.isEmpty()? prefix: prefix + ".",
                                c.getKey(),
                                c.getValue().isList()? "a": "not",
                                fd.getFullName(),
                                fd.isRepeated()? "": "not"
                            )
                        );
                    }

                    builder.setField(fd, ele.value);
                    ret = ret || ele.valid;
                }
            }

        }

        return ret;
    }

    private DataEntry writeOneData(DataDstWriterNode desc, Descriptors.FieldDescriptor fd, String prefix) throws ConvException {
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        DataEntry ret = new DataEntry();

        switch (fd.getJavaType()) {
            case INT: {
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Integer(0)));
                break;
            }

            case LONG:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Long(0)));
                break;
            }

            case FLOAT:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Float(0)));
                break;
            }

            case DOUBLE:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Double(0)));
                break;
            }

            case BOOLEAN:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, Boolean.FALSE));
                break;
            }

            case STRING: {
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, ""));
                break;
            }

            case BYTE_STRING: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(prefix, "");
                ret.valid = res.valid;
                if (null == encoding || encoding.isEmpty()) {
                    ret.value = com.google.protobuf.ByteString.copyFrom(res.value.getBytes());
                } else {
                    ret.value = com.google.protobuf.ByteString.copyFrom(res.value.getBytes(Charset.forName(encoding)));
                }
                break;
            }
            case ENUM: {
                DataContainer<Integer> res1 = DataSrcImpl.getOurInstance().getValue(prefix, new Integer(0));
                ret.valid = res1.valid;

                Descriptors.EnumValueDescriptor enum_val = fd.getEnumType().findValueByNumber(res1.value);
                if (null != enum_val) {
                    ret.value = enum_val;
                    return ret;
                }

                DataContainer<String> res2 = DataSrcImpl.getOurInstance().getValue(prefix, "");
                ret.valid = res2.valid;
                enum_val = fd.getEnumType().findValueByName(res2.value);

                if (null != enum_val) {
                    ret.value = enum_val;
                    return ret;
                }

                System.err.println(String.format("[ERROR] serialize failed. %s data error.", prefix));
                break;
            }

            case MESSAGE: {
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                ret.valid = writeData(node, desc, fd.getMessageType(), prefix);
                ret.value = node.build();
                break;
            }

            default:
                break;
        }

        return ret;
    }

    /**
     * 生成常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildConst() {
        if(null == load_pb_file(ProgramOptions.getInstance().protocolFile)) {
            return null;
        }

        if (null == last_pb_data) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<String, Object>();

        for(HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fdp : last_pb_data.descp_map.entrySet()) {
            String[] names = null;
            HashMap<String, Object> fd_root = ret;

            if(false == fdp.getValue().getPackage().isEmpty()) {
                names = fdp.getValue().getPackage().split("\\.");
            }

            if (null != names) {
                for(String seg: names) {
                    if(fd_root.containsKey(seg)) {
                        Object node = fd_root.get(seg);
                        if (node instanceof HashMap) {
                            fd_root = (HashMap<String, Object>)node;
                        } else {
                            System.err.println(String.format("[ERROR] package name %s conflict(failed in %s).", fdp.getValue().getPackage(), seg));
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
                        System.err.println(String.format("[ERROR] enum name %s.%s conflict.", fdp.getValue().getPackage(), seg));
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
        if(ProgramOptions.getInstance().protocolFile == ProgramOptions.getInstance().constPrint) {
            return null;
        }

        try {
            File f = new File(ProgramOptions.getInstance().protocolFile);

            FileInputStream fin = new FileInputStream(ProgramOptions.getInstance().protocolFile);
            byte[] all_buffer = new byte[(int)f.length()];
            fin.read(all_buffer);

            return all_buffer;
        } catch (FileNotFoundException e) {
            System.err.println(String.format("[ERROR] protocol file %s not found.", ProgramOptions.getInstance().protocolFile));
        } catch (IOException e) {
            System.err.println(String.format("[ERROR] read protocol file %s failed.", ProgramOptions.getInstance().protocolFile));
            e.printStackTrace();
        }

        return null;
    }
}
