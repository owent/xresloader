package org.xresloader.core.data.dst;

import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.scheme.SchemeConf;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
            endl = getSystemEndl();

            ident = "";
            for (int i = 0; i < ProgramOptions.getInstance().prettyIndent; ++i) {
                ident += " ";
            }
        }
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "lua";
    }

    public boolean isUsingLuaModule() {
        return ProgramOptions.getInstance().luaModule != null && !ProgramOptions.getInstance().luaModule.isEmpty();
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        DataDstJava.DataDstObject data_obj = build_data(compiler);
        StringBuffer sb = new StringBuffer();
        int init_ident = 1;
        String split_body = ",";
        if (isUsingLuaModule()) {
            init_ident = 0;
            split_body = "";

            String mod_name = "UNKNOWN";
            // body
            for (Map.Entry<String, List<Object>> item : data_obj.body.entrySet()) {
                mod_name = item.getKey();
                break;
            }

            sb.append(String.format("module(\"%s.%s\", package.seeall)", ProgramOptions.getInstance().luaModule,
                    mod_name)).append(getSystemEndl());
            sb.append("-- this file is generated by xresloader, please don't edit it.").append(getSystemEndl())
                    .append(endl);

            // header
            sb.append(ident).append("header = ");
            writeData(sb, data_obj.header, init_ident);
            sb.append(getSystemEndl()).append(endl);
            sb.append(ident).append("data_message_type = ");
            writeData(sb, data_obj.data_message_type, init_ident);
            sb.append(getSystemEndl()).append(endl);

        } else {
            sb.append("-- this file is generated by xresloader, please don't edit it.").append(getSystemEndl())
                    .append(endl);

            sb.append("return {").append(endl);

            // header
            sb.append(ident).append("[1] = ");
            writeData(sb, data_obj.header, init_ident);
            sb.append(",").append(endl);

            sb.append(ident).append("[2] = ");
            writeData(sb, data_obj.data_message_type, init_ident);
            sb.append(",").append(endl);
        }

        // body
        for (Map.Entry<String, List<Object>> item : data_obj.body.entrySet()) {
            writeIdent(sb, init_ident);
            sb.append(item.getKey()).append(" = ");

            writeData(sb, item.getValue(), init_ident);
            sb.append(split_body).append(endl);
        }

        if (!isUsingLuaModule()) {
            sb.append("}");
        }

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }

    @Override
    public final DataDstWriterNode compile() {
        this.logErrorMessage("lua can not be protocol description.");
        return null;
    }

    private void writeIdent(StringBuffer sb, int ident_num) {
        for (; ident_num > 0; --ident_num) {
            sb.append(ident);
        }
    }

    private String quote(String input) {
        StringWriter w = new StringWriter();
        if (input == null || input.isEmpty()) {
            w.write("\"\"");
            return w.toString();
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = input.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = input.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        w.write("\\u{");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                        w.write("}");
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');

        return w.toString();
    }

    @SuppressWarnings("unchecked")
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
            sb.append(((Boolean) data) ? "true" : "false");
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            sb.append(quote((String) data));
            return;
        }

        // 列表
        if (data instanceof List) {
            List<Object> ls = (List<Object>) data;
            sb.append("{").append(endl);

            for (Object obj : ls) {
                writeIdent(sb, ident_num + 1);
                writeData(sb, obj, ident_num + 1);
                sb.append(",").append(endl);
            }

            writeIdent(sb, ident_num);
            sb.append("}");
            return;
        }

        // Hashmap
        if (data instanceof Map<?, ?>) {
            Map<?, ?> mp = (Map<?, ?>) data;
            sb.append("{").append(endl);

            ArrayList<Map.Entry<?, ?>> sorted_array = new ArrayList<Map.Entry<?, ?>>();
            sorted_array.ensureCapacity(mp.size());
            sorted_array.addAll(mp.entrySet());
            sorted_array.sort((l, r) -> {
                if (l.getKey() instanceof Integer && r.getKey() instanceof Integer) {
                    return ((Integer) l.getKey()).compareTo((Integer) r.getKey());
                } else if (l.getKey() instanceof Long && r.getKey() instanceof Long) {
                    return ((Long) l.getKey()).compareTo((Long) r.getKey());
                } else {
                    return l.getKey().toString().compareTo(r.getKey().toString());
                }
            });
            for (Map.Entry<?, ?> item : sorted_array) {
                writeIdent(sb, ident_num + 1);
                if (data instanceof SpecialInnerHashMap<?, ?>) {
                    if (item.getKey() instanceof String) {
                        sb.append("[");
                        sb.append(quote((String) item.getKey())).append("] = ");
                    } else {
                        sb.append("[");
                        sb.append(item.getKey()).append("] = ");
                    }
                } else {
                    if (item.getKey() instanceof String && !isStrictIdentify((String) item.getKey())) {
                        sb.append("[");
                        sb.append(quote((String) item.getKey())).append("] = ");
                    } else {
                        sb.append(item.getKey()).append(" = ");
                    }
                }

                writeData(sb, item.getValue(), ident_num + 1);
                sb.append(",").append(endl);
            }

            writeIdent(sb, ident_num);
            sb.append("}");
            return;
        }

        sb.append(quote(data.toString()));
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        init();

        StringBuffer sb = new StringBuffer();

        if (isUsingLuaModule()) {
            sb.append(String.format("module(\"%s\", package.seeall)", ProgramOptions.getInstance().luaModule))
                    .append(getSystemEndl());
            sb.append("-- this file is generated by xresloader, please don't edit it.").append(getSystemEndl())
                    .append(endl);
        } else {
            sb.append("-- this file is generated by xresloader, please don't edit it.").append(getSystemEndl())
                    .append(endl);
        }

        sb.append("local const_res = ");
        writeData(sb, data, 0);
        sb.append(endl).append(endl);

        // 写入全局,需要防止覆盖问题
        if (ProgramOptions.getInstance().luaGlobal) {
            sb.append(getSystemEndl());
            sb.append("local function extend(dst, src)").append(endl);
            sb.append(ident).append("for k, v in pairs(src) do").append(endl);
            sb.append(ident).append(ident).append("if not dst[k] or 'table' ~= type(v) then").append(endl);
            sb.append(ident).append(ident).append(ident).append("dst[k] = src[k]").append(endl);
            sb.append(ident).append(ident).append("else").append(endl);
            sb.append(ident).append(ident).append(ident).append("if 'table' ~= type(dst[k]) then").append(endl);
            sb.append(ident).append(ident).append(ident).append(ident).append("dst[k] = {}").append(endl);
            sb.append(ident).append(ident).append(ident).append("end").append(endl);
            sb.append(ident).append(ident).append(ident).append("extend(dst[k], src[k])").append(endl);
            sb.append(ident).append(ident).append("end").append(endl);
            sb.append(ident).append("end").append(endl);
            sb.append("end").append(endl);
            sb.append("extend(_G, const_res)").append(endl).append(endl);
        }

        sb.append(getSystemEndl());
        if (isUsingLuaModule()) {
            for (HashMap.Entry<String, Object> itemSet : data.entrySet()) {
                sb.append(String.format("%s = const_res.%s", itemSet.getKey(), itemSet.getKey())).append(endl);
            }
        } else {
            sb.append("return const_res").append(endl);
        }

        // 带编码的输出
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        if (null == encoding || encoding.isEmpty())
            return sb.toString().getBytes();
        return sb.toString().getBytes(Charset.forName(encoding));
    }
}
