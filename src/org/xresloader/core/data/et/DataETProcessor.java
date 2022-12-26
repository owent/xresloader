package org.xresloader.core.data.et;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.xresloader.core.data.dst.DataDstJava;
import org.xresloader.core.data.dst.DataDstWriterNode;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.scheme.SchemeConf;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

/**
 * Created by anakin on 2020/10/20.
 */
public class DataETProcessor extends DataDstJava {

    static private DataETProcessor instance;

    private ScriptEngine scriptEngine = null;
    private Invocable invocable = null;
    private String lastDataSourceFile = "";
    private String lastDataSourceTable = "";
    private String lastOutputFile = "";

    private DataETProcessor() {
        ScriptEngineManager mgr = new ScriptEngineManager();
        scriptEngine = mgr.getEngineByName("javascript");
    }

    public void reset() throws ConvException {
        boolean engineInitSucceed = false;
        try {
            if (scriptEngine != null && !SchemeConf.getInstance().getCallbackScriptPath().isEmpty()) {
                scriptEngine.setContext(new SimpleScriptContext());
                scriptEngine.put("gOurInstance", DataSrcImpl.getOurInstance());
                scriptEngine.put("gSchemeConf", SchemeConf.getInstance());
                scriptEngine.eval(new FileReader(new File(SchemeConf.getInstance().getCallbackScriptPath())));
                engineInitSucceed = true;
            }
        } catch (ScriptException | FileNotFoundException e) {
            throw new ConvException(e.toString());
        }
        invocable = null;
        if (engineInitSucceed) {
            invocable = (Invocable) scriptEngine;
        }
    }

    public void initNextTable() throws ConvException {
        try {
            if (invocable != null) {
                invocable.invokeFunction("initDataSource");
            }
        } catch (NoSuchMethodException | ScriptException e) {
            throw (new ConvException(e.toString()));
        }
    }

    public static DataETProcessor getInstance() {
        if (instance == null) {
            instance = new DataETProcessor();
        }
        return instance;
    }

    Object transformJava2Pb(FieldDescriptor fd, Object obj) throws UnsupportedEncodingException, ConvException {
        Object val = null;
        if (obj == null) {
            return null;
        }
        switch (fd.getType()) {
            case DOUBLE:
                if (obj instanceof Number) {
                    val = ((Number) obj).doubleValue();
                } else if (obj instanceof String) {
                    val = Double.valueOf((String) obj);
                } else {
                    throw new ConvException(
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "");
                }
                break;
            case FLOAT:
                if (obj instanceof Number) {
                    val = ((Number) obj).floatValue();
                } else if (obj instanceof String) {
                    val = Float.valueOf((String) obj);
                } else {
                    throw new ConvException(
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "");
                }
                break;
            case FIXED32:
            case SFIXED32:
            case SINT32:
            case INT32:
            case UINT32:
                if (obj instanceof Number) {
                    val = ((Number) obj).intValue();
                } else if (obj instanceof String) {
                    val = Integer.valueOf((String) obj);
                } else {
                    throw new ConvException(
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "");
                }
                break;
            case FIXED64:
            case SFIXED64:
            case SINT64:
            case INT64:
            case UINT64:
                if (obj instanceof Number) {
                    val = ((Number) obj).longValue();
                } else if (obj instanceof String) {
                    val = Long.valueOf((String) obj);
                } else {
                    throw new ConvException(
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "");
                }
                break;
            case ENUM:
                if (obj instanceof Number) {
                    val = fd.getEnumType().findValueByNumber(((Number) obj).intValue());
                } else if (obj instanceof String) {
                    val = fd.getEnumType().findValueByNumber(Integer.valueOf((String) obj));
                }
                break;
            case BOOL:
                val = DataSrcImpl.getBooleanFromString(obj.toString());
                break;
            case STRING:
                val = obj.toString();
                break;
            case BYTES:
                if (SchemeConf.getInstance().getKey().getEncoding() != null) {
                    val = obj.toString().getBytes(SchemeConf.getInstance().getKey().getEncoding());
                } else {
                    val = obj.toString().getBytes();
                }
                break;
            default: {
                logErrorMessage("Plain type %s of %s.%s not supported", fd.getType().toString(), fd.getFullName(),
                        fd.getName());
            }

        }
        return val;
    }

