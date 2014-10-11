package com.owent.xresloader.data.dst;

/**
 * Created by owentou on 2014/10/10.
 */
public abstract class DataDstImpl {
    public boolean init() {
        return false;
    }

    public DataDstWriterNode compile() {
        return null;
    }

    public byte[] build(DataDstWriterNode desc) {
        return new byte[0];
    }
}
