package com.owent.xresloader.data.dst;

import java.util.HashMap;

/**
 * Created by owentou on 2014/10/10.
 */
public abstract class DataDstImpl {
    /**
     * 初始化
     * @return
     */
    public boolean init() {
        return false;
    }

    /**
     * 编译并缓存协议描述文件
     * @return
     */
    public DataDstWriterNode compile() {
        return null;
    }

    /**
     * 生成数据
     * @param desc
     * @return
     */
    public byte[] build(DataDstWriterNode desc) {
        return new byte[0];
    }

    /**
     * 生成常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildConst() {
        return null;
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public byte[] dumpConst(HashMap<String, Object> data) { return null; };
}
