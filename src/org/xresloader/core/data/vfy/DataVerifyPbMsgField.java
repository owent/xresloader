package org.xresloader.core.data.vfy;

import org.xresloader.Xresloader;

import com.google.protobuf.DescriptorProtos;

/**
 * Created by owent on 2016/12/7.
 */
public class DataVerifyPbMsgField extends DataVerifyImpl {
    public DataVerifyPbMsgField(DescriptorProtos.DescriptorProto desc) {
        super(desc.getName());

        for (DescriptorProtos.FieldDescriptorProto fd : desc.getFieldList()) {
            allNames.put(fd.getName(), (long) fd.getNumber());
            allNumbers.add((long) fd.getNumber());

            // alias extension
            if (fd.getOptions().getExtensionCount(Xresloader.fieldAlias) > 0) {
                for (String alias_name : fd.getOptions().getExtension(Xresloader.fieldAlias)) {
                    String alias_name_striped = alias_name.strip();
                    if (!alias_name_striped.isEmpty()) {
                        allNames.put(alias_name_striped, (long) fd.getNumber());
                    }
                }
            }
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
