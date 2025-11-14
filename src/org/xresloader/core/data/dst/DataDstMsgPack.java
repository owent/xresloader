package org.xresloader.core.data.dst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageBufferPacker;
import org.xresloader.core.data.err.ConvException;

/**
 * Created by owentou on 2014/10/10.
 */
public class DataDstMsgPack extends DataDstJava {

    @Override
    public boolean init() {
        return true;
    }

    /**
     * @return 协议处理器名字
     */
    @Override
    public String name() {
        return "msgpack";
    }

    private void writeData(MessageBufferPacker packer, Object data) throws IOException {
        // null
        if (null == data) {
            packer.packNil();
            return;
        }

        // 字符串&二进制
        if (data instanceof String) {
            packer.packString((String) data);
            return;
        }

        // 数字
        // 枚举值已被转为Java Long，会在这里执行
        if (data instanceof Integer) {
            packer.packInt((Integer) data);
            return;
        }
        if (data instanceof Long) {
            packer.packLong((long) data);
            return;
        }
        if (data instanceof Short) {
            packer.packLong((short) data);
            return;
        }
        if (data instanceof Float) {
            packer.packFloat((float) data);
            return;
        }
        if (data instanceof Double) {
            packer.packDouble((double) data);
            return;
        }
        if (data instanceof Byte) {
            packer.packByte((byte) data);
            return;
        }
        if (data instanceof java.math.BigInteger) {
            packer.packBigInteger((java.math.BigInteger) data);
            return;
        }

        // 布尔
        if (data instanceof Boolean) {
            packer.packBoolean((Boolean) data);
            return;
        }

        // 列表
        if (data instanceof List<?>) {
            packer.packArrayHeader(((List<?>) data).size());
            for (Object subobj : (List<?>) data) {
                writeData(packer, subobj);
            }
            return;
        }

        // Hashmap
        if (data instanceof Map<?, ?>) {
            Map<?, ?> mp = (Map<?, ?>) data;

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

            packer.packMapHeader(sorted_array.size());
            for (Map.Entry<?, ?> item : sorted_array) {
                writeData(packer, item.getKey());
                writeData(packer, item.getValue());
            }

            return;
        }

        packer.packString(data.toString());
    }

    @Override
    public final byte[] build(DataDstImpl compiler) throws ConvException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        DataDstJava.DataDstObject data_obj = build_data(compiler);

        try {
            packer.packMapHeader(3);
            packer.packString("header");
            writeData(packer, data_obj.header);
            packer.packString("data_block");
            writeData(packer, data_obj.body);
            packer.packString("data_message_type");
            writeData(packer, data_obj.data_message_type);
        } catch (IOException e) {
            this.logErrorMessage("MessagePacker write failed.");
            e.printStackTrace();
        }

        return packer.toByteArray();
    }

    @Override
    public final DataDstWriterNode compile() {
        this.logErrorMessage("msgpack can not be protocol description.");
        return null;
    }

    /**
     * 转储常量数据
     * 
     * @return 常量数据,不支持的时候返回空
     */
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

        try {
            writeData(packer, data);
        } catch (IOException e) {
            this.logErrorMessage("MessagePacker write failed.");
            e.printStackTrace();
        }

        return packer.toByteArray();
    }
}
