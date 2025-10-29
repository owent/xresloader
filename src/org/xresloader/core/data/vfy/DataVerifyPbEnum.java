package org.xresloader.core.data.vfy;

import org.xresloader.Xresloader;
import com.google.protobuf.DescriptorProtos;

/**
 * Created by owent on 2016/12/7.
 */
public class DataVerifyPbEnum extends DataVerifyImpl {
    public DataVerifyPbEnum(DescriptorProtos.EnumDescriptorProto desc) {
        super(desc.getName());

        for (DescriptorProtos.EnumValueDescriptorProto val_desc : desc.getValueList()) {
            all_names.put(val_desc.getName(), (long) val_desc.getNumber());
            all_numbers.add((long) val_desc.getNumber());

            // alias extension
            if (val_desc.getOptions().getExtensionCount(Xresloader.enumAlias) > 0) {
                for (String alias_name : val_desc.getOptions().getExtension(Xresloader.enumAlias)) {
                    String alias_name_striped = alias_name.strip();
                    if (!alias_name_striped.isEmpty()) {
                        all_names.put(alias_name_striped, (long) val_desc.getNumber());
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
