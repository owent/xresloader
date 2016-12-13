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

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;

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
                HashMap<String, Object> msg = new HashMap<String, Object>();
                if(dumpMessage(msg, desc)) {
                    item_list.add(msg);
                }
            }
        }

        return ret;
    }

    private void dumpDefault(HashMap<String, Object> builder, DataDstWriterNode desc, String field_name,  DataDstWriterNode.DataDstChildrenNode as_child) {
        Object val = null;
        switch (desc.getType()) {
            case INT:
                val = Integer.valueOf(0);
                break;
            case LONG:
                val = Long.valueOf(0);
                break;
            case BOOLEAN:
                val = Boolean.FALSE;
                break;
            case STRING:
                val = "";
                break;
            case BYTES:
                val = new byte[0];
                break;
            case FLOAT:
                val = Float.valueOf(0);
                break;
            case DOUBLE:
                val = Double.valueOf(0);
                break;
            case MESSAGE: {
                HashMap<String, Object> sub_msg = new HashMap<String, Object>();
                for(Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> sub_item: desc.getChildren().entrySet()) {
                    for (DataDstWriterNode sub_desc : sub_item.getValue().nodes) {
                        dumpDefault(sub_msg, sub_desc, sub_item.getKey(), sub_item.getValue());
                    }
                }
                break;
            }
        }

        if (null == val) {
            ProgramOptions.getLoger().error("serialize failed. %s is not supported for java default value", desc.getType().toString());
            return;
        }

        if (as_child.isList) {
            ArrayList<Object> old = (ArrayList<Object>)builder.getOrDefault(field_name,null);
            if (null == old) {
                old = new ArrayList<Object>();
                builder.put(field_name, old);
            }
            old.add(val);
        } else {
            builder.put(field_name, val);
        }
    }


    /**
     * 转储数据到builder
     * @param builder 转储目标
     * @param node message的描述结构
     * @return 有数据则返回true
     * @throws ConvException
     */
    private boolean dumpMessage(HashMap<String, Object> builder, DataDstWriterNode node) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            for (DataDstWriterNode child: c.getValue().nodes) {
                if (dumpField(builder, child, c.getKey(), c.getValue())) {
                    ret = true;
                }
            }
        }

        return ret;
    }

    private boolean dumpField(HashMap<String, Object> builder, DataDstWriterNode desc, String field_name, DataDstWriterNode.DataDstChildrenNode as_child) throws ConvException {
        if (null == desc.identify && DataDstWriterNode.JAVA_TYPE.MESSAGE != desc.getType()) {
            dumpDefault(builder, desc, field_name, as_child);
            return false;
        }

        Object val = null;
        switch (desc.getType())
        {
            case INT: {
                DataContainer<Integer> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Integer.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case LONG:{
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Long.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case FLOAT:{
                DataContainer<Float> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Float.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case DOUBLE:{
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Double.valueOf(0));
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case BOOLEAN:{
                DataContainer<Boolean> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, Boolean.FALSE);
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case STRING: {
                DataContainer<String> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
                break;
            }

            case BYTES: {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if(null != res && res.valid) {
                    String encoding = SchemeConf.getInstance().getKey().getEncoding();
                    if (null == encoding || encoding.isEmpty()) {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes());
                    } else {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes(Charset.forName(encoding)));
                    }
                }
                break;
            }

            case MESSAGE: {
                HashMap<String, Object> node = new HashMap<String, Object>();
                if (dumpMessage(node, desc)) {
                    val = node;
                }
                break;
            }

            default:
                break;
        }

        if (null == val) {
            if (as_child.isRequired) {
                dumpDefault(builder, desc, field_name, as_child);
            }
            return false;
        }

        if (as_child.isList) {
            ArrayList<Object> old = (ArrayList<Object>)builder.getOrDefault(field_name,null);
            if (null == old) {
                old = new ArrayList<Object>();
                builder.put(field_name, old);
            }
            old.add(val);
        } else {
            builder.put(field_name, val);
        }

        return true;
    }
}
