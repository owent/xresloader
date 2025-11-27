package org.xresloader.core.data.dst;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.scheme.SchemeConf;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstJson extends DataDstJava {

    /**
     * Regular Expression Pattern that matches JSON Numbers. This is primarily used
     * for
     * output to guarantee that we are always writing valid JSON.
     * The same as JSONObject.NUMBER_PATTERN
     */
    static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    // The same as JSONObject.indent
    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    @Override
    public boolean init() {
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    @Override
    public String name() {
        return "json";
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        JSONArray wrapper = new JSONArray();
        DataDstJava.DataDstObject data_obj = build_data(compiler);

        wrapper.put(data_obj.header);
        wrapper.put(data_obj.body);
        wrapper.put(data_obj.data_message_type);

        try {
            String encoded = stringify(wrapper, ProgramOptions.getInstance().prettyIndent).toString();

            // 带编码的输出
            String encoding = SchemeConf.getInstance().getKey().getEncoding();
            if (null == encoding || encoding.isEmpty())
                return encoded.getBytes();
            return encoded.getBytes(Charset.forName(encoding));
        } catch (JSONException e) {
            setLastErrorMessage(
                    "stringify JSON object failed, %s", e.getMessage());
            throw new ConvException(getLastErrorMessage());
        }
    }

    @Override
    public final DataDstWriterNode compile() {
        this.logErrorMessage("json can not be protocol description.");
        return null;
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    @Override
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        JSONObject wrapper;

        if (null != data) {
            wrapper = new JSONObject(data);
        } else {
            wrapper = new JSONObject();
        }

        try {
            String encoded = stringify(wrapper, ProgramOptions.getInstance().prettyIndent).toString();

            // 带编码的输出
            String encoding = SchemeConf.getInstance().getKey().getEncoding();
            if (null == encoding || encoding.isEmpty())
                return encoded.getBytes();
            return encoded.getBytes(Charset.forName(encoding));
        } catch (JSONException e) {
            setLastErrorMessage(
                    "stringify JSON object failed, %s", e.getMessage());
            throw new ConvException(getLastErrorMessage());
        }
    }

    static public final Writer stringify(Object value, int indentFactor) throws JSONException {
        StringWriter writer = new StringWriter();
        synchronized (writer.getBuffer()) {
            return stringifyWriteValue(writer, value, indentFactor, 0);
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    static Writer stringifyWriteObject(Writer writer, JSONObject value, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean needsComma = false;
            final int length = value.length();
            writer.write('{');

            if (length == 1) {
                final String key = value.keys().next();
                writer.write(JSONObject.quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                try {
                    writer = stringifyWriteValue(writer, value.opt(key), indentFactor, indent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                }
            } else if (length != 0) {
                final int newIndent = indent + indentFactor;
                Map<String, Object> mp = value.toMap();
                ArrayList<Map.Entry<String, ?>> sorted_array = new ArrayList<Map.Entry<String, ?>>();
                sorted_array.ensureCapacity(mp.size());
                sorted_array.addAll(mp.entrySet());
                sorted_array.sort((l, r) -> {
                    return l.getKey().compareTo(r.getKey());
                });

                for (final Map.Entry<String, ?> entry : sorted_array) {
                    if (needsComma) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newIndent);
                    final String key = entry.getKey();
                    writer.write(JSONObject.quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    try {
                        writer = stringifyWriteValue(writer, entry.getValue(), indentFactor, newIndent);
                    } catch (Exception e) {
                        throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                    }
                    needsComma = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new JSONException(exception);
        }
    }

    static Writer stringifyWriteArray(Writer writer, JSONArray value, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean needsComma = false;
            int length = value.length();
            writer.write('[');

            if (length == 1) {
                try {
                    writer = stringifyWriteValue(writer, value.opt(0),
                            indentFactor, indent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: 0", e);
                }
            } else if (length != 0) {
                final int newIndent = indent + indentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (needsComma) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newIndent);
                    try {
                        writer = stringifyWriteValue(writer, value.opt(i),
                                indentFactor, newIndent);
                    } catch (Exception e) {
                        throw new JSONException("Unable to write JSONArray value at index: " + i, e);
                    }
                    needsComma = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write(']');
            return writer;
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }

    static Writer stringifyWriteValue(Writer writer, Object value, int indentFactor, int indent)
            throws JSONException {
        try {
            if (value == null || value.equals(null)) {
                writer.write("null");
            } else if (value instanceof String) {
                writer.write(JSONObject.quote(value.toString()));
            } else if (value instanceof JSONString) {
                Object o;
                try {
                    o = ((JSONString) value).toJSONString();
                } catch (Exception e) {
                    throw new JSONException(e);
                }
                writer.write(o != null ? o.toString() : JSONObject.quote(value.toString()));
            } else if (value instanceof Number) {
                // not all Numbers may match actual JSON Numbers. i.e. fractions or Imaginary
                final String numberAsString = JSONObject.numberToString((Number) value);
                if (NUMBER_PATTERN.matcher(numberAsString).matches()) {
                    // IEEE 754
                    if (SchemeConf.getInstance().getJsonOptions().enableLargeNumberAsString && value instanceof Long
                            && ((long) value > ((1L << 53) - 1) || (long) value < -((1L << 53) - 1))) {
                        JSONObject.quote(numberAsString, writer);
                    } else {
                        writer.write(numberAsString);
                    }
                } else {
                    // The Number value is not a valid JSON number.
                    // Instead we will quote it as a string
                    JSONObject.quote(numberAsString, writer);
                }
            } else if (value instanceof Boolean) {
                writer.write(value.toString());
            } else if (value instanceof Enum<?>) {
                writer.write(JSONObject.quote(((Enum<?>) value).name()));
            } else if (value instanceof JSONObject) {
                writer = stringifyWriteObject(writer, ((JSONObject) value), indentFactor, indent);
            } else if (value instanceof JSONArray) {
                writer = stringifyWriteArray(writer, ((JSONArray) value), indentFactor, indent);
            } else if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                writer = stringifyWriteObject(writer, new JSONObject(map), indentFactor, indent);
            } else if (value instanceof Collection) {
                Collection<?> coll = (Collection<?>) value;
                writer = stringifyWriteArray(writer, new JSONArray(coll), indentFactor, indent);
            } else if (value.getClass().isArray()) {
                writer = stringifyWriteArray(writer, new JSONArray(value), indentFactor, indent);
            } else {
                JSONObject.quote(value.toString(), writer);
            }
        } catch (IOException exception) {
            throw new JSONException(exception);
        }

        return writer;
    }
}
