package org.xresloader.core.data.dst;

import java.io.IOException;
import java.util.HashMap;

import org.xresloader.core.data.dst.DataDstWriterNode.JAVA_TYPE;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.ProgramOptions;

/**
 * Created by owentou on 2014/10/10.
 */
public abstract class DataDstImpl {
    private String systemEndl = null;
    private String lastErrorMessage = "";

    public String getSystemEndl() {
        if (null != systemEndl) {
            return systemEndl;
        }

        systemEndl = System.getProperty("line.separator", "\n");
        if (null == systemEndl || systemEndl.isEmpty()) {
            systemEndl = "\r\n";
        }
        return systemEndl;
    }

    /**
     * 初始化
     * 
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
     * 
     * @return 协议映射关系
     */
    public DataDstWriterNode compile() throws ConvException {
        return null;
    }

    /**
     * 生成数据
     * 
     * @param src 生成输出结构的描述器
     * @return
     */
    public byte[] build(DataDstImpl src) throws ConvException {
        return new byte[0];
    }

    /**
     * 生成常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildConst() {
        return null;
    }

    /**
     * 生成选项数据
     * 
     * @return 选项数据,不支持的时候返回空
     */
    public HashMap<String, Object> buildOptions(ProgramOptions.ProtoDumpType dumpType) {
        return null;
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        return null;
    }

    public String getLastErrorMessage() {
        return this.lastErrorMessage;
    }

    public void setLastErrorMessage(String format, Object... args) {
        this.lastErrorMessage = String.format(format, args);
    }

    public void logErrorMessage(String format, Object... args) {
        this.setLastErrorMessage(format, args);
        ProgramOptions.getLoger().error("%s", this.lastErrorMessage);
    }

    static public String[] splitPlainGroups(String input, String sep) {
        if (sep == null || sep.isEmpty()) {
            sep = ",;|";
        }

        if (input == null || input.isEmpty()) {
            return null;
        }

        char sepC = 0;
        for (int i = 0; sepC == 0 && i < input.length(); ++i) {
            if (sep.indexOf(input.charAt(i)) < 0) {
                continue;
            }

            sepC = input.charAt(i);
        }

        return input.split("\\" + sepC);
    }

    static public Boolean parsePlainDataBoolean(String input, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (input == null) {
            return false;
        }

        String item = ExcelEngine.tryMacro(input.trim());
        return DataSrcImpl.getBooleanFromString(item);
    }

    static public Boolean[] parsePlainDataBoolean(String[] groups, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (groups == null) {
            return null;
        }

        Boolean[] ret = new Boolean[groups.length];
        for (int i = 0; i < groups.length; ++i) {
            ret[i] = parsePlainDataBoolean(groups[i], ident, field);
        }

        return ret;
    }

    static public String parsePlainDataString(String input, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (input == null) {
            return null;
        }

        if(DataSrcImpl.getOurInstance().isInitialized() && ProgramOptions.getInstance().enableStringMacro) {
            return ExcelEngine.tryMacro(input.trim());
        } else {
            return input.trim();
        }
    }

    static public String[] parsePlainDataString(String[] groups, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (groups == null) {
            return null;
        }

        String[] ret = new String[groups.length];
        for (int i = 0; i < groups.length; ++i) {
            String item = groups[i];
            ret[i] = parsePlainDataString(item, ident, field);
        }

        return ret;
    }

    static public Long parsePlainDataLong(String input, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (input == null) {
            return null;
        }

        String item = ExcelEngine.tryMacro(input.trim());
        Long ret;
        if (ident != null) {
            ret = DataVerifyImpl.getAndVerify(ident.getVerifier(), ident.name, item);
            if (ident.getRatio() != 1) {
                ret *= ident.getRatio();
            }
        } else if (field != null) {
            ret = DataVerifyImpl.getAndVerify(field.getVerifier(), field.getName(), item);
            if (field.mutableExtension().ratio != 1) {
                ret *= field.mutableExtension().ratio;
            }
        } else {
            ret = Long.valueOf(0);
        }

        return ret;
    }

    static public Long[] parsePlainDataLong(String[] groups, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (groups == null) {
            return null;
        }

        Long[] ret = new Long[groups.length];
        for (int i = 0; i < groups.length; ++i) {
            ret[i] = parsePlainDataLong(groups[i], ident, field);
        }

        return ret;
    }

