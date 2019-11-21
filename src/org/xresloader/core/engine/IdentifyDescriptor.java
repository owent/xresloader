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

    public List<DataVerifyImpl> getVerifier() {
        return verifyEngine;
    }
}
