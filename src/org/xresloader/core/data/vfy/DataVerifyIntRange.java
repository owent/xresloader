package org.xresloader.core.data.vfy;

import org.xresloader.core.ProgramOptions;

public class DataVerifyIntRange extends DataVerifyImpl {
    private long lowerBound = 0;
    private long upperBound = 0;

    public DataVerifyIntRange(String range) {
        super(range);

        if (range.isEmpty()) {
            return;
        }

        int split_pos = 1;
        for(; split_pos < range.length(); ++ split_pos) {
            if ('-' == range.charAt(split_pos)) {
                break;
            }
        }

        try {
            if (split_pos >= range.length()) {
                lowerBound = Math.round(Double.valueOf(range));
                upperBound = lowerBound + 1;
            }


            lowerBound = Math.round(Double.valueOf(range.substring(0, split_pos).trim()));
            if (split_pos + 1 < range.length()) {
                upperBound = Math.round(Double.valueOf(range.substring(split_pos + 1).trim())) + 1;
            } else {
                upperBound = lowerBound + 1;
            }
        } catch (NumberFormatException e) {
            ProgramOptions.getLoger().error("invalid integer range %s verifier", range);
        }
    }

    public boolean isValid() {
        return upperBound > lowerBound;
    }

    @Override
    public boolean get(long number, DataVerifyResult res)  {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (number >= lowerBound && number < upperBound) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String intstr, DataVerifyResult res) {
        // check if it's a number
        boolean is_int = true;
        for(int i = 0; is_int && i < intstr.length(); ++ i) {
            char c = intstr.charAt(i);
            if ((c < '0' || c > '9') &&
                    '.' != c &&
                    '-' != c) {
                is_int = false;
            }
        }

        if (is_int) {
            return get(Math.round(Double.valueOf(intstr)), res);
        }

        res.success = false;
        return false;
    }
}
