package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataContainer;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstLua extends DataDstImpl {

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        StringBuffer sb = new StringBuffer();

        sb.append("return {\n");
        sb.append("    [1] = {\n");
        sb.append("        xrex_ver = \"" + ProgramOptions.getInstance().getVersion() + "\",\n");
        sb.append("        data_ver = \"" + ProgramOptions.getInstance().getVersion() + "\",\n");
        sb.append("        count = " + DataSrcImpl.getOurInstance().getRecordNumber() + ",\n");
        sb.append("        hash_code = \"lua data has no hash code\"\n");
        sb.append("    },\n");
        sb.append("    " + SchemeConf.getInstance().getProtoName() + " = {\n");

        while (DataSrcImpl.getOurInstance().next()) {
            sb.append("        {\n");
            writeData(sb, desc, "            ", "");
            sb.append("        },\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

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


    private boolean writeData(StringBuffer sb, DataDstWriterNode desc, String indent, String prefix) {
        boolean ret = false;
        int  old_len = sb.length();

        for (Map.Entry<String, DataDstWriterNode> c : desc.getChildren().entrySet()) {
            String _name = DataDstWriterNode.makeNodeName(c.getKey());
            if (c.getValue().isList()) {
                sb.append(indent);
                sb.append(_name + " = {\n");

                for (int i = 0; i < c.getValue().getListCount(); ++i) {
                    String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey(), i);
                    int bak_len = sb.length();
                    if(writeOneData(sb, c.getValue(), indent + "    ", new_prefix, "")) {
                        ret = true;
                    } else if(false == ProgramOptions.getInstance().enbleEmptyList) {
                        sb.setLength(bak_len);
                    }
                }
                sb.append(indent);
                sb.append("},\n");
            } else {
                String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey());
                if(writeOneData(sb, c.getValue(), indent, new_prefix, DataDstWriterNode.makeNodeName(c.getKey()))) {
                    ret = true;
                }
            }

        }

        if (false == ret) {
            sb.setLength(old_len);
        }
        return ret;
    }

    private boolean writeOneData(StringBuffer sb, DataDstWriterNode desc, String indent, String prefix, String _name) {
        boolean ret = false;

        sb.append(indent);
        if (!_name.isEmpty())
            sb.append(_name + " = ");

        switch (desc.getType()) {
            case INT:
            case LONG: {
                DataContainer<Long> res = DataSrcImpl.getOurInstance().getValue(prefix, new Long(0));
                ret = res.valid;
                sb.append(res.get());
                break;
            }

            case FLOAT:
            case DOUBLE: {
                DataContainer<Double> res = DataSrcImpl.getOurInstance().getValue(prefix, new Double(0));
                ret = res.valid;
                sb.append(res.get());
                break;
            }

            case BOOLEAN: {
                DataContainer<Boolean> res = DataSrcImpl.getOurInstance().getValue(prefix, Boolean.FALSE);
                ret = res.valid;
                sb.append(res.get().toString());
                break;
            }

            case STRING: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(prefix, "");
                ret = res.valid;

                sb.append("\"");
                sb.append(res.get()
                    .replaceAll("\"", "\\\"")
                    .replaceAll("\\\\", "\\\\")
                );
                sb.append("\"");
                break;
            }

            case BYTE_STRING: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(prefix, "");
                ret = res.valid;

                sb.append("\"");

                sb.append(
                    Hex.encodeHexString(
                        res.get().getBytes()
                    )
                );
                sb.append("\"");
                break;
            }

            case ENUM: {
                DataContainer<Long> res = DataSrcImpl.getOurInstance().getValue(prefix, new Long(0));
                ret = res.valid;
                sb.append(res.get());
                break;
            }

            case OBJECT: {
                sb.append(" {\n");
                ret = writeData(sb, desc, indent + "    ", prefix);
                sb.append(indent);
                sb.append("}");

                break;
            }

            default: {
                sb.append("nil");
                break;
            }
        }

        sb.append(",\n");

        return ret;
    }
}
