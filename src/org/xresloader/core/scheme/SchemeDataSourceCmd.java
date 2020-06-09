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
        if(null == ProgramOptions.getInstance().dataSourceMetas || ProgramOptions.getInstance().dataSourceMetas.length < 4) {
            throw new InitializeException("Data source error. DataSource,ProtoName,OutputFile and KeyRow is required.");
        }

        return 0;
    }

    @Override
    public boolean load_scheme(String table) throws InitializeException {
        for(String cfg_val : ProgramOptions.getInstance().dataSourceMetas) {
            String unwrap_val = cfg_val;
            if (unwrap_val.length() > 2 && ('"' == unwrap_val.charAt(0) || '\'' == unwrap_val.charAt(0)) && unwrap_val.charAt(0) == unwrap_val.charAt(unwrap_val.length() - 1)) {
                unwrap_val = unwrap_val.substring(1, unwrap_val.length() - 1);
            }

            int eq_index = unwrap_val.indexOf('=');
            if (eq_index < 0) {
                ProgramOptions.getLoger().error("Data meta %s error.Must be KEY=%s",
                        unwrap_val,
                        String.join(
                            ProgramOptions.getInstance().dataSourceMetaDelimiter,
                            "VALUE1", "VALUE2", "VALUE3"
                        )
                );
                continue;
            }

            ArrayList<String> vals = new ArrayList<String>();
            vals.ensureCapacity(3);
            if (unwrap_val.length() > eq_index + 1) {
                for(String vn: unwrap_val.substring(eq_index + 1).split(ProgramOptions.getInstance().dataSourceMetaDelimiter)) {
                    vals.add(vn.trim());
                }
            }

            while(vals.size() < 3) {
                vals.add("");
            }

            set_scheme(unwrap_val.substring(0, eq_index).trim(), vals);
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
