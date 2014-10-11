package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstLua extends DataDstImpl {

    public boolean init() {
        return true;
    }

    public final byte[] build(DataDstWriterNode desc) {
        StringBuffer sb = new StringBuffer();

        sb.append("return {\n");
        sb.append("    header = {\n");
        sb.append("        xrex_ver = \"" + ProgramOptions.getInstance().getVersion() + "\",\n");
        sb.append("        data_ver = \"" + ProgramOptions.getInstance().getVersion() + "\",\n");
        sb.append("        count = " + DataSrcImpl.getOurInstance().getRecordNumber() + ",\n");
        sb.append("        hash_code = \"lua data has no hash code\"\n");
        sb.append("    },\n");
        sb.append("    " + SchemeConf.getInstance().getProtoName() + "= {\n");


        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString().getBytes();
    }


    public final DataDstWriterNode compile() {
        return null;
    }
}
