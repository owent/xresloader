package com.owent.xresloader.engine;

import com.owent.xresloader.data.vfy.DataVerifyImpl;

/**
 * Created by owt50 on 2016/12/7.
 */
public class IdentifyDescriptor {
    public String name = "";
    public String verifier = null;
    public int index = 0;
    public DataVerifyImpl verify_engine = null;

    public IdentifyDescriptor(){}
}
