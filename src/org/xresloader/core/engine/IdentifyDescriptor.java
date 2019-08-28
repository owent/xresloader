package org.xresloader.core.engine;

import java.util.LinkedList;
import java.util.List;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyResult;

/**
 * Created by owt50 on 2016/12/7.
 */
public class IdentifyDescriptor {
    public String name = "";
    public String dataSourceFieldVerifier = null;
    public int index = 0;
    public List<DataVerifyImpl> verifyEngine = null;
    public int ratio = 1;

    public IdentifyDescriptor() {
    }

    public boolean hasVerifier() {
        return null != verifyEngine && false == verifyEngine.isEmpty();
    }

    public void resetVerifier() {
        verifyEngine = null;
    }

    public void addVerifier(DataVerifyImpl ver) {
        if (null == ver) {
            return;
        }

        if (null == verifyEngine) {
            verifyEngine = new LinkedList<DataVerifyImpl>();
        }

        verifyEngine.add(ver);
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

            for (DataVerifyImpl vfy : verifyEngine) {
                if (vfy.get(n, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("check %d for %s at row %d, column %d in %s failed, %s", n, name,
                    DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1, DataSrcImpl.getOurInstance().getCurrentTableName(), e.getMessage()));
        }

        throw new ConvException(String.format("check %d for %s at row %d, column %d in %s failed, check data failed.", n, name,
                DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1, DataSrcImpl.getOurInstance().getCurrentTableName()));
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

            for (DataVerifyImpl vfy : verifyEngine) {
                if (vfy.get(val, verify_cache)) {
                    return verify_cache.value;
                }
            }
        } catch (Exception e) {
            throw new ConvException(String.format("convert %s for %s at row %d, column %d in %s failed, %s", val, name,
                    DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1, DataSrcImpl.getOurInstance().getCurrentTableName(), e.getMessage()));
        }

        throw new ConvException(String.format("convert %s for %s at row %d, column %d in %s failed, check data failed.", val, name,
                DataSrcImpl.getOurInstance().getCurrentRowNum() + 1, index + 1, DataSrcImpl.getOurInstance().getCurrentTableName()));
    }
}
