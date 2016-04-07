package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.scheme.SchemeConf;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2016/04/07.
 */
public class DataDstJavascript extends DataDstJava {
    private String endl = "\n";
    private String ident = "    ";

    private enum EXPORT_MODE { GLOBAL, NODEJS, AMD };

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
    public final byte[] build(DataDstWriterNode desc) throws ConvException {
        DataDstObject data_obj = build_data(desc);
        StringBuffer sb = new StringBuffer();

        HashMap<String, Object> conv_type = new HashMap<String, Object>();
        String header_name = "";
        for(Map.Entry<String, List<Object> > data_item: data_obj.body.entrySet()) {
            conv_type.put(data_item.getKey(), data_item.getValue());
        }

        for(Map.Entry<String, List<Object> > data_item: data_obj.body.entrySet()) {
            header_name = String.format("%s_header", data_item.getKey());
            if (!conv_type.containsKey(header_name)) {
                conv_type.put(header_name, data_obj.header);
                break;
            }
        }

        writeExport(sb, conv_type, 0);

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

    private void writeExport(StringBuffer sb, HashMap<String, Object> export_items, int ident_num) {
        String export_mode_name = ProgramOptions.getInstance().javascriptExport;
        EXPORT_MODE export_mode = EXPORT_MODE.GLOBAL;
        if (null != export_mode_name) {
            // nodejs mode
            if (export_mode_name.equalsIgnoreCase("nodejs")) {
                export_mode = EXPORT_MODE.NODEJS;
            } else if (export_mode_name.equalsIgnoreCase("amd")) {
                export_mode = EXPORT_MODE.AMD;
            }
        }

        // export mode header
        switch(export_mode) {
            case AMD: {
                sb.append("define({");
                break;
            }
            default: {
                break;
            }
        }

        // export mode content
        boolean is_first = true;
        for(Map.Entry<String, Object> item: export_items.entrySet()) {
            // export mode header
            switch(export_mode) {
                case AMD: {
                    if (is_first) {
                        sb.append(endl);
                    } else {
                        sb.append(",").append(endl);
                    }

                    writeIdent(sb, ident_num + 1);
                    sb.append(item.getKey()).append(": ");
                    writeData(sb, item.getValue(), 1);
                    break;
                }
                case NODEJS: {
                    sb.append(String.format("exports.%s = ", item.getKey()));
                    writeData(sb, item.getValue(), 0);
                    sb.append(";").append(endl);
                    break;
                }
                default: {
                    sb.append(String.format("(window || global).%s = ", item.getKey()));
                    writeData(sb, item.getValue(), 0);
                    sb.append(";").append(endl);
                    break;
                }
            }

            is_first = false;
        }

        // export mode footer
        switch(export_mode) {
            case AMD: {
                writeIdent(sb, ident_num);
                sb.append(endl).append("});").append(endl);
                break;
            }
            default: {
                break;
            }
        }
    }

    private void writeData(StringBuffer sb, Object data, int ident_num) {
        // null
        if (null == data) {
            sb.append("undefined");
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
            // 利用json的字符串格式，和javascript一样的没必要再引入一个库
            sb.append(JSONObject.quote((String)data));
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>)data;
            sb.append("[");

            boolean is_first = true;
            for(Object obj: ls) {
                if (is_first) {
                    sb.append(endl);
                } else {
                    sb.append(",").append(endl);
                }

                writeIdent(sb, ident_num + 1);
                writeData(sb, obj, ident_num + 1);
                is_first = false;
            }

            if (!is_first) {
                sb.append(endl);
                writeIdent(sb, ident_num);
            }
            sb.append("]");
            return;
        }

        // Hashmap
        if (data instanceof Map) {
            Map<String, Object> mp = (Map<String, Object>)data;
            sb.append("{");

            boolean is_first = true;
            for(Map.Entry<String, Object> item: mp.entrySet()) {
                if (is_first) {
                    sb.append(endl);
                } else {
                    sb.append(",").append(endl);
                }

                writeIdent(sb, ident_num + 1);
                sb.append(item.getKey()).append(" : ");

                writeData(sb, item.getValue(), ident_num + 1);
                is_first = false;
            }

            if (!is_first) {
                sb.append(endl);
                writeIdent(sb, ident_num);
            }
            sb.append("}");
            return;
        }

        System.out.println(String.format("[ERROR] rewrite %s as null, should not called here.", data.toString()));
        sb.append("nil");
    }

    /**
     * 转储常量数据
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) {
        init();

        StringBuffer sb = new StringBuffer();
        writeExport(sb, data, 0);
        sb.append(endl);

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    };
}
