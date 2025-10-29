package org.xresloader.core.data.vfy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;

/**
 * Created by owent on 2016/12/7.
 */

public abstract class DataVerifyImpl {
    public enum ValidatorFailedLevel {
        WARNING(1), ERROR(2);

        private final int value;

        private ValidatorFailedLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    protected HashMap<String, Long> all_names = new HashMap<>();
    protected HashSet<Long> all_numbers = new HashSet<>();
    protected String name = "";

    private static ThreadLocal<Pattern> PERCENT_PATTERN = ThreadLocal
            .withInitial(() -> Pattern.compile("^\\s*((\\-\\s*)?[0-9\\,]+(\\.[0-9\\,]+)?)\\s*%\\s*$"));
    private static ThreadLocal<Pattern> INTEGER_WITH_DOT_PATTERN = ThreadLocal
            .withInitial(() -> Pattern.compile("^\\s*((\\-\\s*)?[0-9\\,]+)\\s*(%\\s*)?$"));

    protected DataVerifyImpl(String _name) {
        name = _name;
    }

    protected DataVerifyImpl(ValidatorTokens tokens) {
        name = tokens.name;
    }

    public int getVersion() {
        return 0;
    }

    abstract public boolean isValid();

    public boolean get(double number, DataVerifyResult res) {
        // 0 值永久有效
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (all_numbers.contains(Math.round(number))) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    public boolean get(long number, DataVerifyResult res) {
        // 0 值永久有效
        if (0 == number) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        if (all_numbers.contains(number)) {
            res.success = true;
            res.value = number;
            return res.success;
        }

        res.success = false;
        return false;
    }

    private static double doubleValueOf(String input) throws NumberFormatException {
        {
            Matcher matcher = INTEGER_WITH_DOT_PATTERN.get().matcher(input);
            if (matcher.matches()) {
                input = input.replaceAll(",", "").trim();
            }
        }

        Matcher matcher = PERCENT_PATTERN.get().matcher(input);
        if (matcher.matches()) {
            return Double.parseDouble(matcher.group(1).trim()) / 100.0;
        }
        return Double.parseDouble(input);
    }

    private static long longValueOf(String input) throws NumberFormatException {
        long ret;
        {
            Matcher matcher = INTEGER_WITH_DOT_PATTERN.get().matcher(input);
            if (matcher.matches()) {
                input = input.replaceAll(",", "").trim();
            }
        }

        Matcher matcher = PERCENT_PATTERN.get().matcher(input);
        if (matcher.matches()) {
            ret = Long.parseLong(matcher.group(1).trim());
            if (ret % 100 != 0) {
                throw new NumberFormatException(
                        String.format("The number part of %s can not be devided by 100", input));
            }
        } else {
            ret = Long.parseLong(input);
        }

        return ret;
    }

    public boolean get(String enum_name, DataVerifyResult res) throws NumberFormatException {
        if (null == enum_name || enum_name.isEmpty()) {
            res.success = true;
            res.value = 0;
            return res.success;
        }

        boolean is_numeric = true;
        boolean is_double = false;
        for (int i = 0; is_numeric && i < enum_name.length(); ++i) {
            char c = enum_name.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c && ',' != c) {
                if (!PERCENT_PATTERN.get().matcher(enum_name).matches()) {
                    is_numeric = false;
                }
            }
            if ('.' == c) {
                is_double = true;
            }
        }

        if (is_numeric) {
            if (is_double) {
                return get(doubleValueOf(enum_name), res);
            } else {
                return get(longValueOf(enum_name), res);
            }
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

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return getName();
    }

    static public String collectValidatorNames(List<DataVerifyImpl> verifyEngine) {
        StringBuilder sb = new StringBuilder();
        for (DataVerifyImpl vfy : verifyEngine) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(vfy.getDescription());
        }

        return sb.toString();
    }

    static public long getAndVerify(List<DataVerifyImpl> verifyEngine, String path, int n) throws ConvException {
        return getAndVerify(verifyEngine, path, (long) n);
    }

    static public long getAndVerify(List<DataVerifyImpl> verifyEngine, String path, long n) throws ConvException {
        // 不能直接调用double版本，会发生数据裁剪
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            return n;
        }

