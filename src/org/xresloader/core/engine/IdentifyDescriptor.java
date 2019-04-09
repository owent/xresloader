package org.xresloader.core.engine;

import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyResult;

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

    public IdentifyDescriptor() {
    }

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
        return getAndVerify((long) n);
    }

    public long getAndVerify(long n) throws ConvException {
        if (!hasVerifier()) {
            return n;
        }

        try {
            DataVerifyResult verify_cache = new DataVerifyResult();

            for (DataVerifyImpl vfy : verify_engine) {
                if (vfy.get(n, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("check %d for %s at row %d, column %d in %s failed, %s", n, name,
                    DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1,
                    DataSrcImpl.getOurInstance().getCurrentTableName(), e.getMessage()));
        }

        throw new ConvException(String.format("check %d for %s at row %d, column %d in %s failed, check data failed.",
                n, name, DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1,
                DataSrcImpl.getOurInstance().getCurrentTableName()));
    }

    public long getAndVerify(String val) throws ConvException {
        boolean is_int = true;
        for (int i = 0; is_int && i < val.length(); ++i) {
            char c = val.charAt(i);
            if ((c < '0' || c > '9') && '.' != c && '-' != c) {
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

            for (DataVerifyImpl vfy : verify_engine) {
                if (vfy.get(val, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("convert %s for %s at row %d, column %d in %s failed, %s", val, name,
                    DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1,
                    DataSrcImpl.getOurInstance().getCurrentTableName(), e.getMessage()));
        }

        throw new ConvException(String.format("convert %s for %s at row %d, column %d in %s failed, check data failed.",
                val, name, DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1,
                DataSrcImpl.getOurInstance().getCurrentTableName()));
    }
}
