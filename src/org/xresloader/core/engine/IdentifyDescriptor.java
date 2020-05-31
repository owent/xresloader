package org.xresloader.core.engine;

import java.util.LinkedList;
import java.util.List;

import org.xresloader.core.data.dst.DataDstWriterNode;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyResult;

/**
 * Created by owt50 on 2016/12/7. 这个数据结构对应Excel里的一列
 */
public class IdentifyDescriptor {
    /**
     * 名字，对应标准化后的映射路径
     */
    public String name = "";

    /**
     * 索引，即Excel里的列号
     */
    public int index = 0;

    /**
     * 验证器文本
     */
    public String dataSourceFieldVerifier = null;
    /**
     * 验证器列表，包含配置在数据源里的验证器和协议里的验证器
     */
    public List<DataVerifyImpl> verifyEngine = null;

    /**
     * 指向写出节点的字段条目，如果是列表的话这个指向对应的列表中的一项
     */
    public DataDstWriterNode referToWriterNode;

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

    public int getRatio() {
        if (null == referToWriterNode || null == referToWriterNode.getReferBrothers()
                || null == referToWriterNode.getReferBrothers().innerFieldDesc) {
            return 1;
        }

        return referToWriterNode.getReferBrothers().innerFieldDesc.mutableExtension().ratio;
    }
}
