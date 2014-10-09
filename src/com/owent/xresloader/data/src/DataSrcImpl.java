package com.owent.xresloader.data.src;

/**
 * Created by owentou on 2014/10/9.
 */
public class DataSrcImpl {
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

    protected DataSrcImpl() {
    }

    public int init() {
        return -41;
    }

    public boolean next() {
        return false;
    }

    public String getString(int col) {
        return "";
    }

    public int getInt(int col) {
        return 0;
    }

    public double getDouble(int col) {
        return 0;
    }
}
