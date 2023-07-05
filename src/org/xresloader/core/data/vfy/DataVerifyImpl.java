package org.xresloader.core.data.vfy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.xresloader.core.data.err.ConvException;

/**
 * Created by owent on 2016/12/7.
 */

public abstract class DataVerifyImpl {
    protected HashMap<String, Long> all_names = new HashMap<String, Long>();
    protected HashSet<Long> all_numbers = new HashSet<Long>();
    protected String name = "";

    protected DataVerifyImpl(String _name) {
        name = _name;
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
                return get(Double.valueOf(enum_name), res);
            } else {
                return get(Long.valueOf(enum_name), res);
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

    static public long getAndVerify(List<DataVerifyImpl> verifyEngine, String path, int n) throws ConvException {
        return getAndVerify(verifyEngine, path, (long) n);
    }

    static public long getAndVerify(List<DataVerifyImpl> verifyEngine, String path, long n) throws ConvException {
        return Math.round(getAndVerify(verifyEngine, path, (double) n));
    }

    static public double getAndVerify(List<DataVerifyImpl> verifyEngine, String path, double n) throws ConvException {
        if (verifyEngine == null || verifyEngine.isEmpty()) {
            return n;
        }

        try {
            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
                if (vfy.get(n, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("Check %g for %s failed, %s", n, path, e.getMessage()));
        }

        throw new ConvException(String.format("Check %g for %s failed, check data failed.", n, path));
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
                return getAndVerify(verifyEngine, path, Double.valueOf(val));
            } else {
                return getAndVerify(verifyEngine, path, Long.valueOf(val));
            }
        }

        try {
            if (verifyEngine == null || verifyEngine.isEmpty()) {
                if (is_double) {
                    return Double.valueOf(val);
                } else {
                    return Long.valueOf(val);
                }
            }

            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verifyEngine) {
                if (vfy.get(val, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("Convert %s for %s failed, %s", val, path, e.getMessage()));
        }

        throw new ConvException(String.format("Convert %s for %s failed, check data failed.", val, path));
    }

    static public long getAndVerifyToLong(List<DataVerifyImpl> verifyEngine, String path, String val)
            throws ConvException {
        return Math.round(getAndVerifyToDouble(verifyEngine, path, val));
    }

    static public class ValidatorTokens {
        public String normalizeName = null;
        public ArrayList<String> parameters = new ArrayList<String>();

        public ValidatorTokens() {
        }

        public boolean initialize() {
            // Special mode(>NUM,>=NUM,<NUM,<=NUM,LOW-HIGH)
            if (this.parameters.size() == 1) {
                char firstChar = this.parameters.get(0).charAt(0);
                if (firstChar == '>' || firstChar == '<' || firstChar == '-' || firstChar >= '0' || firstChar <= '9') {
                    this.normalizeName = this.parameters.get(0).replaceAll("\\s+", "");
                    return this.normalizeName.length() > 0;
                }
            }

            this.normalizeName = String.join(",", this.parameters);
            return this.normalizeName.length() > 0;
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
