package org.xresloader.core.data.vfy;

import java.util.List;

import org.xresloader.core.ProgramOptions;

public class DataVerifyInValues extends DataVerifyImpl {
    public DataVerifyInValues(List<String> values) {
        super(String.format("InValues(%s)", String.join(",", values)));

        for (String value : values) {
            String stripValue = value.trim();

            boolean isNumber = true;
            for (int i = 0; i < stripValue.length(); ++i) {
                char c = stripValue.charAt(i);
                if (!Character.isDigit(c) && c != '-' && c != '+') {
                    isNumber = false;
                    break;
                }
            }

            long numValue = 0;
            if (isNumber) {
                try {
                    numValue = Long.parseLong(stripValue);
                    this.allNumbers.add(numValue);
                } catch (NumberFormatException e) {
                    ProgramOptions.getLoger().warn(
                            String.format("DataVerifyInValues: value '%s' is not a valid number, treat as string\n",
                                    stripValue));
                }
            }
            this.allNames.put(value, numValue);
        }
    }

    @Override
    public boolean isValid() {
        return !this.allNames.isEmpty();
    }

    @Override
    public boolean get(double number, DataVerifyResult res) {
        String value;
        if (number == (long) number) {
            value = String.format("%d", (long) number);
        } else {
            value = String.format("%g", number);
        }

        if (this.allNames.containsKey(value)) {
            res.success = true;

            // 如果已有值，不要改。可能被其他验证器做过转换
            if (res.value == null) {
                res.value = number;
            }
            return true;
        }

        if (res.value != null) {
            if (res.value instanceof String) {
                String strValue = (String) res.value;
                return this.getString(strValue, res, false);
            }

            if (res.value instanceof Long || res.value instanceof Integer || res.value instanceof Short) {
                String strValue = res.value.toString();
                return this.getString(strValue, res, false);
            }

            if (res.value instanceof Double || res.value instanceof Float) {
                return this.getString(String.format("%g", res.value), res, false);
            }
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(long number, DataVerifyResult res) {
        if (this.allNumbers.contains(number)) {
            res.success = true;

            // 如果已有值，不要改。可能被其他验证器做过转换
            if (res.value == null) {
                res.value = number;
            }
            return true;
        }

        if (res.value != null) {
            if (res.value instanceof String) {
                String strValue = (String) res.value;
                return this.getString(strValue, res, false);
            }

            if (res.value instanceof Long || res.value instanceof Integer || res.value instanceof Short) {
                String strValue = res.value.toString();
                return this.getString(strValue, res, false);
            }

            if (res.value instanceof Double || res.value instanceof Float) {
                return this.getString(String.format("%g", res.value), res, false);
            }
        }

        res.success = false;
        return false;
    }

    @Override
    public boolean get(String input, DataVerifyResult res) throws NumberFormatException {
        return getString(input, res, true);
    }

    public boolean getString(String input, DataVerifyResult res, boolean checkResult) throws NumberFormatException {
        if (this.allNames.containsKey(input)) {
            res.success = true;

            // 如果已有值，不要改。可能被其他验证器做过转换
            if (res.value == null) {
                res.value = input;
            }
            return true;
        }

        // 转换值检测
        if (checkResult && res.value != null) {
            if (this.allNames.containsKey(res.value.toString())) {
                res.success = true;
                return true;
            }
        }

        res.success = false;
        return false;
    }
}
