package org.xresloader.core.data.dst;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstJson extends DataDstJava {

    @Override
    public boolean init() {
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "json";
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        JSONArray wrapper = new JSONArray();
        DataDstJava.DataDstObject data_obj = build_data(compiler);

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
        ProgramOptions.getLoger().error("json can not be protocol description.");
        return null;
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        JSONObject wrapper = null;

        if (null != data) {
            wrapper = new JSONObject(data);
        } else {
            wrapper = new JSONObject();
        }

        String encoded = wrapper.toString(ProgramOptions.getInstance().prettyIndent);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return encoded.getBytes();
        return encoded.getBytes(Charset.forName(encoding));
    }
}
