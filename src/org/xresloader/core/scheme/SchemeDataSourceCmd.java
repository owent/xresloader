package org.xresloader.core.scheme;

import org.xresloader.core.ProgramOptions;

import java.util.ArrayList;

import org.xresloader.core.data.err.InitializeException;

/**
 * Created by owt50 on 2016/12/10.
 */
public class SchemeDataSourceCmd extends SchemeDataSourceBase {
    @Override
    public int load() throws InitializeException {
        // 至少有四个必须字段，所以少于四项的肯定错
        if (null == ProgramOptions.getInstance().dataSourceMetas
                || ProgramOptions.getInstance().dataSourceMetas.length < 4) {
            throw new InitializeException("Data source error. DataSource,ProtoName,OutputFile and KeyRow is required.");
        }

        return 0;
    }

    @Override
    @Deprecated
    public boolean load_scheme(String table) throws InitializeException {
        for (String cfgVal : ProgramOptions.getInstance().dataSourceMetas) {
            String unwrapVal = cfgVal;
            if (unwrapVal.length() > 2 && ('"' == unwrapVal.charAt(0) || '\'' == unwrapVal.charAt(0))
                    && unwrapVal.charAt(0) == unwrapVal.charAt(unwrapVal.length() - 1)) {
                unwrapVal = unwrapVal.substring(1, unwrapVal.length() - 1);
            }

            int eqIndex = unwrapVal.indexOf('=');
            if (eqIndex < 0) {
                ProgramOptions.getLogger().error("Data meta %s error.Must be KEY=%s", unwrapVal, String
                        .join(ProgramOptions.getInstance().dataSourceMetaDelimiter, "VALUE1", "VALUE2", "VALUE3"));
                continue;
            }

            ArrayList<String> vals = new ArrayList<String>();
            vals.ensureCapacity(3);
            if (unwrapVal.length() > eqIndex + 1) {
                for (String vn : unwrapVal.substring(eqIndex + 1)
                        .split(ProgramOptions.getInstance().dataSourceMetaDelimiter)) {
                    vals.add(vn.trim());
                }
            }

            while (vals.size() < 3) {
                vals.add("");
            }

            setScheme(unwrapVal.substring(0, eqIndex).trim(), vals);
        }

        boolean ret = true;
        // 检查数据
        if (SchemeConf.getInstance().getDataSource().isEmpty()) {
            throw new InitializeException("DataSource is required");
        }

        if (0 == SchemeConf.getInstance().getKey().getRow()) {
            throw new InitializeException("KeyRow is required");
        }

        if (SchemeConf.getInstance().getProtoName().isEmpty()) {
            throw new InitializeException("ProtoName is required");
        }

        if (SchemeConf.getInstance().getOutputFile().isEmpty()) {
            throw new InitializeException("ProtoName is required");
        }

        return ret;
    }

    public boolean isSupportMultipleScheme() {
        return false;
    }
}
