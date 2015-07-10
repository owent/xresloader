package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.scheme.SchemeConf;
import org.json.*;

import java.io.StringWriter;
import java.nio.charset.Charset;


/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstJson extends DataDstJava {


    @Override
    public boolean init() {
       return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        JSONArray wrapper = new JSONArray();
        DataDstJava.DataDstObject data_obj = build_data(desc);

        wrapper.put(data_obj.header);
        wrapper.put(data_obj.body);

        String encoded = wrapper.toString(ProgramOptions.getInstance().prettyIndent);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return encoded.getBytes();
        return encoded.getBytes(Charset.forName(encoding));
    }

    @Override
    public final DataDstWriterNode compile() {
        System.err.println("[ERROR] json can not be protocol description.");
        return null;
    }
}
