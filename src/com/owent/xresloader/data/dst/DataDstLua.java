package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataContainer;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.commons.codec.binary.Hex;
import org.json.*;

import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstLua extends DataDstJava {
    private String endl = "\n";
    private String ident = "    ";

    @Override
    public boolean init() {
        if (ProgramOptions.getInstance().prettyIndent <= 0) {
            endl = " ";
            ident = "";
        } else {
            endl = System.getProperty("line.separator", "\n");

            ident = "";
            for (int i = 0; i < ProgramOptions.getInstance().prettyIndent; ++ i) {
                ident += " ";
            }
        }
        return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        DataDstJava.DataDstObject data_obj = build_data(desc);
        StringBuffer sb = new StringBuffer();

        sb.append("return {").append(endl);

        // header
        sb.append(ident).append("[1] = ");
        writeData(sb, data_obj.header, 1);
        sb.append(",").append(endl);

        // body
        for(Map.Entry<String, List<Object> > item: data_obj.body.entrySet()) {
            writeIdent(sb, 1);
            sb.append(item.getKey()).append(" = ");

            writeData(sb, item.getValue(), 1);
            sb.append(",").append(endl);
        }


        sb.append("}");

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }

    @Override
    public final DataDstWriterNode compile() {
        System.err.println("[ERROR] lua can not be protocol description.");
        return null;
    }

    private void writeIdent(StringBuffer sb, int ident_num) {
        for(; ident_num > 0; -- ident_num) {
            sb.append(ident);
        }
    }

    private void writeData(StringBuffer sb, Object data, int ident_num) {
        // null
        if (null == data) {
            sb.append("nil");
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Number) {
            sb.append(data.toString());
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            sb.append(((Boolean) data)? "true": "false");
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            // 利用json的字符串格式，和lua一样的没必要再引入一个库
            sb.append(JSONObject.quote((String)data));
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>)data;
            sb.append("{").append(endl);

            for(Object obj: ls) {
                writeIdent(sb, ident_num + 1);
                writeData(sb, obj, ident_num + 1);
                sb.append(",").append(endl);
            }

            writeIdent(sb, ident_num);
            sb.append("}");
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>)data;
            sb.append("{").append(endl);

            for(Map.Entry<String, Object> item: mp.entrySet()) {
                writeIdent(sb, ident_num + 1);
                sb.append(item.getKey()).append(" = ");

                writeData(sb, item.getValue(), ident_num + 1);
                sb.append(",").append(endl);
            }

            writeIdent(sb, ident_num);
            sb.append("}");
            return;
        }

        System.out.println(String.format("[ERROR] rewrite %s as nil, should not called here.", data.toString()));
        sb.append("nil");
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) {
        init();

        StringBuffer sb = new StringBuffer();
        sb.append("local const_res = ");
        writeData(sb, data, 0);
        sb.append(endl).append(endl);

        // 写入全局?
        if (ProgramOptions.getInstance().luaGlobal) {
            sb.append("for k, v in pairs(const_res) do").append(endl);
            sb.append(ident).append("_G[k] = v").append(endl);
            sb.append("end").append(endl).append(endl);
        }

        sb.append("return const_res").append(endl);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    };
}
