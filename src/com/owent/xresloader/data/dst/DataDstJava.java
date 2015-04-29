package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class DataDstJava extends DataDstImpl {
    public class DataDstObject {
        public HashMap<String, Object> header = new HashMap<String, Object>();
        public HashMap<String, List<Object> > body = new HashMap<String, List<Object>>();
    }

    protected DataDstObject build_data(DataDstWriterNode desc) {
        DataDstObject ret = new DataDstObject();

        ret.header.put("xrex_ver", ProgramOptions.getInstance().getVersion());
        ret.header.put("data_ver", ProgramOptions.getInstance().getVersion());
        ret.header.put("count", DataSrcImpl.getOurInstance().getRecordNumber());
        ret.header.put("hash_code", "no hash code");

        List<Object> item_list = new ArrayList<Object>();
        ret.body.put(SchemeConf.getInstance().getProtoName(), item_list);

        while (DataSrcImpl.getOurInstance().next()) {
            item_list.add(writeData(desc, ""));
        }

        return ret;
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
