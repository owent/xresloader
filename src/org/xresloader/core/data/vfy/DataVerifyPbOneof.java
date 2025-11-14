package org.xresloader.core.data.vfy;

import org.xresloader.Xresloader;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

/**
 * Created by owent on 2020/05/29.
 */
public class DataVerifyPbOneof extends DataVerifyImpl {
    public DataVerifyPbOneof(Descriptors.OneofDescriptor desc) {
        super(desc.getName());

        for (Descriptors.FieldDescriptor fd : desc.getFields()) {

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

    // If oneof verifier is refer by other fields, it's not built and should be
    // built by origin protos
    public DataVerifyPbOneof(DescriptorProtos.OneofDescriptorProto desc, DescriptorProtos.DescriptorProto msgDesc) {
        super(desc.getName());

        int oneof_index = -1;
        int check_index = 0;
        for (DescriptorProtos.OneofDescriptorProto fd : msgDesc.getOneofDeclList()) {
            if (fd == desc || fd.getName().equals(desc.getName())) {
                oneof_index = check_index;
                break;
            }

            check_index = check_index + 1;
        }

        for (DescriptorProtos.FieldDescriptorProto fd : msgDesc.getFieldList()) {
            if (!fd.hasOneofIndex()) {
                continue;
            }

            if (fd.getOneofIndex() != oneof_index) {
                continue;
            }

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