        DataVerifyResult verify_cache = new DataVerifyResult();
        for (DataVerifyImpl vfy : verifyEngine) {
            try {
                if (vfy.get(n, verify_cache)) {
                    if (verify_cache.value == null) {
                        return 0;
                    }
                    if (verify_cache.value instanceof Double) {
                        return Math.round((Double) verify_cache.value);
                    }
                    if (verify_cache.value instanceof Long) {
                        return ((Long) verify_cache.value).longValue();
                    }
                    return longValueOf(verify_cache.value.toString());
                }
            } catch (Exception e) {
                String message = String.format("Check %d for %s with validator %s failed, %s", n, path,
                        vfy.getDescription(), e.getMessage());
                if (getValidatorFailedLevel(vfy) == ValidatorFailedLevel.ERROR) {
                    throw new ConvException(
                            message);
                } else {
                    ProgramOptions.getLoger().warn(message);
                }
            }
        }

        String message = String.format("Check %d for %s with %s %s failed, check data failed.", n, path,
                getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine));
        if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
            throw new ConvException(
                    message);
        } else {
            ProgramOptions.getLoger().warn(message);
            return n;
        }
    }

    static private String getValidatorWord(List<DataVerifyImpl> verifyEngine) {
        if (verifyEngine == null || verifyEngine.isEmpty() || verifyEngine.size() == 1) {
            return "validator";
        }

        return "validator(s)";
    }

    static private ValidatorFailedLevel getValidatorFailedLevel(DataVerifyImpl verifyEngine) {
        if (!ProgramOptions.getInstance().enableDataValidator) {
            return ValidatorFailedLevel.WARNING;
        }

        if (verifyEngine == null) {
            return ValidatorFailedLevel.ERROR;
        }

        // version: 0 means always make failed as a error
        if (0 == verifyEngine.getVersion()) {
            return ValidatorFailedLevel.ERROR;
        }

        int noErrorVersion = ProgramOptions.getInstance().dataValidatorNoErrorVersion;
        if (noErrorVersion <= 0) {
            return ValidatorFailedLevel.ERROR;
        }

        if (verifyEngine.getVersion() >= noErrorVersion) {
            return ValidatorFailedLevel.WARNING;
        }
        return ValidatorFailedLevel.ERROR;
    }

    static private ValidatorFailedLevel getValidatorFailedLevel(List<DataVerifyImpl> verifyEngine) {
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            if (!ProgramOptions.getInstance().enableDataValidator) {
                return ValidatorFailedLevel.WARNING;
            }
            return ValidatorFailedLevel.ERROR;
        }

        ValidatorFailedLevel ret = ValidatorFailedLevel.ERROR;
        for (var vfy : verifyEngine) {
            ValidatorFailedLevel level = getValidatorFailedLevel(vfy);
            if (level.getValue() < ret.getValue()) {
                ret = level;
            }
        }

        return ret;
    }

    static public double getAndVerify(List<DataVerifyImpl> verifyEngine, String path, double n) throws ConvException {
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            return n;
        }

        DataVerifyResult verify_cache = new DataVerifyResult();

        for (DataVerifyImpl vfy : verifyEngine) {
            try {
                if (vfy.get(n, verify_cache)) {
                    if (verify_cache.value == null) {
                        return 0;
                    }
                    if (verify_cache.value instanceof Double) {
                        return (double) verify_cache.value;
                    }
                    if (verify_cache.value instanceof Long) {
                        return ((Long) verify_cache.value).doubleValue();
                    }
                    return doubleValueOf(verify_cache.value.toString());
                }
            } catch (Exception e) {
                String value;
                if (n == (long) n) {
                    value = String.format("%d", (long) n);
                } else {
                    value = String.format("%g", n);
                }
                String message = String.format("Check %s for %s with validator %s failed, %s", value, path,
                        vfy.getDescription(), e.getMessage());
                if (getValidatorFailedLevel(vfy) == ValidatorFailedLevel.ERROR) {
                    throw new ConvException(
                            message);
                } else {
                    ProgramOptions.getLoger().warn(message);
                }
            }
        }

        String value;
        if (n == (long) n) {
            value = String.format("%d", (long) n);
        } else {
            value = String.format("%g", n);
        }

        String message = String.format("Check %s for %s with %s %s failed, check data failed.", value, path,
                getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine));
        if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
            throw new ConvException(
                    message);
        } else {
            ProgramOptions.getLoger().warn(message);
            return n;
        }
    }

    static public Number getAndVerifyToNumber(List<DataVerifyImpl> verifyEngine, String path, String val,
            boolean is_double)
            throws ConvException {
        boolean is_numeric = true;
        boolean has_dot = false;
        for (int i = 0; is_numeric && i < val.length(); ++i) {
            char c = val.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c && ',' != c) {
                if (!PERCENT_PATTERN.get().matcher(val).matches()) {
                    is_numeric = false;
                }
            }
            if ('.' == c) {
                has_dot = true;
            }
        }
        if (!is_double && is_numeric && has_dot) {
            is_double = true;
        }
        if (!is_double && is_numeric) {
            double test_range = doubleValueOf(val);
            if (test_range > Long.MAX_VALUE || test_range < Long.MIN_VALUE) {
                is_double = true;
            }
        }

        try {
            if (is_numeric) {
                if (is_double) {
                    return Double.valueOf(getAndVerify(verifyEngine, path, doubleValueOf(val)));
                } else {
                    return Long.valueOf(getAndVerify(verifyEngine, path, longValueOf(val)));
                }
            }

            if (verifyEngine == null || verifyEngine.isEmpty()) {
                if (is_double) {
                    return Double.valueOf(doubleValueOf(val));
                } else {
                    return Long.valueOf(longValueOf(val));
                }
            }

            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
                try {
                    if (vfy.get(val, verify_cache)) {
                        if (verify_cache.value == null) {
                            return 0;
                        }
                        if (verify_cache.value instanceof Double) {
                            return (Double) verify_cache.value;
                        }
                        if (verify_cache.value instanceof Long) {
                            return (Long) verify_cache.value;
                        }
                        if (is_double) {
                            return Double.valueOf(doubleValueOf(verify_cache.value.toString()));
                        } else {
                            return Long.valueOf(longValueOf(verify_cache.value.toString()));
                        }
                    }
                } catch (Exception e) {
                    String message = String.format("Check %s for %s with validator %s failed, %s", val, path,
                            vfy.getDescription(), e.getMessage());
                    if (getValidatorFailedLevel(vfy) == ValidatorFailedLevel.ERROR) {
                        throw new ConvException(
                                message);
                    } else {
                        ProgramOptions.getLoger().warn(message);
                    }
                }
            }
        } catch (Exception e) {
            String message = String.format("Convert %s for %s failed, %s", val, path, e.getMessage());
            if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
                throw new ConvException(
                        message);
            } else {
                ProgramOptions.getLoger().warn(message);
            }
        }

        String message;
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            message = String.format("Convert %s for %s, check data failed.", val,
                    path);
        } else {
            message = String.format("Convert %s for %s with %s %s failed, check data failed.", val,
                    path, getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine));
        }
        if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
            throw new ConvException(message);
        } else {
            ProgramOptions.getLoger().warn(message);
            if (is_double) {
                return Double.valueOf(0);
            } else {
                return Long.valueOf(0);
            }
        }
    }

    static public double getAndVerifyToDouble(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        Number value = getAndVerifyToNumber(verifyEngine, path, val, true);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else {
            return 0.0;
        }
    }

    static public String getAndVerifyToString(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            return val;
        }

        DataVerifyResult verify_cache = new DataVerifyResult();

        for (DataVerifyImpl vfy : verifyEngine) {
            try {
                if (vfy.get(val, verify_cache)) {
                    if (verify_cache.value == null) {
                        return "";
                    }
                    if (verify_cache.value instanceof Double) {
                        String value;
                        if ((double) verify_cache.value == (long) ((double) verify_cache.value)) {
                            value = String.format("%d", (long) ((double) verify_cache.value));
                        } else {
                            value = String.format("%g", (double) verify_cache.value);
                        }
                        return value;
                    }
                    if (verify_cache.value instanceof Long) {
                        return verify_cache.value.toString();
                    }
                    return verify_cache.value.toString();
                }
            } catch (Exception e) {
                String message = String.format("Check %s for %s with validator %s failed, %s", val, path,
                        vfy.getDescription(), e.getMessage());
                if (getValidatorFailedLevel(vfy) == ValidatorFailedLevel.ERROR) {
                    throw new ConvException(
                            message);
                } else {
                    ProgramOptions.getLoger().warn(message);
                }
            }
        }

        String message;
        if (verifyEngine.isEmpty()) {
            message = String.format("Convert %s for %s failed, check data failed.", val,
                    path);
        } else {
            message = String.format("Convert %s for %s with %s %s failed, check data failed.", val,
                    path, getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine));
        }
        if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
            throw new ConvException(message);
        } else {
            ProgramOptions.getLoger().warn(message);
            return "";
        }
    }

    static public long getAndVerifyToLong(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        Number value = getAndVerifyToNumber(verifyEngine, path, val, false);
        long ret;
        if (value instanceof Double) {
            ret = Math.round(value.doubleValue());
            if (Math.abs(value.doubleValue() - (double) ret) > 1e-6) {
                String message;
                if (verifyEngine == null || verifyEngine.isEmpty()) {
                    message = String.format("Convert %s for %s failed, not a integer.", val,
                            path);
                } else {
                    message = String.format("Convert %s for %s with %s %s failed, not a integer.", val,
                            path, getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine));
                }
                if (getValidatorFailedLevel(verifyEngine) == ValidatorFailedLevel.ERROR) {
                    throw new ConvException(message);
                } else {
                    ProgramOptions.getLoger().warn(message);
                }
            }
        } else if (value instanceof Long) {
            ret = value.longValue();
        } else {
            ret = 0;
        }
        return ret;
    }

    static public class ValidatorTokens {
        public String name = "";
        public ArrayList<String> parameters = new ArrayList<>();

        public ValidatorTokens() {
        }

        public boolean initialize() {
            if (this.parameters.isEmpty()) {
                return false;
            }

            // Special mode(>NUM,>=NUM,<NUM,<=NUM,LOW-HIGH)
            if (this.parameters.size() == 1) {
                char firstChar = this.parameters.get(0).charAt(0);
                if (firstChar == '>' || firstChar == '<' || firstChar == '-' || firstChar >= '0' || firstChar <= '9') {
                    this.name = this.parameters.get(0).replaceAll("\\s+", "");
                } else {
                    this.name = this.parameters.get(0);
                }

                return this.name.length() > 0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(this.parameters.get(0));
            sb.append("(\"");
            for (int i = 1; i < this.parameters.size(); ++i) {
                sb.append(this.parameters.get(i).replace("\\", "\\\\").replace("\"", "\\\""));
                if (i + 1 != this.parameters.size()) {
                    sb.append("\",\"");
                }
            }
            sb.append("\")");
            this.name = sb.toString();
            return this.name.length() > 0;
        }
    }

    static private ValidatorTokens appendValidator(ValidatorTokens previous, String param, boolean stringMode) {
        if (!stringMode) {
            param = param.strip();
            if (param.isEmpty()) {
                return previous;
            }
        }

        if (null == previous) {
            previous = new ValidatorTokens();
        }
        previous.parameters.add(param.strip());
        return previous;
    }

    static public LinkedList<ValidatorTokens> buildValidators(String verifier) {
        if (null == verifier) {
            return null;
        }

        String stripedVerifer = verifier.trim();
        if (stripedVerifer.isEmpty()) {
            return null;
        }

        LinkedList<ValidatorTokens> ret = new LinkedList<>();
        int start = 0;
        int end = 0;
        char stringMark = 0;
        ValidatorTokens current = null;
        boolean startParameter = false;
        for (; end < stripedVerifer.length(); ++end) {
            char c = stripedVerifer.charAt(end);

            // Close string
            if (stringMark != 0) {
                if (c == stringMark) {
                    if (end > start) {
                        current = appendValidator(current, stripedVerifer.substring(start, end), true);
                    } else {
                        current = appendValidator(current, "", true);
                    }
                    start = end + 1;
                    stringMark = 0;
                }

                continue;
            }

            // Start string
            if (c == '"' || c == '\'') {
                if (end > start) {
                    current = appendValidator(current, stripedVerifer.substring(start, end), false);
                }

                start = end + 1;
                stringMark = c;
                continue;
            }

            if (c == '|') {
                if (end > start) {
                    current = appendValidator(current, stripedVerifer.substring(start, end), false);
                }

                if (current != null) {
                    if (current.initialize()) {
                        ret.add(current);
                    }
                }

                current = null;
                start = end + 1;
                continue;
            }

            if (startParameter) {
                if (c == ')' || c == ',') {
                    if (end > start) {
                        current = appendValidator(current, stripedVerifer.substring(start, end), false);
                    }

                    if (c == ')') {
                        startParameter = false;
                    }
                    start = end + 1;
                }
            } else {
                if (c == '(') {
                    if (end > start) {
                        current = appendValidator(current, stripedVerifer.substring(start, end), false);
                    }
                    startParameter = true;
                    start = end + 1;
                }
            }
        }

        if (start < stripedVerifer.length()) {
            current = appendValidator(current, stripedVerifer.substring(start), false);
        }
        if (current != null) {
            if (current.initialize()) {
                ret.add(current);
            }
        }

        return ret;
    }
}
