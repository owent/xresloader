package com.owent.xresloader.data.src;

/**
 * Created by 欧文韬 on 2015/5/11.
 */
public class DataContainer<T> {
    public boolean valid = false;
    public T value;

    public DataContainer() {}

    public DataContainer<T> setDefault(T v) {
        value = v;

        return this;
    }

    public DataContainer<T> set(T v) {
        value = v;
        valid = true;

        return this;
    }

    public T get() {
        return value;
    }
}