    /**
     * @param msgDesc
     * @param src     Map<String, Object>，配置行按类型转换后的Map
     * @param builder 要填充的Message对象
     * @throws ConvException
     */
    private void fillMap2Message(Descriptor msgDesc, Object src, DynamicMessage.Builder builder) throws ConvException {
        Map<?, ?> srcMap;
        if (src instanceof Map<?, ?>) {
            srcMap = (Map<?, ?>) src;
        } else {
            return;
        }
        for (FieldDescriptor fd : msgDesc.getFields()) {
            try {
                Object curValue = srcMap.getOrDefault(fd.getName(), null);
                if (curValue == null)
                    continue;
                if (fd.isMapField()) {
                    for (Map.Entry<?, ?> mapItem : ((SpecialInnerHashMap<?, ?>) curValue).entrySet()) {
                        // Map类型是List<MapEntry>，只能通过MapEntry.value类型判断是否为Message
                        Descriptor mapKVDesc = fd.getMessageType();
                        DynamicMessage.Builder pushMapItem = DynamicMessage.newBuilder(mapKVDesc);
                        var keyDesc = mapKVDesc.findFieldByName("key");
                        pushMapItem.setField(keyDesc, transformJava2Pb(keyDesc, mapItem.getKey()));
                        var valueDesc = mapKVDesc.findFieldByName("value");
                        if (valueDesc.getType() == Type.MESSAGE) {
                            DynamicMessage.Builder subMsgBuild = DynamicMessage.newBuilder(valueDesc.getMessageType());
                            fillMap2Message(valueDesc.getMessageType(), mapItem.getValue(), subMsgBuild);
                            pushMapItem.setField(valueDesc, subMsgBuild.build());
                        } else {
                            pushMapItem.setField(valueDesc, transformJava2Pb(valueDesc, mapItem.getValue()));
                        }
                        builder.addRepeatedField(fd, pushMapItem.build());
                    }
                } else if (fd.isRepeated()) {
                    for (Object arrItem : (ArrayList<?>) curValue) {
                        if (fd.getType() == Type.MESSAGE) {
                            DynamicMessage.Builder subMsgBuild = DynamicMessage.newBuilder(fd.getMessageType());
                            fillMap2Message(fd.getMessageType(), arrItem, subMsgBuild);
                            builder.addRepeatedField(fd, subMsgBuild.build());
                        } else {
                            builder.addRepeatedField(fd, transformJava2Pb(fd, arrItem));
                        }
                    }
                } else {
                    if (fd.getType() == Type.MESSAGE) {
                        DynamicMessage.Builder subMsgBuild = DynamicMessage.newBuilder(fd.getMessageType());
                        fillMap2Message(fd.getMessageType(), curValue, subMsgBuild);
                        builder.setField(fd, subMsgBuild.build());
                    } else {
                        builder.setField(fd, transformJava2Pb(fd, curValue));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ConvException(
                        String.format("FillMap2PbMsg failed\nFullName: %s\n%s\n", fd.getFullName(), e.toString()));
            }
        }
    }

    public DynamicMessage.Builder dumpPbMessage(Descriptor currentMsgDesc, DataDstWriterNode node)
            throws ConvException {
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(currentMsgDesc);
        HashMap<String, Object> msgMap = new HashMap<>();
        if (dumpMapMessage(msgMap, node) == false) {
            return null;
        }
        fillMap2Message(currentMsgDesc, msgMap, builder);
        return builder;
    }

    public boolean dumpMapMessage(HashMap<String, Object> builder, DataDstWriterNode node) throws ConvException {
        boolean dumpSucceed = dumpMessage(builder, node);
        if (dumpSucceed == false) {
            return dumpSucceed;
        }
        if (lastOutputFile != SchemeConf.getInstance().getOutputFileAbsPath()) {
            lastOutputFile = SchemeConf.getInstance().getOutputFileAbsPath();
            reset();
        }
        if (lastDataSourceFile != DataSrcImpl.getOurInstance().getCurrentFileName()
                || lastDataSourceTable != DataSrcImpl.getOurInstance().getCurrentTableName()) {
            initNextTable();
            lastDataSourceFile = DataSrcImpl.getOurInstance().getCurrentFileName();
            lastDataSourceTable = DataSrcImpl.getOurInstance().getCurrentTableName();
        }
        if (invocable instanceof Invocable) {
            try {
                Object ret = invocable.invokeFunction("currentMessageCallback", builder, node.getTypeDescriptor());
                if (ret instanceof Boolean && ret.equals(false)) {
                    throw new ConvException("Script return " + ret);
                }
            } catch (ScriptException | NoSuchMethodException e) {
                throw new ConvException(e.toString());
            }
        }
        return dumpSucceed;
    }

}
