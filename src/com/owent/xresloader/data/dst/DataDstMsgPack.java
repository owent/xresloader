package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstMsgPack extends DataDstImpl {

    private MessagePack msgpack = null;

    @Override
    public boolean init() {
        try {
            msgpack = new MessagePack();
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public final byte[] build(DataDstWriterNode desc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgpack.createPacker(out);

        HashMap<String, Object> header = new HashMap<String, Object>();
        HashMap<String, List<Object>> body = new HashMap<String, List<Object>>();

        header.put("xrex_ver", ProgramOptions.getInstance().getVersion());
        header.put("data_ver", ProgramOptions.getInstance().getVersion());
        header.put("count", DataSrcImpl.getOurInstance().getRecordNumber());
        header.put("hash_code", "message pack has no hash code");

        List<Object> item_list = new ArrayList<Object>();
        body.put(SchemeConf.getInstance().getProtoName(), item_list);

        while (DataSrcImpl.getOurInstance().next()) {
            item_list.add(writeData(desc, ""));
        }

        try {
            packer.write(header);
            packer.write(body);
        } catch (IOException e) {
            System.err.println("[ERROR] MessagePacker write failed.");
            e.printStackTrace();
        }
        // 带编码的输出
//        String encoding = SchemeConf.getInstance().getKey().getEncoding();
//        if (null == encoding || encoding.isEmpty())
//            return sb.toString().getBytes();
        return out.toByteArray();
    }

    @Override
    public final DataDstWriterNode compile() {
        System.err.println("[ERROR] lua can not be protocol description.");
        return null;
    }


    private Object writeData(DataDstWriterNode desc, String prefix) {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        for (Map.Entry<String, DataDstWriterNode> c : desc.getChildren().entrySet()) {
            String _name = DataDstWriterNode.makeNodeName(c.getKey());
            if (c.getValue().isList()) {
                List<Object> item_list = new ArrayList<Object>();

                for (int i = 0; i < c.getValue().getListCount(); ++i) {
                    String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey(), i);
                    item_list.add(writeOneData(c.getValue(), new_prefix));
                }

                ret.put(_name, item_list);
            } else {
                String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey());
                ret.put(_name, writeOneData(c.getValue(), new_prefix));
            }

        }

        return ret;
    }

    private Object writeOneData(DataDstWriterNode desc, String prefix) {
        switch (desc.getType()) {
            case INT:
            case LONG:
                return DataSrcImpl.getOurInstance().getValue(prefix, new Long(0));

            case FLOAT:
            case DOUBLE:
                return DataSrcImpl.getOurInstance().getValue(prefix, new Double(0));

            case BOOLEAN:
                return DataSrcImpl.getOurInstance().getValue(prefix, Boolean.FALSE);

            case STRING:
                return DataSrcImpl.getOurInstance().getValue(prefix, "");

            case BYTE_STRING:
                return DataSrcImpl.getOurInstance().getValue(prefix, "").getBytes();

            case ENUM:
                return DataSrcImpl.getOurInstance().getValue(prefix, new Long(0));

            case OBJECT:
                return writeData(desc, prefix);

            default:
                return null;
        }
    }
}
