package org.xresloader.core.scheme;

import org.xresloader.core.data.err.InitializeException;

/**
 * Created by owentou on 2014/9/30.
 */
public interface SchemeDataSourceImpl {

    int load() throws InitializeException;

    default boolean loadScheme(String table) throws InitializeException {
        return load_scheme(table);
    }

    /** @deprecated Implemented by legacy scheme providers; callers should use {@link #loadScheme(String)}. */
    @Deprecated
    boolean load_scheme(String table) throws InitializeException;

    boolean isSupportMultipleScheme();
}
