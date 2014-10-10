package com.owent.xresloader.data.dst;

/**
 * Created by owentou on 2014/10/10.
 */
public abstract class DataDstImpl {
    public boolean init() {
        return false;
    }

    public byte[] compile(DataDstImpl desc) {
        return new byte[0];
    }
}
