package org.xresloader.core.data.vfy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
            throw new ConvException(String.format("Check %d for %s failed, %s", n, path, e.getMessage()));
        }

        throw new ConvException(String.format("Check %d for %s failed, check data failed.", n, path));
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
}