    static public Double parsePlainDataDouble(String input, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (input == null) {
            return 0.0;
        }

        try {
            String item = ExcelEngine.tryMacro(input.trim());
            Double ret = Double.valueOf(item);
            if (ident != null) {
                if (ident.getRatio() != 1) {
                    ret *= ident.getRatio();
                }
            } else if (field != null) {
                if (field.mutableExtension().ratio != 1) {
                    ret *= field.mutableExtension().ratio;
                }
            }

            return ret;
        } catch (java.lang.NumberFormatException e) {
            throw new ConvException(String.format("Try to convert %s to double failed.", input));
        }
    }

    static public Double[] parsePlainDataDouble(String[] groups, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field) throws ConvException {
        if (groups == null) {
            return null;
        }

        Double[] ret = new Double[groups.length];
        for (int i = 0; i < groups.length; ++i) {
            String item = ExcelEngine.tryMacro(groups[i].trim());
            ret[i] = parsePlainDataDouble(item, ident, field);
        }

        return ret;
    }

    static public Object[] parsePlainDataOneof(String input, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor oneof) throws ConvException {
        if (input == null) {
            return null;
        }

        String[] groups = splitPlainGroups(input, getPlainOneofSeparator(oneof));
        if (groups == null || groups.length < 1) {
            return null;
        }

        String item = ExcelEngine.tryMacro(groups[0].trim());
        Long select;
        if (ident != null) {
            select = DataVerifyImpl.getAndVerify(ident.getVerifier(), ident.name, item);
        } else {
            try {
                select = Long.valueOf(DataVerifyImpl.getAndVerify(oneof.getVerifier(), "[PLAIN TEXT]", item.trim()));
            } catch (java.lang.NumberFormatException e) {
                throw new ConvException(String.format("Try to convert %s to oneof case failed.", input));
            }
        }

        DataDstWriterNode.DataDstFieldDescriptor field = oneof.getFieldById(select.intValue());
        if (field == null) {
            return null;
        }

        Object[] ret = new Object[groups.length];
        ret[0] = field;
        for (int i = 1; i < groups.length; ++i) {
            ret[i] = groups[i];
        }

        if (ret.length > 2) {
            DataSrcImpl current_source = DataSrcImpl.getOurInstance();
            String[] ignored = new String[ret.length - 2];
            for (int i = 2; i < ret.length; ++i) {
                ignored[i - 2] = groups[i];
            }
            if (current_source != null) {
                ProgramOptions.getLoger().warn(
                        "Convert %s from \"%s\", we need only %d fields but provided %d, \"%s\" may be ignored.%s  > File: %s, Table: %s, Row: %d, Column: %d",
                        oneof.getFullName(), input, 2, ret.length, String.join("\",\"", ignored),
                        ProgramOptions.getEndl(), current_source.getCurrentFileName(),
                        current_source.getCurrentTableName(), current_source.getCurrentRowNum() + 1,
                        current_source.getLastColomnNum() + 1);
            } else {
                ProgramOptions.getLoger().warn(
                        "Convert %s from \"%s\", we need only %d fields but provided %d, \"%s\" may be ignored.",
                        oneof.getFullName(), input, 2, ret.length, String.join("\",\"", ignored));
            }
        }

        return ret;
    }

    static public String getPlainFieldSeparator(DataDstWriterNode.DataDstFieldDescriptor field) {
        if (field == null) {
            return null;
        }

        if (field.isList()) {
            return field.mutableExtension().plainSeparator;
        }

        String ret = field.mutableExtension().plainSeparator;
        if (field.getType() != JAVA_TYPE.MESSAGE) {
            return ret;
        }

        if (ret == null) {
            ret = field.getTypeDescriptor().mutableExtension().plainSeparator;
        } else if (field.getTypeDescriptor().mutableExtension().plainSeparator != null) {
            ret = ret + field.getTypeDescriptor().mutableExtension().plainSeparator;
        }

        return ret;
    }

    static public String getPlainMessageSeparator(DataDstWriterNode.DataDstFieldDescriptor field) {
        if (field == null) {
            return null;
        }

        if (field.isList()) {
            if (field.getType() == JAVA_TYPE.MESSAGE) {
                return field.getTypeDescriptor().mutableExtension().plainSeparator;
            }
        }

        String ret = field.mutableExtension().plainSeparator;
        if (field.getType() != JAVA_TYPE.MESSAGE) {
            return ret;
        }

        if (ret == null) {
            ret = field.getTypeDescriptor().mutableExtension().plainSeparator;
        } else if (field.getTypeDescriptor().mutableExtension().plainSeparator != null) {
            ret = ret + field.getTypeDescriptor().mutableExtension().plainSeparator;
        }

        return ret;
    }

    static public String getPlainOneofSeparator(DataDstWriterNode.DataDstOneofDescriptor oneof) {
        if (oneof == null) {
            return null;
        }

        return oneof.mutableExtension().plainSeparator;
    }
}
