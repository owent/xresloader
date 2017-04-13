package com.owent.xresloader.scheme;

/**
 * Created by owentou on 2014/9/30.
 */
public interface SchemeDataSourceImpl {

    int load();

    boolean load_scheme(String table);

    boolean isSupportMultipleScheme();
}
