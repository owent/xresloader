package com.owent.xresloader.data.vfy;

import com.owent.xresloader.ProgramOptions;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by owt50 on 2016/12/7.
 */

public abstract class DataVerifyImpl {
    protected HashMap<String, Long> all_names = new HashMap<String, Long>();
    protected HashSet<Long> all_numbers = new HashSet<Long>();
    protected String name = "";

    protected DataVerifyImpl(String _name) {
        name = _name;
    }

    public boolean get(long number, DataVerifyResult res) {
        // 0 值永久有效
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (all_numbers.contains((Long)number)) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    public boolean get(String enum_name, DataVerifyResult res) {
        if (null == enum_name || enum_name.isEmpty()) {
            res.success = true;
            res.value = 0;
            return res.success;
        }

        boolean is_int = true;
        for(int i = 0; is_int && i < enum_name.length(); ++ i) {
            char c = enum_name.charAt(i);
            if ((c < '0' || c > '9') &&
                    '.' != c &&
                    '-' != c) {
                is_int = false;
            }
        }

        if (is_int) {
            return get(Math.round(Double.valueOf(enum_name)), res);
        }

        Long ret = all_names.getOrDefault(enum_name, null);
        if (null == ret) {
            res.success = false;
            return false;
        }

        res.success = true;
        res.value = ret;
        return res.success;
    }

    public String toString() {
        return name;
    }
}
