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

    protected HashMap<String, Long> allNames = new HashMap<>();
    protected HashSet<Long> allNumbers = new HashSet<>();
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

        if (allNumbers.contains(Math.round(number))) {
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

        if (allNumbers.contains(number)) {
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

        Long ret = allNames.getOrDefault(enum_name, null);
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

    static private int getAndVerifyTypeValidator(DataVerifyImpl typeValidator,
            String path, int n, DataVerifyResult verifyCache, boolean testMode) throws ConvException {
        return (int) getAndVerifyTypeValidator(typeValidator, path, (long) n, verifyCache, testMode);
    }

    static private long getAndVerifyTypeValidator(DataVerifyImpl typeValidator,
            String path, long n, DataVerifyResult verifyCache, boolean testMode) throws ConvException {
        if (typeValidator == null || !typeValidator.isValid()) {
            return n;
        }

        if (!typeValidator.get(n, verifyCache)) {
            if (testMode) {
                return n;
            }
            String message = String.format("Check %d for %s with type validator %s failed, check data failed.", n,
                    path, typeValidator.getName());
            if (getValidatorFailedLevel(typeValidator) == ValidatorFailedLevel.ERROR) {
                throw new ConvException(
                        message);
            } else {
                ProgramOptions.getLoger().warn(message);
                return n;
            }
        }

        if (verifyCache.value == null) {
            return n;
        }
        if (verifyCache.value instanceof Double) {
            return Math.round((Double) verifyCache.value);
        }
        if (verifyCache.value instanceof Long) {
            return ((Long) verifyCache.value);
        }

        return longValueOf(verifyCache.value.toString());
    }

    static private double getAndVerifyTypeValidator(DataVerifyImpl typeValidator,
            String path, double n, DataVerifyResult verifyCache, boolean testMode) throws ConvException {
        if (typeValidator == null || !typeValidator.isValid()) {
            return n;
        }

        if (!typeValidator.get(n, verifyCache)) {
            if (testMode) {
                return n;
            }
            String message = String.format("Check %f for %s with type validator %s failed, check data failed.", n,
                    path, typeValidator.getName());
            if (getValidatorFailedLevel(typeValidator) == ValidatorFailedLevel.ERROR) {
                throw new ConvException(
                        message);
            } else {
                ProgramOptions.getLoger().warn(message);
                return n;
            }
        }

        if (verifyCache.value == null) {
            return n;
        }
        if (verifyCache.value instanceof Double) {
            return (double) verifyCache.value;
        }
        if (verifyCache.value instanceof Long) {
            return ((Long) verifyCache.value).doubleValue();
        }
        return doubleValueOf(verifyCache.value.toString());
    }

    static private String getAndVerifyTypeValidator(DataVerifyImpl typeValidator,
            String path, String val, DataVerifyResult verifyCache, boolean testMode) throws ConvException {
        if (typeValidator == null || !typeValidator.isValid()) {
            return val;
        }

        if (!typeValidator.get(val, verifyCache)) {
            if (testMode) {
                return val;
            }
            String message = String.format("Check %s for %s with type validator %s failed, check data failed.", val,
                    path, typeValidator.getName());
            if (getValidatorFailedLevel(typeValidator) == ValidatorFailedLevel.ERROR) {
                throw new ConvException(
                        message);
            } else {
                ProgramOptions.getLoger().warn(message);
                return val;
            }
        }

        if (verifyCache.value == null) {
            return val;
        }

        return verifyCache.value.toString();
    }

    static public long getAndVerifyNumeric(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path, int n)
            throws ConvException {
        return getAndVerifyNumeric(verifyEngine, typeValidator, path, (long) n);
    }

    @SuppressWarnings("UseSpecificCatch")
    static public long getAndVerifyNumeric(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path, long n) throws ConvException {
        // 不能直接调用double版本，会发生数据裁剪
        if ((verifyEngine == null || verifyEngine.isEmpty()) && typeValidator == null) {
            return n;
        }

        DataVerifyResult verifyCache = new DataVerifyResult();
        if (verifyEngine != null && !verifyEngine.isEmpty()) {
            for (DataVerifyImpl vfy : verifyEngine) {
                try {
                    if (vfy.get(n, verifyCache)) {
                        if (verifyCache.value == null) {
                            return getAndVerifyTypeValidator(typeValidator, path, 0, verifyCache, false);
                        }
                        if (verifyCache.value instanceof Double) {
                            return getAndVerifyTypeValidator(typeValidator, path,
                                    Math.round((Double) verifyCache.value), verifyCache, false);
                        }
                        if (verifyCache.value instanceof Long) {
                            return getAndVerifyTypeValidator(typeValidator, path, ((Long) verifyCache.value),
                                    verifyCache, false);
                        }
                        return getAndVerifyTypeValidator(typeValidator, path,
                                longValueOf(verifyCache.value.toString()), verifyCache, false);
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
        } else {
            return getAndVerifyTypeValidator(typeValidator, path, n, verifyCache, false);
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

    @SuppressWarnings("UseSpecificCatch")
    static public double getAndVerifyNumeric(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path,
            double n) throws ConvException {
        if ((verifyEngine == null || verifyEngine.isEmpty()) && typeValidator == null) {
            return n;
        }

        DataVerifyResult verifyCache = new DataVerifyResult();
        if (verifyEngine != null && !verifyEngine.isEmpty()) {
            for (DataVerifyImpl vfy : verifyEngine) {
                try {
                    if (vfy.get(n, verifyCache)) {
                        if (verifyCache.value == null) {
                            return getAndVerifyTypeValidator(typeValidator, path, 0, verifyCache, false);
                        }
                        if (verifyCache.value instanceof Double) {
                            return getAndVerifyTypeValidator(typeValidator, path, (double) verifyCache.value,
                                    verifyCache, false);
                        }
                        if (verifyCache.value instanceof Long) {
                            return getAndVerifyTypeValidator(typeValidator, path,
                                    ((Long) verifyCache.value).doubleValue(), verifyCache, false);
                        }
                        return getAndVerifyTypeValidator(typeValidator, path,
                                doubleValueOf(verifyCache.value.toString()), verifyCache, false);
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
        } else {
            return getAndVerifyTypeValidator(typeValidator, path, n, verifyCache, false);
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    static public Number getAndVerifyToNumber(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path, String val,
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

        boolean hasValidator = verifyEngine != null && !verifyEngine.isEmpty();
        try {
            if (is_numeric) {
                if (is_double) {
                    return getAndVerifyNumeric(verifyEngine, typeValidator, path, doubleValueOf(val));
                } else {
                    return getAndVerifyNumeric(verifyEngine, typeValidator, path, longValueOf(val));
                }
            }

            DataVerifyResult verifyCache = new DataVerifyResult();
            if (hasValidator) {
                for (DataVerifyImpl vfy : verifyEngine) {
                    try {
                        if (vfy.get(val, verifyCache)) {
                            if (verifyCache.value == null) {
                                return getAndVerifyTypeValidator(typeValidator, path, 0, verifyCache, false);
                            }
                            if (verifyCache.value instanceof Double) {
                                return getAndVerifyTypeValidator(typeValidator, path, (Double) verifyCache.value,
                                        verifyCache, false);
                            }
                            if (verifyCache.value instanceof Long) {
                                return getAndVerifyTypeValidator(typeValidator, path, (Long) verifyCache.value,
                                        verifyCache, false);
                            }
                            if (is_double) {
                                return getAndVerifyTypeValidator(typeValidator, path,
                                        doubleValueOf(verifyCache.value.toString()), verifyCache, false);
                            } else {
                                return getAndVerifyTypeValidator(typeValidator, path,
                                        longValueOf(verifyCache.value.toString()), verifyCache, false);
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
            }

            // 如果有类型验证器，且自定义验证器无效的情况，可以尝试先执行类型验证器的数据转换
            if (typeValidator != null) {
                val = getAndVerifyTypeValidator(typeValidator, path, val, verifyCache, true);
                if (verifyCache.success) {
                    // 如果成功，下一步不需要再处理类型验证器了
                    return getAndVerifyToNumber(verifyEngine, null, path, val, is_double);
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
        if (!hasValidator) {
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

    static public double getAndVerifyToDouble(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path, String val)
            throws ConvException {
        Number value = getAndVerifyToNumber(verifyEngine, typeValidator, path, val, true);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else {
            return 0.0;
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    static public String getAndVerifyToString(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator,
            String path, String val)
            throws ConvException {
        if ((verifyEngine == null || verifyEngine.isEmpty()) && typeValidator == null) {
            return val;
        }

        DataVerifyResult verifyCache = new DataVerifyResult();
        if (verifyEngine != null && !verifyEngine.isEmpty()) {
            for (DataVerifyImpl vfy : verifyEngine) {
                try {
                    if (vfy.get(val, verifyCache)) {
                        if (verifyCache.value == null) {
                            return getAndVerifyTypeValidator(typeValidator, path, "", verifyCache, false);
                        }
                        if (verifyCache.value instanceof Double) {
                            String value;
                            if ((double) verifyCache.value == (long) ((double) verifyCache.value)) {
                                value = String.format("%d", (long) ((double) verifyCache.value));
                            } else {
                                value = String.format("%g", (double) verifyCache.value);
                            }
                            return getAndVerifyTypeValidator(typeValidator, path, value, verifyCache, false);
                        }
                        if (verifyCache.value instanceof Long) {
                            return getAndVerifyTypeValidator(typeValidator, path, verifyCache.value.toString(),
                                    verifyCache, false);
                        }
                        return getAndVerifyTypeValidator(typeValidator, path, verifyCache.value.toString(), verifyCache,
                                false);
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
        } else {
            return getAndVerifyTypeValidator(typeValidator, path, val, verifyCache, false);
        }
    }

    static public long getAndVerifyToLong(List<DataVerifyImpl> verifyEngine, DataVerifyImpl typeValidator, String path,
            String val)
            throws ConvException {
        Number value = getAndVerifyToNumber(verifyEngine, typeValidator, path, val, false);
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
