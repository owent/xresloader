package org.xresloader.core.data.et;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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

    private DataETProcessor() {
    }

    public void reset() throws ConvException {
        ScriptEngineManager mgr = new ScriptEngineManager();
        scriptEngine = mgr.getEngineByName("javascript");
        try {
            if (scriptEngine != null) {
                scriptEngine.put("gOurInstance", DataSrcImpl.getOurInstance());
                scriptEngine.put("gSchemeConf", SchemeConf.getInstance());
                scriptEngine.eval(new FileReader(new File(SchemeConf.getInstance().getCallbackScriptPath())));
            }
        } catch (ScriptException e) {
            throw new ConvException(e.toString());
        } catch (FileNotFoundException e) {
            scriptEngine = null;
        }
        invocable = null;
        if (scriptEngine instanceof Invocable) {
            invocable = (Invocable)scriptEngine;
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

    Object transformJava2Pb(FieldDescriptor fd, Object obj) {
        Object val = null;
        if (obj == null) {
            return null;
        }
        switch (fd.getType()) {
            case DOUBLE:
                val = ((Number)obj).doubleValue();
                break;
            case FLOAT:
                val = ((Number)obj).floatValue();
                break;
            case FIXED32:
            case SFIXED32:
            case SINT32:
            case INT32:
            case UINT32:
                val = ((Number)obj).intValue();
                break;
            case FIXED64:
            case SFIXED64:
            case SINT64:
            case INT64:
            case UINT64:
                val = ((Number) obj).longValue();
                break;
            case ENUM:
                val = fd.getEnumType().findValueByNumber((int)obj);
                break;
            case BOOL:
                val = (boolean)obj;
                break;
            case STRING:
                val = obj.toString();
                break;
            case BYTES:
                val = obj.toString().getBytes();
                break;
            default: {
                logErrorMessage("Plain type %s of %s.%s not supported", fd.getType().toString(), fd.getFullName(),
                        fd.getName());
            }

        }
        return val;
    }

    @SuppressWarnings("unchecked")
    private void FillMap2Msg(Descriptor msgDesc, HashMap<String, Object> src, DynamicMessage.Builder builder) throws ConvException {
        for (FieldDescriptor fd : msgDesc.getFields()) {
            try {
                Object curValue = src.get(fd.getName());
                if (curValue != null) {
                    if (fd.getType() == Type.MESSAGE) {
                        if (fd.isMapField()) {
                            for (Map.Entry<String, Object> mapItem : ((SpecialInnerHashMap<String, Object>) curValue)
                                    .entrySet()) {
                                // Map类型是List<MapEntry>
                                Descriptor mapKVDesc = fd.getMessageType();
                                DynamicMessage.Builder pushMapItem = DynamicMessage.newBuilder(mapKVDesc);
                                if (mapItem.getValue() instanceof Map) {
                                    DynamicMessage.Builder subMsgBuild = DynamicMessage
                                            .newBuilder(mapKVDesc.findFieldByName("value").getMessageType());
                                    FillMap2Msg(mapKVDesc.findFieldByName("value").getMessageType(),
                                            (HashMap<String, Object>) mapItem.getValue(), subMsgBuild);
                                    pushMapItem.setField(mapKVDesc.findFieldByName("key"), mapItem.getKey());
                                    pushMapItem.setField(mapKVDesc.findFieldByName("value"), subMsgBuild.build());
                                } else {
                                    pushMapItem.setField(mapKVDesc.findFieldByName("key"), mapItem.getKey());
                                    pushMapItem.setField(mapKVDesc.findFieldByName("value"),
                                            transformJava2Pb(mapKVDesc.findFieldByName("value"), mapItem.getValue()));
                                }
                                builder.addRepeatedField(fd, pushMapItem.build());
                            }
                        } else if (fd.isRepeated()) {
                            for (Object arrItem : (ArrayList<?>) curValue) {
                                DynamicMessage.Builder subMsgBuild = DynamicMessage.newBuilder(fd.getMessageType());
                                FillMap2Msg(fd.getMessageType(), (HashMap<String, Object>) arrItem, subMsgBuild);
                                builder.addRepeatedField(fd, subMsgBuild.build());
                            }
                        } else {
                            DynamicMessage.Builder subMsgBuild = DynamicMessage.newBuilder(fd.getMessageType());
                            FillMap2Msg(fd.getMessageType(), (HashMap<String, Object>) curValue, subMsgBuild);
                            builder.setField(fd, subMsgBuild.build());
                        }
                    } else {
                        if (fd.isRepeated()) {
                            for (Object arrItem : (ArrayList<?>) curValue) {
                                builder.addRepeatedField(fd, transformJava2Pb(fd, arrItem));
                            }
                        } else {
                            builder.setField(fd, transformJava2Pb(fd, curValue));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ConvException(String.format("FillMap2PbMsg failed\nFullName: %s\n%s\n", fd.getFullName(), e.toString()));
            }
        }
    }

    public DynamicMessage.Builder dumpPbMsg(Descriptor currentMsgDesc, DataDstWriterNode node) throws ConvException {
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(currentMsgDesc);
        HashMap<String, Object> msgMap = new HashMap<>();
        if (dumpMapMsg(msgMap, node) == false) {
            return null;
        }
        FillMap2Msg(currentMsgDesc, msgMap, builder);
        return builder;
    }

    public boolean dumpMapMsg(HashMap<String, Object> builder, DataDstWriterNode node) throws ConvException {
        boolean dumpSucceed = dumpMessage(builder, node);
        if (lastDataSourceFile != DataSrcImpl.getOurInstance().getCurrentFileName()
                || lastDataSourceTable != DataSrcImpl.getOurInstance().getCurrentTableName()) {
            reset();
            initNextTable();
            lastDataSourceFile = DataSrcImpl.getOurInstance().getCurrentFileName();
            lastDataSourceTable = DataSrcImpl.getOurInstance().getCurrentTableName();
        }
        if (invocable instanceof Invocable) {
            try {
                Object ret = invocable.invokeFunction("currentMessageCallback", builder, node.getTypeDescriptor().getRawDescriptor());
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
