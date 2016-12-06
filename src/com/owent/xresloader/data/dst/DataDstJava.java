package com.owent.xresloader.data.dst;

import com.owent.xresloader.ProgramOptions;
import com.owent.xresloader.data.err.ConvException;
import com.owent.xresloader.data.src.DataContainer;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by owentou on 2015/04/29.
 */
public abstract class DataDstJava extends DataDstImpl {
    private class DataEntry {
        public boolean valid =  false;
        public Object value = null;

        public <T> void set(DataContainer<T> v) {
            valid = v.valid;
            value = v.value;
        }
    }

    /**
     * @return 协议处理器名字
     */
    public String name() {
        return "java";
    }

    public class DataDstObject {
        public HashMap<String, Object> header = new HashMap<String, Object>();
        public HashMap<String, List<Object> > body = new HashMap<String, List<Object>>();
    }

    protected DataDstObject build_data(DataDstImpl compiler) throws ConvException {
        DataDstObject ret = new DataDstObject();

        ret.header.put("xres_ver", ProgramOptions.getInstance().getVersion());
        ret.header.put("data_ver", ProgramOptions.getInstance().getVersion());
        ret.header.put("count", DataSrcImpl.getOurInstance().getRecordNumber());
        ret.header.put("hash_code", "no hash code");

        List<Object> item_list = new ArrayList<Object>();
        ret.body.put(SchemeConf.getInstance().getProtoName(), item_list);

        while (DataSrcImpl.getOurInstance().next_table()) {
            // 生成描述集
            DataDstWriterNode desc = compiler.compile();

            while(DataSrcImpl.getOurInstance().next_row()) {
                DataEntry conv_data = writeData(desc, "");
                if (conv_data.valid) {
                    item_list.add(conv_data.value);
                }
            }
        }

        return ret;
    }

    private DataEntry writeData(DataDstWriterNode desc, String prefix) throws ConvException {
        DataEntry ret = new DataEntry();
        HashMap<String, Object> ret_val = new HashMap<String, Object>();
        ret.value = ret_val;
        for (Map.Entry<String, DataDstWriterNode> c : desc.getChildren().entrySet()) {
            String _name = DataDstWriterNode.makeNodeName(c.getKey());
            if (c.getValue().isList()) {
                List<Object> item_list = new ArrayList<Object>();

                for (int i = 0; i < c.getValue().getListCount(); ++i) {
                    String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey(), i);
                    DataEntry ele = writeOneData(c.getValue(), new_prefix);
                    if (null != ele && (ele.valid || ProgramOptions.getInstance().enbleEmptyList)) {
                        item_list.add(ele.value);
                        ret.valid = ret.valid || ele.valid;
                    }

                }

                if(!item_list.isEmpty()) {
                    ret_val.put(_name, item_list);
                }
            } else {
                String new_prefix = DataDstWriterNode.makeChildPath(prefix, c.getKey());
                DataEntry ele = writeOneData(c.getValue(), new_prefix);
                if (null != ele && ele.valid) {
                    ret_val.put(_name, ele.value);
                    ret.valid = true;
                }
            }

        }

        return ret;
    }

    private DataEntry writeOneData(DataDstWriterNode desc, String prefix) throws ConvException {
        String encoding = SchemeConf.getInstance().getKey().getEncoding();
        DataEntry ret = new DataEntry();

        switch (desc.getType()) {
            case INT:
            case LONG: {
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Long(0)));
                break;
            }

            case FLOAT:
            case DOUBLE:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Double(0)));
                break;
            }

            case BOOLEAN:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, Boolean.FALSE));
                break;
            }

            case STRING:{
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, ""));
                break;
            }

            case BYTE_STRING: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(prefix, "");
                ret.valid = res.valid;
                if (null == encoding || encoding.isEmpty()) {
                    ret.value = com.google.protobuf.ByteString.copyFrom(res.value.getBytes());
                } else {
                    ret.value = com.google.protobuf.ByteString.copyFrom(res.value.getBytes(Charset.forName(encoding)));
                }
                break;
            }

            case ENUM: {
                ret.set(DataSrcImpl.getOurInstance().getValue(prefix, new Long(0)));
                break;
            }

            case OBJECT: {
                ret = writeData(desc, prefix);
                break;
            }

            default:
                break;
        }

        return ret;
    }
}
