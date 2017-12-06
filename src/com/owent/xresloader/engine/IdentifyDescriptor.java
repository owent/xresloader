package com.owent.xresloader.engine;

import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.data.vfy.DataVerifyImpl;
import com.owent.xresloader.data.vfy.DataVerifyResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by owt50 on 2016/12/7.
 */
public class IdentifyDescriptor {
    public String name = "";
    public String verifier = null;
    public int index = 0;
    public List<DataVerifyImpl> verify_engine = null;

    public IdentifyDescriptor() {}

    public boolean hasVerifier() {
        return null != verify_engine && false == verify_engine.isEmpty();
    }

    public void resetVerifier() {
        verify_engine = null;
    }

    public void addVerifier(DataVerifyImpl ver) {
        if (null == ver) {
            return;
        }

        if (null == verify_engine) {
            verify_engine = new LinkedList<DataVerifyImpl>();
        }

        verify_engine.add(ver);
    }

    public long getAndVerify(int n) throws ConvException {
        return getAndVerify((long)n);
    }

    public long getAndVerify(long n) throws ConvException {
        if (!hasVerifier()) {
            return n;
        }

        try {
            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy: verify_engine) {
                if (vfy.get(n, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("check %d for %s at column %d failed, %s",
                    n, name, index + 1, e.getMessage()));
        }

        throw new ConvException(String.format("check %d for %s at column %d failed, check data failed.",
                n, name, index + 1));
    }

    public long getAndVerify(String val) throws ConvException {
        boolean is_int = true;
        for(int i = 0; is_int && i < val.length(); ++ i) {
            char c = val.charAt(i);
            if ((c < '0' || c > '9') &&
                    '.' != c &&
                    '-' != c) {
                is_int = false;
            }
        }

        if (is_int) {
            return getAndVerify(Math.round(Double.valueOf(val)));
        }

        try {
            if (!hasVerifier()) {
                return Math.round(Double.valueOf(val));
            }

            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy: verify_engine) {
                if (vfy.get(val, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("convert %s for %s at column %d failed, %s",
                    val, name, index + 1, e.getMessage()));
        }

        throw new ConvException(String.format("convert %s for %s at column %d failed, check data failed.",
                val, name, index + 1));
    }
}
