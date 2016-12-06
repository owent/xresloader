package com.owent.xresloader.data.src;

import com.owent.xresloader.data.err.ConvException;

import java.util.HashMap;

/**
 * Created by owentou on 2014/10/9.
 */
public abstract class DataSrcImpl {

    private static DataSrcImpl ourInstance = null;

    protected DataSrcImpl() {
    }

    public static DataSrcImpl create(Class clazz) {
        try {
            return ourInstance = (DataSrcImpl) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return ourInstance = null;
    }

    public static DataSrcImpl getOurInstance() {
        return ourInstance;
    }

    public int init() {
        return -41;
    }

    /**
     * @brief 获取下一个数据源
     * @note 每次调用这个函数之后最好重做一次列名映射，否则可能数据不匹配
     * @return 有待处理的数据表返回true
     */
    public boolean next_table() {
        return false;
    }

    /**
     * @brief 获取当前数据源的下一个数据项
     * @return 有数据则返回true
     */
    public boolean next_row() {
        return false;
    }

    public <T> DataContainer<T> getValue(String ident, T dv) throws ConvException {
        DataContainer<T> ret = new DataContainer<T>();
        ret.value = dv;
        return ret;
    }

    public int getRecordNumber() {
        return 0;
    }

    public boolean checkName(String _name) {
        return false;
    }

    public HashMap<String, String> getMacros() {
        return null;
    }
}
