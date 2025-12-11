package org.xresloader.core.data.vfy;

import com.google.protobuf.DescriptorProtos;

public class DataVerifyPbEnumValue extends DataVerifyImpl {
    public DataVerifyPbEnumValue(DescriptorProtos.EnumValueDescriptorProto desc, String fullName) {
        super(desc.getName());

        long value = (long) desc.getNumber();
        allNames.put(desc.getName(), value);
        if (fullName != null && fullName.equals(desc.getName()) == false) {
            allNames.put(fullName, value);
        }
        allNumbers.add(value);

        // 别名
        for (String aliasName : desc.getOptions().getExtension(org.xresloader.Xresloader.enumAlias)) {
            String aliasNameStriped = aliasName.strip();
            if (!aliasNameStriped.isEmpty()) {
                allNames.put(aliasNameStriped, value);
            }
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
