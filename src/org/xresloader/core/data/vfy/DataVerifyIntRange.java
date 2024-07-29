package org.xresloader.core.data.vfy;

import org.xresloader.core.ProgramOptions;

public class DataVerifyIntRange extends DataVerifyImpl {
    private double lowerBound = 0.0;
    private double upperBound = 0.0;

    public DataVerifyIntRange(String range) {
        super(range);

        if (range.isEmpty()) {
            return;
        }

        if (range.charAt(0) == '>' || range.charAt(0) == '<') {
            String value;
            boolean include = false;
            if (range.length() > 0 && range.charAt(1) == '=') {
                include = true;
                value = range.substring(2).trim();
            } else {
                value = range.substring(1).trim();
            }
            try {
                double bound = Double.valueOf(value).doubleValue();
                if (range.charAt(0) == '>') {
                    if (include) {
                        this.lowerBound = bound - Math.ulp(bound);
                    } else {
                        this.lowerBound = bound + Math.ulp(bound);
                    }
                    this.upperBound = Double.MAX_VALUE;
                } else {
                    if (include) {
                        this.upperBound = bound + Math.ulp(bound);
                    } else {
                        this.upperBound = bound - Math.ulp(bound);
                    }
                    this.lowerBound = Double.MIN_VALUE;
                }
            } catch (NumberFormatException e) {
                ProgramOptions.getLoger().error("Invalid integer range %s verifier", range);
            }
            return;
        }

        int split_pos = 1;
        for (; split_pos < range.length(); ++split_pos) {
            if ('-' == range.charAt(split_pos)) {
                break;
            }
        }

        try {
            if (split_pos >= range.length()) {
                double bound = Double.valueOf(range).doubleValue();
                this.lowerBound = bound - Math.ulp(bound);
                this.upperBound = bound + Math.ulp(bound);
            }

            double bound = Double.valueOf(range.substring(0, split_pos).trim()).doubleValue();
            this.lowerBound = bound - Math.ulp(bound);
            if (split_pos + 1 < range.length()) {
                bound = Double.valueOf(range.substring(split_pos + 1).trim()).doubleValue();
            }
            this.upperBound = bound + Math.ulp(bound);
        } catch (NumberFormatException e) {
            ProgramOptions.getLoger().error("Invalid integer range %s verifier", range);
        }
    }

    public boolean isValid() {
        return upperBound > lowerBound;
    }

    @Override
    public boolean get(double number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (number >= lowerBound && number <= upperBound) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String intstr, DataVerifyResult res) throws NumberFormatException {
        // check if it's a number
        boolean is_numeric = true;
        boolean is_double = false;
        for (int i = 0; is_numeric && i < intstr.length(); ++i) {
            char c = intstr.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
                is_numeric = false;
            }
            if ('.' == c) {
                is_double = true;
            }
        }

        if (is_numeric) {
            if (is_double) {
                return get(Double.valueOf(intstr), res);
            } else {
                return get(Long.valueOf(intstr), res);
            }
        }

        res.success = false;
        return false;
    }
}
