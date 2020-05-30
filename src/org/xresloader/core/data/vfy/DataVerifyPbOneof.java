package org.xresloader.core.data.vfy;

import java.util.HashMap;
import java.util.LinkedList;

import org.xresloader.Xresloader;
import org.xresloader.core.data.dst.DataDstImpl;

import com.google.protobuf.Descriptors;

/**
 * Created by owent on 2020/05/29.
 */
public class DataVerifyPbOneof extends DataVerifyImpl {
    public DataVerifyPbOneof(Descriptors.OneofDescriptor desc) {
        super(desc.getName());

        for (Descriptors.FieldDescriptor fd : desc.getFields()) {

            all_names.put(fd.getName(), (long) fd.getNumber());
            all_numbers.add((long) fd.getNumber());

            // alias extension
            if (fd.getOptions().hasExtension(Xresloader.fieldAlias)) {
                String alias_name = fd.getOptions().getExtension(Xresloader.fieldAlias);
                if (!alias_name.isEmpty()) {
                    all_names.put(alias_name, (long) fd.getNumber());
                }
            }
        }
    }
}
