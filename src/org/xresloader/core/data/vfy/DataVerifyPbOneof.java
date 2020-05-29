package org.xresloader.core.data.vfy;

import java.util.HashMap;
import java.util.LinkedList;

import org.xresloader.Xresloader;
import org.xresloader.core.data.dst.DataDstImpl;

import com.google.protobuf.DescriptorProtos;

/**
 * Created by owent on 2020/05/29.
 */
public class DataVerifyPbOneof extends DataVerifyImpl {
    private HashMap<String, LinkedList<DataVerifyImpl>> name_mapping = new HashMap<String, LinkedList<DataVerifyImpl>>();
    private HashMap<Long, LinkedList<DataVerifyImpl>> number_mapping = new HashMap<Long, LinkedList<DataVerifyImpl>>();
    private String seperator = ",;|";

    public DataVerifyPbOneof(DescriptorProtos.OneofDescriptorProto desc) {
        super(desc.getName());

        if (desc.getOptions().hasExtension(Xresloader.oneofSeparator)) {
            this.seperator = desc.getOptions().getExtension(Xresloader.oneofSeparator);
        }
    }

    public void addSubVerify(String name, DataVerifyImpl vfy) {
        if (name == null || vfy == null) {
            return;
        }

        if (name.isEmpty()) {
            return;
        }

        LinkedList<DataVerifyImpl> set = this.name_mapping.getOrDefault(name, null);
        if (set == null) {
            set = new LinkedList<DataVerifyImpl>();
            this.name_mapping.put(name, set);
        }

        set.add(vfy);
    }

    public void addSubVerify(Long index, DataVerifyImpl vfy) {
        if (index == 0 || vfy == null) {
            return;
        }

        LinkedList<DataVerifyImpl> set = this.number_mapping.getOrDefault(index, null);
        if (set == null) {
            set = new LinkedList<DataVerifyImpl>();
            this.number_mapping.put(index, set);
        }

        set.add(vfy);
    }

    @Override
    public boolean get(long number, DataVerifyResult res) {
        // oneof 必须走plain模式，所以不接受数字类型配置
        res.success = false;
        return false;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) {
        String[] split_groups = DataDstImpl.splitPlainGroups(input, this.seperator);
        if (split_groups.length < 2) {
            res.success = false;
            return false;
        }

        String oneof_type = split_groups[0].trim();
        if (oneof_type.isEmpty()) {
            res.success = false;
            return false;
        }

        // check if it's a number
        boolean is_int = true;
        for (int i = 0; is_int && i < oneof_type.length(); ++i) {
            char c = oneof_type.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
                is_int = false;
            }
        }

        LinkedList<DataVerifyImpl> sub_verifies = null;
        if (is_int) {
            sub_verifies = this.number_mapping.getOrDefault(Math.round(Double.valueOf(oneof_type)), null);
        } else {
            sub_verifies = this.name_mapping.getOrDefault(oneof_type, null);
        }

        if (null == sub_verifies) {
            res.success = false;
            return false;
        }

        if (sub_verifies.isEmpty()) {
            res.value = 0; // just store 0
            res.success = true;
            return true;
        }

        // any verify success is accepted
        for (DataVerifyImpl vfy : sub_verifies) {
            if (vfy.get(split_groups[1], res)) {
                return true;
            }
        }

        res.success = false;
        return false;
    }
}
