package com.owent.xresloader.data.src;

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

    public boolean next() {
        return false;
    }

    public <T> T getValue(String ident, T dv) {
        return dv;
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
