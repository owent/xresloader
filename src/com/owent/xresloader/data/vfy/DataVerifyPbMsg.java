package com.owent.xresloader.data.vfy;

import com.google.protobuf.DescriptorProtos;

/**
 * Created by owt50 on 2016/12/7.
 */
public class DataVerifyPbMsg extends DataVerifyImpl {
    public DataVerifyPbMsg(DescriptorProtos.DescriptorProto desc) {
        super(desc.getName());

        for (DescriptorProtos.FieldDescriptorProto fd : desc.getFieldList()) {
            all_names.put(fd.getName(), (long)fd.getNumber());
            all_numbers.add((long)fd.getNumber());
        }
    }
}
