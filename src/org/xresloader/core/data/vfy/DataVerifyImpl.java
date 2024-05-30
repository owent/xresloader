package org.xresloader.core.data.vfy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xresloader.core.data.err.ConvException;

/**
 * Created by owent on 2016/12/7.
 */

public abstract class DataVerifyImpl {
    protected HashMap<String, Long> all_names = new HashMap<String, Long>();
    protected HashSet<Long> all_numbers = new HashSet<Long>();
    protected String name = "";

    private static ThreadLocal<Pattern> PERCENT_PATTERN = ThreadLocal
            .withInitial(() -> Pattern.compile("^\\s*((\\-\\s*)?[0-9]+(\\.[0-9]+)?)\\s*%\\s*$"));
    private static ThreadLocal<Pattern> INTEGER_WITH_DOT_PATTERN = ThreadLocal
            .withInitial(() -> Pattern.compile("^\\s*((\\-\\s*)?[0-9\\,]+)\\s*$"));

    protected DataVerifyImpl(String _name) {
        name = _name;
    }

    protected DataVerifyImpl(ValidatorTokens tokens) {
        name = tokens.name;
    }

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

    private static double doubleValueOf(String input) {
        Matcher matcher = PERCENT_PATTERN.get().matcher(input);
        if (matcher.matches()) {
            return Double.valueOf(matcher.group(1).trim()) / 100.0;
        }
        return Double.valueOf(input);
    }

    private static long longValueOf(String input) {
        Matcher matcher = INTEGER_WITH_DOT_PATTERN.get().matcher(input);
        if (matcher.matches()) {
            return Long.valueOf(matcher.group(1).trim());
        }
        return Long.valueOf(input);
    }

    public boolean get(String enum_name, DataVerifyResult res) {
        if (null == enum_name || enum_name.isEmpty()) {
            res.success = true;
            res.value = 0;
            return res.success;
        }

        boolean is_numeric = true;
        boolean is_double = false;
        for (int i = 0; is_numeric && i < enum_name.length(); ++i) {
            char c = enum_name.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
                is_numeric = false;
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
        StringBuffer sb = new StringBuffer();
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
        return Math.round(getAndVerify(verifyEngine, path, (double) n));
    }

    static private String getValidatorWord(List<DataVerifyImpl> verifyEngine) {
        if (verifyEngine == null || verifyEngine.isEmpty() || verifyEngine.size() == 1) {
            return "validator";
        }

        return "validator(s)";
    }

    static public double getAndVerify(List<DataVerifyImpl> verifyEngine, String path, double n) throws ConvException {
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            return n;
        }

        try {
            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
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
            }
        } catch (Exception e) {
            String value;
            if (n == (long) n) {
                value = String.format("%d", (long) n);
            } else {
                value = String.format("%g", n);
            }
            throw new ConvException(String.format("Check %s for %s with %s %s failed, %s", value, path,
                    getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine), e.getMessage()));
        }

        String value;
        if (n == (long) n) {
            value = String.format("%d", (long) n);
        } else {
            value = String.format("%g", n);
        }
        throw new ConvException(
                String.format("Check %s for %s with %s %s failed, check data failed.", value, path,
                        getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine)));
    }

    static public double getAndVerifyToDouble(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        boolean is_numeric = true;
        boolean is_double = false;
        for (int i = 0; is_numeric && i < val.length(); ++i) {
            char c = val.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
                is_numeric = false;
            }
            if ('.' == c) {
                is_double = true;
            }
        }

        if (is_numeric) {
            if (is_double) {
                return getAndVerify(verifyEngine, path, doubleValueOf(val));
            } else {
                return getAndVerify(verifyEngine, path, longValueOf(val));
            }
        }

        try {
            if (verifyEngine == null || verifyEngine.isEmpty()) {
                if (is_double) {
                    return doubleValueOf(val);
                } else {
                    return longValueOf(val);
                }
            }

            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
                if (vfy.get(val, verify_cache)) {
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
            }
        } catch (Exception e) {
            if (verifyEngine == null || verifyEngine.isEmpty()) {
                throw new ConvException(String.format("Convert %s for %s failed, %s", val, path, e.getMessage()));
            } else {
                throw new ConvException(String.format("Convert %s for %s with %s %s failed, %s", val, path,
                        getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine), e.getMessage()));
            }
        }

        throw new ConvException(String.format("Convert %s for %s with %s %s failed, check data failed.", val,
                path, getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine)));
    }

    static public String getAndVerifyToString(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        try {
            if (verifyEngine == null || verifyEngine.isEmpty()) {
                return val;
            }

            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
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
                        return ((Long) verify_cache.value).toString();
                    }
                    return verify_cache.value.toString();
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("Convert %s for %s with %s %s failed, %s", val, path,
                    getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine), e.getMessage()));
        }

        throw new ConvException(String.format("Convert %s for %s with %s %s failed, check data failed.", val,
                path, getValidatorWord(verifyEngine), collectValidatorNames(verifyEngine)));
    }

    static public long getAndVerifyToLong(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        return Math.round(getAndVerifyToDouble(verifyEngine, path, val));
    }

    static public class ValidatorTokens {
        public String name = "";
        public ArrayList<String> parameters = new ArrayList<String>();

        public ValidatorTokens() {
        }

        public boolean initialize() {
            if (this.parameters.size() == 0) {
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

        LinkedList<ValidatorTokens> ret = new LinkedList<ValidatorTokens>();
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
