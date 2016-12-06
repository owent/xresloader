package com.owent.xresloader.data.dst;

import com.owent.xresloader.data.err.ConvException;

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
     * @return 协议处理器名字
     */
    public String name() {
        return this.getClass().getTypeName();
    }

    /**
     * 编译并返回协议映射关系
     * @return 协议映射关系
     */
    public DataDstWriterNode compile() throws ConvException {
        return null;
    }

    /**
     * 生成数据
     * @param compiler 生成输出结构的描述器
     * @return
     */
    public byte[] build(DataDstImpl compiler) throws ConvException {
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
