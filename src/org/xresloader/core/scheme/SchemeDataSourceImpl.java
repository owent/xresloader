package org.xresloader.core.scheme;

import org.xresloader.core.data.err.InitializeException;

/**
 * Created by owentou on 2014/9/30.
 */
public interface SchemeDataSourceImpl {

    int load() throws InitializeException;

    boolean load_scheme(String table) throws InitializeException;

    boolean isSupportMultipleScheme();
}
