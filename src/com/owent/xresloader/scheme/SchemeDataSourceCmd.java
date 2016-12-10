package com.owent.xresloader.scheme;

import com.owent.xresloader.ProgramOptions;

import java.util.ArrayList;

/**
 * Created by owt50 on 2016/12/10.
 */
public class SchemeDataSourceCmd extends SchemeDataSourceBase {
    @Override
    public int load() {
        // 至少有四个必须字段，所以少于四项的肯定错
        if(ProgramOptions.getInstance().dataSourceMetas.length < 4) {
            ProgramOptions.getLoger().error("Data source error. DataSource,ProtoName,OutputFile and KeyRow is required.");
            return -21;
        }

        return 0;
    }

    @Override
    public boolean load_scheme(String table) {
        for(String cfg_val : ProgramOptions.getInstance().dataSourceMetas) {
            int eq_index = cfg_val.indexOf('=');
            if (eq_index < 0) {
                ProgramOptions.getLoger().error("Data meta %s error.Must be KEY=%s",
                        cfg_val,
                        String.join(
                            ProgramOptions.getInstance().dataSourceMetaDelimiter.replaceAll("\\(.)", "$1"),
                            "VALUE1", "VALUE2", "VALUE3"
                        )
                );
                continue;
            }

            ArrayList<String> vals = new ArrayList<String>();
            vals.ensureCapacity(3);
            if (cfg_val.length() > eq_index + 1) {
                for(String vn: cfg_val.substring(eq_index + 1).split(ProgramOptions.getInstance().dataSourceMetaDelimiter)) {
                    vals.add(vn.trim());
                }
            }

            while(vals.size() < 3) {
                vals.add("");
            }

            set_scheme(cfg_val.substring(0, eq_index).trim(), vals);
        }

        boolean ret = true;
        // 检查数据
        if (SchemeConf.getInstance().getDataSource().isEmpty()) {
            ProgramOptions.getLoger().error("DataSource is required");
            ret = false;
        }

        if (0 == SchemeConf.getInstance().getKey().getRow()) {
            ProgramOptions.getLoger().error("KeyRow is required");
            ret = false;
        }

        if (SchemeConf.getInstance().getProtoName().isEmpty()) {
            ProgramOptions.getLoger().error("ProtoName is required");
            ret = false;
        }

        if (SchemeConf.getInstance().getOutputFile().isEmpty()) {
            ProgramOptions.getLoger().error("ProtoName is required");
            ret = false;
        }

        return ret;
    }
}
