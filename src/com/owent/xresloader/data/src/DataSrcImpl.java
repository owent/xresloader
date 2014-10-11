package com.owent.xresloader.data.src;

/**
 * Created by owentou on 2014/10/9.
 */
public abstract class DataSrcImpl {
    private static DataSrcImpl ourInstance = null;

    public static DataSrcImpl create(Class clazz) {
        try {
            return ourInstance = (DataSrcImpl)clazz.newInstance();
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

    protected DataSrcImpl() {
    }

    public int init() {
        return -41;
    }

    public boolean next() {
        return false;
    }

    public <T> T getValue(String ident, Class<T> clazz) {
        T ret = null;
        try {
            ret = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public int getRecordNumber() {
        return 0;
    }

    public boolean checkName(String _name) { return false; }
}
