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
                for (String aliasName : fd.getOptions().getExtension(Xresloader.fieldAlias)) {
                    String aliasNameStriped = aliasName.strip();
                    if (!aliasNameStriped.isEmpty()) {
                        allNames.put(aliasNameStriped, (long) fd.getNumber());
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
