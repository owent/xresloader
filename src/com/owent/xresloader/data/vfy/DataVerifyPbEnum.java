package com.owent.xresloader.data.vfy;

import com.google.protobuf.DescriptorProtos;

/**
 * Created by owt50 on 2016/12/7.
 */
public class DataVerifyPbEnum extends DataVerifyImpl {
    public DataVerifyPbEnum(DescriptorProtos.EnumDescriptorProto desc) {
        super(desc.getName());

        for (DescriptorProtos.EnumValueDescriptorProto val_desc : desc.getValueList()) {
            all_names.put(val_desc.getName(), (long)val_desc.getNumber());
            all_numbers.add((long)val_desc.getNumber());
        }
    }
}
