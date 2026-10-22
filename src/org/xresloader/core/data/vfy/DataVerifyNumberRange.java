package org.xresloader.core.data.vfy;

import org.xresloader.core.ProgramOptions;

public class DataVerifyNumberRange extends DataVerifyImpl {
    private Number lowerBound = 0.0;
    private Number upperBound = 0.0;

    private static Number parseLowerBound(String value, boolean include) throws NumberFormatException {
        Double bound = Double.valueOf(value);
        boolean isDouble = false;
        if (value.indexOf('.') >= 0) {
            isDouble = true;
        } else if (bound > Long.MAX_VALUE || bound < Long.MIN_VALUE) {
            isDouble = true;
        }

        if (isDouble) {
            return include ? bound - Math.ulp(bound) : bound + Math.ulp(bound);
        } else {
            return include ? Long.valueOf(value) : Long.valueOf(Long.parseLong(value) + 1);
        }
    }

    private static Number parseUpperBound(String value, boolean include) throws NumberFormatException {
        Double bound = Double.valueOf(value);
        boolean isDouble = false;
        if (value.indexOf('.') >= 0) {
            isDouble = true;
        } else if (bound > Long.MAX_VALUE || bound < Long.MIN_VALUE) {
            isDouble = true;
        }

        if (isDouble) {
            return include ? bound + Math.ulp(bound) : bound - Math.ulp(bound);
        } else {
            return include ? Long.valueOf(value) : Long.valueOf(Long.parseLong(value) - 1);
        }
    }

    public DataVerifyNumberRange(String range) {
        this(range, range);
    }

    public DataVerifyNumberRange(String name, String range) {
        super(name);

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
                if (range.charAt(0) == '>') {
                    if (include) {
                        this.lowerBound = parseLowerBound(value, true);
                    } else {
                        this.lowerBound = parseLowerBound(value, false);
                    }
                    this.upperBound = Double.MAX_VALUE;
                } else {
                    if (include) {
                        this.upperBound = parseUpperBound(value, true);
                    } else {
                        this.upperBound = parseUpperBound(value, false);
                    }
                    this.lowerBound = Double.MIN_VALUE;
                }
            } catch (NumberFormatException e) {
                ProgramOptions.getLogger().error("Invalid integer range %s verifier", range);
            }
            return;
        }

        int splitPos = 1;
        for (; splitPos < range.length(); ++splitPos) {
            if ('-' == range.charAt(splitPos)) {
                break;
            }
        }

        try {
            if (splitPos >= range.length()) {
                this.lowerBound = parseLowerBound(range, true);
                this.upperBound = parseUpperBound(range, true);
            }

            this.lowerBound = parseLowerBound(range.substring(0, splitPos).trim(), true);
            if (splitPos + 1 < range.length()) {
                this.upperBound = parseUpperBound(range.substring(splitPos + 1).trim(), true);
            } else {
                this.upperBound = this.lowerBound;
            }
        } catch (NumberFormatException e) {
            ProgramOptions.getLogger().error("Invalid integer range %s verifier", range);
        }
    }

    @Override
    public boolean isValid() {
        if (upperBound instanceof Double || lowerBound instanceof Double) {
            return upperBound.doubleValue() >= lowerBound.doubleValue();
        }
        return upperBound.longValue() >= lowerBound.longValue();
    }

    @Override
    public boolean get(double number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (number >= lowerBound.doubleValue() && number <= upperBound.doubleValue()) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(long number, DataVerifyResult res) {
        // 0 值永久有效,因为空数据项会被填充默认值
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (lowerBound instanceof Double) {
            if (number < Math.round(lowerBound.doubleValue())) {
                res.success = false;
                return false;
            }
        } else if (number < lowerBound.longValue()) {
            res.success = false;
            return false;
        }

        if (upperBound instanceof Double) {
            if (number > Math.round(upperBound.doubleValue())) {
                res.success = false;
                return false;
            }
        } else if (number > upperBound.longValue()) {
            res.success = false;
            return false;
        }

        res.success = true;
        res.value = number;
        return res.success;
    }

    @Override
    public boolean get(String intstr, DataVerifyResult res) throws NumberFormatException {
        // check if it's a number
        boolean isNumeric = true;
        boolean isDouble = false;
        for (int i = 0; isNumeric && i < intstr.length(); ++i) {
            char c = intstr.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
                isNumeric = false;
            }
            if ('.' == c) {
                isDouble = true;
            }
        }
        if (isNumeric && !isDouble) {
            Double d = Double.valueOf(intstr);
            if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
                isDouble = true;
            }
        }

        if (isNumeric) {
            if (isDouble) {
                return get(Double.parseDouble(intstr), res);
            } else {
                return get(Long.parseLong(intstr), res);
            }
        }

        res.success = false;
        return false;
    }
}
