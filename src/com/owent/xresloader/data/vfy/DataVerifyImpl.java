package com.owent.xresloader.data.vfy;

import com.owent.xresloader.ProgramOptions;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by owt50 on 2016/12/7.
 */
public abstract class DataVerifyImpl {
    protected HashMap<String, Integer> all_names = new HashMap<String, Integer>();
    protected HashSet<Integer> all_numbers = new HashSet<Integer>();
    protected String name = "";

    protected DataVerifyImpl(String _name) {
        name = _name;
    }

    public int get(int number) {
        // 0 值永久有效
        if (0 == number) {
            return number;
        }

        if (all_numbers.contains(number)) {
            return number;
        }
        ProgramOptions.getLoger().error("%s has no date with number = %d", name, number);
        return 0;
    }

    public int get(String enum_name) {
        if (null == enum_name || enum_name.isEmpty()) {
            return 0;
        }

        boolean is_int = false;
        for(int i = 0; i < enum_name.length(); ++ i) {
            if ((enum_name.charAt(i) >= '0' && enum_name.charAt(i) < '9') || '.' == enum_name.charAt(i)) {
                is_int = true;
            } else {
                is_int = false;
                break;
            }
        }

        if (is_int) {
            return get(Double.valueOf(enum_name).intValue());
        }

        Integer ret = all_names.getOrDefault(enum_name, null);
        if (null == ret) {
            ProgramOptions.getLoger().error("%s has no date with field name = %s", name, enum_name);
            return 0;
        }

        return ret;
    }
}
