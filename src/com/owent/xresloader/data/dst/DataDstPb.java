package com.owent.xresloader.data.dst;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;
import com.owent.xresloader.pb.PbHeader;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private HashMap<String, Descriptors.FileDescriptor> desc_map = new HashMap<String, Descriptors.FileDescriptor>();
    private HashMap<String, DescriptorProtos.FileDescriptorProto> descp_map = new HashMap<String, DescriptorProtos.FileDescriptorProto>();
    private Descriptors.Descriptor currentMsgDesc = null;

    @Override
    public boolean init() {
        desc_map.clear();
        descp_map.clear();

        try {
            InputStream fis = new FileInputStream(ProgramOptions.getInstance().protocolFile);

            DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis);

            DescriptorProtos.FileDescriptorProto selected_fdp = null;
            for (DescriptorProtos.FileDescriptorProto fdp : fds.getFileList()) {
                descp_map.put(fdp.getName(), fdp);

                if (null != selected_fdp)
                    continue;
                for (DescriptorProtos.DescriptorProto dp : fdp.getMessageTypeList()) {
                    if (dp.getName().equals(SchemeConf.getInstance().getProtoName())) {
                        selected_fdp = fdp;
                        break;
                    }
                }
            }

            Descriptors.FileDescriptor fd = build_fd(selected_fdp.getName());
            currentMsgDesc = fd.findMessageTypeByName(SchemeConf.getInstance().getProtoName());

        } catch (FileNotFoundException e) {
            System.err.println("[ERROR] read protocol file \"" + ProgramOptions.getInstance().protocolFile + "\" failed." + e.toString());
            return false;
        } catch (IOException e) {
            System.err.println("[ERROR] parse protocol file \"" + ProgramOptions.getInstance().protocolFile + "\" failed." + e.toString());
            return false;
        }

        return null != currentMsgDesc;
    }


    @Override
    public final DataDstWriterNode compile() {
        DataDstWriterNode ret = new DataDstWriterNode();
        if (test(ret, currentMsgDesc, new LinkedList<String>()))
            return ret;

        return null;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        // 初始化header
        PbHeader.xresloader_datablocks.Builder blocks = PbHeader.xresloader_datablocks.newBuilder();
        PbHeader.xresloader_header.Builder header = blocks.getHeaderBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getVersion());
        header.setHashCode("");

        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().next()) {
            ByteString data = convData(desc);
            if (null != data && !data.isEmpty()) {
                ++count;
                blocks.addDataBlock(data);
            }
        }
        header.setCount(count);

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(blocks.build().toByteArray());
            header.setHashCode("md5:" + Hex.encodeHexString(md5.digest()));
        } catch (NoSuchAlgorithmException e) {
            System.err.println("[ERROR] failed to find md5 algorithm.");
            header.setHashCode("");
        }

        // 写出
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        try {

            PbHeader.xresloader_header builtHeader = header.build();
            blocks.build().writeTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERROR] try to serialize protobuf header failed." + e.toString());
            System.err.println("[ERROR] " + header.build().getInitializationErrorString());
        }
        return writer.toByteArray();
    }

    private Descriptors.FileDescriptor build_fd(String fdn) {
        Descriptors.FileDescriptor ret = desc_map.getOrDefault(fdn, null);
        if (null != ret)
            return ret;

        DescriptorProtos.FileDescriptorProto fdp = descp_map.get(fdn);
        try {
            Descriptors.FileDescriptor[] deps = get_deps(fdp);
            if (null == deps) {
                System.err.println("[ERROR] build protocol \"" + fdn + "\" failed(dependency build failed).");
                return null;
            }

            ret = Descriptors.FileDescriptor.buildFrom(fdp, deps);
            desc_map.put(fdn, ret);

        } catch (Descriptors.DescriptorValidationException e) {
            e.printStackTrace();
            System.err.println("[ERROR] build protocol \"" + fdn + "\" failed." + e.toString());
            return null;
        }

        return ret;
    }

    private Descriptors.FileDescriptor[] get_deps(DescriptorProtos.FileDescriptorProto fdp) {
        Descriptors.FileDescriptor[] ret = new Descriptors.FileDescriptor[fdp.getDependencyCount()];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = build_fd(fdp.getDependency(i));
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
    private boolean test(DataDstWriterNode node, Descriptors.Descriptor desc, LinkedList<String> name_list) {
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

                        DataDstWriterNode c = new DataDstWriterNode();
                        c.setListCount(count);
                        c.setType(fd.getJavaType().toString());
                        node.addChild(fd.getName(), c);
                        has_data = has_data || count > 0;
                    } else {
                        // 非 list 类型
                        String real_name = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                        if (data_src.checkName(real_name)) {
                            DataDstWriterNode c = new DataDstWriterNode();
                            c.setType(fd.getJavaType().toString());
                            node.addChild(fd.getName(), c);
                            has_data = true;

                        } else if (fd.isRequired()) {
                            System.err.println("[ERROR] required field \"" + real_name + "\" not found");
                            ret = false;
                        }
                    }
                    break;


                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;
                        DataDstWriterNode c = new DataDstWriterNode();

                        name_list.addLast("");
                        for (; ; ++count) {
                            name_list.removeLast();
                            name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName(), count));
                            if (!test(c, fd.getMessageType(), name_list))
                                break;
                        }
                        name_list.removeLast();
                        has_data = has_data || count > 0;

                        c.setListCount(count);
                        node.addChild(fd.getName(), c);
                    } else {
                        DataDstWriterNode c = new DataDstWriterNode();
                        name_list.addLast(DataDstWriterNode.makeNodeName(fd.getName()));
                        if (test(c, fd.getMessageType(), name_list)) {
                            node.addChild(fd.getName(), c);
                            has_data = true;
                        } else if (fd.isRequired()) {
                            System.err.println("[ERROR] required field \"" + fd.getName() + "\" not found");
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


    private ByteString convData(DataDstWriterNode desc) {
        DynamicMessage.Builder root = DynamicMessage.newBuilder(currentMsgDesc);

        writeData(root, desc, currentMsgDesc, "");
        try {
            return root.build().toByteString();
        } catch (Exception e) {
            System.err.println("[ERROR] serialize failed." + root.getInitializationErrorString());
            return null;
        }
    }


    private void writeData(DynamicMessage.Builder builder, DataDstWriterNode desc, Descriptors.Descriptor proto_desc, String prefix) {
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
                    Object ele = writeOneData(c.getValue(), fd, new_prefix);
                    if (null != ele)
                        builder.addRepeatedField(fd, ele);
                }
            } else {
                String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey());
                Object ele = writeOneData(c.getValue(), fd, new_prefix);
                if (null != ele)
                    builder.setField(fd, ele);
            }

        }
    }

    private Object writeOneData(DataDstWriterNode desc, Descriptors.FieldDescriptor fd, String prefix) {
        String encoding = SchemeConf.getInstance().getKey().getEncoding();

        switch (fd.getJavaType()) {
            case INT:
                return new Integer(DataSrcImpl.getOurInstance().getValue(prefix, new Integer(0)).toString());

            case LONG:
                return DataSrcImpl.getOurInstance().getValue(prefix, new Long(0)).longValue();

            case FLOAT:
                return new Float(DataSrcImpl.getOurInstance().getValue(prefix, new Float(0)).toString());

            case DOUBLE:
                return DataSrcImpl.getOurInstance().getValue(prefix, new Double(0)).doubleValue();

            case BOOLEAN:
                return DataSrcImpl.getOurInstance().getValue(prefix, Boolean.FALSE);

            case STRING:
                return DataSrcImpl.getOurInstance().getValue(prefix, "");

            case BYTE_STRING:
                return com.google.protobuf.ByteString.copyFrom(
                    (null == encoding || encoding.isEmpty())?
                        DataSrcImpl.getOurInstance().getValue(prefix, "").getBytes():
                        DataSrcImpl.getOurInstance().getValue(prefix, "").getBytes(Charset.forName(encoding))
                );

            case ENUM:
                return fd.getEnumType().findValueByName(DataSrcImpl.getOurInstance().getValue(prefix, "")).getNumber();

            case MESSAGE:
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                writeData(node, desc, fd.getMessageType(), prefix);
                return node.build();

            default:
                return null;
        }
    }
}
