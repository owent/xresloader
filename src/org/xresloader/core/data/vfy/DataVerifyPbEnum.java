package org.xresloader.core.data.vfy;

import org.xresloader.Xresloader;

import com.google.protobuf.DescriptorProtos;

/**
 * Created by owent on 2016/12/7.
 */
public class DataVerifyPbEnum extends DataVerifyImpl {
    public DataVerifyPbEnum(DescriptorProtos.EnumDescriptorProto desc) {
        super(desc.getName());

        for (DescriptorProtos.EnumValueDescriptorProto valDesc : desc.getValueList()) {
            allNames.put(valDesc.getName(), (long) valDesc.getNumber());
            allNumbers.add((long) valDesc.getNumber());

            // alias extension
            if (valDesc.getOptions().getExtensionCount(Xresloader.enumAlias) > 0) {
                for (String aliasName : valDesc.getOptions().getExtension(Xresloader.enumAlias)) {
                    String aliasNameStriped = aliasName.strip();
                    if (!aliasNameStriped.isEmpty()) {
                        allNames.put(aliasNameStriped, (long) valDesc.getNumber());
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
