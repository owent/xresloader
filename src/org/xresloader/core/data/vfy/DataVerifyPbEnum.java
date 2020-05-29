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
            if (val_desc.getOptions().hasExtension(Xresloader.enumAlias)) {
                String alias_name = val_desc.getOptions().getExtension(Xresloader.enumAlias);
                if (!alias_name.isEmpty()) {
                    all_names.put(alias_name, (long) val_desc.getNumber());
                }
            }
        }
    }
}
