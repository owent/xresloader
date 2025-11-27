package org.xresloader.core.engine;

import java.util.LinkedList;
import java.util.List;

import org.xresloader.core.data.dst.DataDstWriterNode;
import org.xresloader.core.data.vfy.DataVerifyImpl;

/**
 * Created by owent on 2016/12/7. 这个数据结构对应Excel里的一列
 */
public class IdentifyDescriptor implements Cloneable {
    /**
     * 名字，对应标准化后的映射路径
     */
    public String name = "";

    /**
     * 数据Field所在数据源的位置，从0开始（transpose模式为行号，非transpose模式为列号）
     */
    private int dataFieldIndex = 0;

    /**
     * 验证器文本
     */
    public String dataSourceFieldValidator = null;
    /**
     * 验证器列表，包含配置在数据源里的验证器和协议里的验证器
     */
    public List<DataVerifyImpl> verifyEngine = null;

    /**
     * 指向写出节点的字段条目，如果是列表的话这个指向对应的列表中的一项
     */
    public DataDstWriterNode referToWriterNode;

    public IdentifyDescriptor(int dataFieldIndex) {
        this.dataFieldIndex = dataFieldIndex;
    }

    public void resetDataSourcePosition(int dataFieldIndex) {
        this.dataFieldIndex = dataFieldIndex;
    }

    public int getDataFieldIndex() {
        return dataFieldIndex;
    }

    public boolean hasValidator() {
        return null != verifyEngine && false == verifyEngine.isEmpty();
    }

    public void resetValidator() {
        verifyEngine = null;
    }

    public void addValidator(DataVerifyImpl ver) {
        if (null == ver) {
            return;
        }

        if (null == verifyEngine) {
            verifyEngine = new LinkedList<>();
        }

        verifyEngine.add(ver);
    }

    public List<DataVerifyImpl> getValidator() {
        return verifyEngine;
    }

    public DataVerifyImpl getTypeValidator() {
        if (this.referToWriterNode != null) {
            if (this.referToWriterNode.getFieldDescriptor() != null) {
                return this.referToWriterNode.getFieldDescriptor().getTypeValidator();
            }
            if (this.referToWriterNode.getOneofDescriptor() != null) {
                return this.referToWriterNode.getOneofDescriptor().getTypeValidator();
            }
            if (this.referToWriterNode.getTypeDescriptor() != null) {
                return this.referToWriterNode.getTypeDescriptor().getTypeValidator();
            }
        }

        return null;
    }

    public int getRatio() {
        if (null == referToWriterNode || null == referToWriterNode.getReferBrothers()
                || null == referToWriterNode.getReferBrothers().innerFieldDesc) {
            return 1;
        }

        return referToWriterNode.getReferBrothers().innerFieldDesc.mutableExtension().ratio;
    }

    @Override
    public IdentifyDescriptor clone() throws CloneNotSupportedException {
        return (IdentifyDescriptor) super.clone();
    }
}
