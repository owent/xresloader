package org.xresloader.core.data.vfy;

import org.xresloader.Xresloader;
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

            // alias extension
            if (fd.getOptions().hasExtension(Xresloader.fieldAlias)) {
                String alias_name = fd.getOptions().getExtension(Xresloader.fieldAlias);
                if (!alias_name.isEmpty()) {
                    all_names.put(alias_name, (long)fd.getNumber());    
                }
            }
        }
    }
}
