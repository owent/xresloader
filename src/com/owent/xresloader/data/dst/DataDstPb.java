package com.owent.xresloader.data.dst;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.scheme.SchemeConf;
import com.owent.xrexloader.pb.PbHeader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private HashMap<String, Descriptors.FileDescriptor> desc_map = new HashMap<String, Descriptors.FileDescriptor>();
    private HashMap<String, DescriptorProtos.FileDescriptorProto> descp_map = new HashMap<String, DescriptorProtos.FileDescriptorProto>();
    private Descriptors.Descriptor currentMsgDesc = null;

    public boolean init() {
        desc_map.clear();
        descp_map.clear();

        try {
            InputStream fis = new FileInputStream(ProgramOptions.getInstance().protocolFile);

            DescriptorProtos.FileDescriptorSet fds = DescriptorProtos.FileDescriptorSet.parseFrom(fis);

            DescriptorProtos.FileDescriptorProto selected_fdp = null;
            for(DescriptorProtos.FileDescriptorProto fdp: fds.getFileList()) {
                descp_map.put(fdp.getName(), fdp);

                if (null != selected_fdp)
                    continue;
                for(DescriptorProtos.DescriptorProto dp: fdp.getMessageTypeList()) {
                    if(dp.getName().equals(SchemeConf.getInstance().getProtoName())) {
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

    public byte[] compile(DataDstImpl desc) {
        PbHeader.xresloader_header.Builder header = PbHeader.xresloader_header.getDefaultInstance().toBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getVersion());

        return new byte[0];
    }

    private Descriptors.FileDescriptor build_fd(String fdn) {
        Descriptors.FileDescriptor ret = desc_map.getOrDefault(fdn, null);
        if(null != ret)
            return ret;

        DescriptorProtos.FileDescriptorProto fdp = descp_map.get(fdn);
        try {
            Descriptors.FileDescriptor[] deps = get_deps(fdp);
            if(null == deps) {
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
        for(int i = 0; i < ret.length; ++ i) {
            ret[i] = build_fd(fdp.getDependency(i));
            if (null == ret[i])
                return null;
        }

        return ret;
    }
}
