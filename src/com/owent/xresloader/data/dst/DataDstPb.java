package com.owent.xresloader.data.dst;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;
import com.owent.xrexloader.pb.PbHeader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

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
            currentMsgDesc = fd.findMessageTypeByName("role_cfg");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("[ERROR] read protocol file \"" + ProgramOptions.getInstance().protocolFile + "\" failed." + e.toString());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
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
        PbHeader.xresloader_header.Builder header = PbHeader.xresloader_header.getDefaultInstance().toBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getVersion());

        return new byte[0];
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
}
