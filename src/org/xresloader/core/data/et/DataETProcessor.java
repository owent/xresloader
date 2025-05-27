package org.xresloader.core.data.et;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.Bindings;

import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.xresloader.core.ProgramOptions;
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
    private Object undefinedObject = null;
    private Class<?> undefinedClass = null;

    private DataETProcessor() throws ConvException {
        ScriptEngineManager mgr = new ScriptEngineManager();
        for (String name : new String[] { "nashorn", "javascript", "js", "JavaScript", "rhino", "graal.js" }) {
            if (scriptEngine != null) {
                break;
            }
            scriptEngine = mgr.getEngineByName(name);

            if (scriptEngine != null && name.equals("graal.js")) {
                Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
                bindings.put("polyglot.js.allowHostAccess", true);
                bindings.put("polyglot.js.allowNativeAccess", true);
                bindings.put("polyglot.js.allowIO", true);
                bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
                bindings.put("polyglot.js.allowHostClassLoading", true);
                bindings.put("polyglot.js.allowAllAccess", true);
            }
        }

        if (scriptEngine == null) {
            ProgramOptions.getLoger().warn(
                    "Failed to load org.openjdk.nashorn from ScriptEngineManager(classpath=%s), try to call NashornScriptEngineFactory directly",
                    System.getProperty("java.class.path"));
            scriptEngine = new org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory().getScriptEngine();
        }

        if (scriptEngine == null) {
            ProgramOptions.getLoger().warn(
                    "Failed to initialize script engine(nashorn,javascript,rhino,graal.js), script plugin will not be available");
        }
    }

    /**
     * private void initRhinoEngine(SimpleScriptContext sc) throws ScriptException {
     * Bindings scope = sc.getBindings(ScriptContext.ENGINE_SCOPE);
     * String loadFunc = "function load(file) { " +
     * " var FileReader = java.io.FileReader;" +
     * " var BufferedReader = java.io.BufferedReader;" +
     * " var reader = new BufferedReader(new FileReader(file));" +
     * " var line;" +
     * " var script = '';" +
     * " while ((line = reader.readLine()) !== null) { script += line + '\\n'; }" +
     * " reader.close();" +
     * " eval(script);" +
     * "};" +
     * "var global = this;";
     * 
     * scriptEngine.eval(loadFunc, scope);
     * 
     * scope.put("__moduleCache", new org.mozilla.javascript.NativeObject());
     * scope.put("__basePath", ProgramOptions.getInstance().dataSourceDirectory);
     * // new java.io.File().exists()
     * 
     * // 注入简化版的require函数
     * String simpleRequire = "function require(modulePath) {\n" +
     * " // 解析完整路径\n" +
     * " if (!modulePath.endsWith('.js')) modulePath += '.js';\n" +
     * " var fullPath = modulePath;\n" +
     * " for(var __i in __basePath) {\n" +
     * " var f = new java.io.File(__basePath[__i], modulePath);\n" +
     * " if (f.exists()) {\n" +
     * " fullPath = f.getCanonicalPath();\n" +
     * " break;\n" +
     * " }\n" +
     * " }\n" +
     * " \n" +
     * " // 检查缓存\n" +
     * " if (__moduleCache[fullPath]) return __moduleCache[fullPath].exports;\n" +
     * " \n" +
     * " // 创建模块\n" +
     * " var module = { exports: {} };\n" +
     * " __moduleCache[fullPath] = module;\n" +
     * " \n" +
     * " // 读取模块内容\n" +
     * " var content = '';\n" +
     * " var reader = new java.io.FileReader(fullPath);\n" +
     * " var buffer = new java.io.BufferedReader(reader);\n" +
     * " var line;\n" +
     * " while ((line = buffer.readLine()) !== null) content += line + '\\n';\n" +
     * " buffer.close();\n" +
     * " \n" +
     * " // 执行模块代码\n" +
     * " var dirName = new java.io.File(fullPath).getParent();\n" +
     * " var wrapper = new Function('exports', 'require', 'module', '__dirname',
     * '__filename', content);\n"
     * +
     * " wrapper(module.exports, require, module, dirName, fullPath);\n" +
     * " \n" +
     * " return module.exports;\n" +
     * "}";
     * scriptEngine.eval(simpleRequire, scope);
     * }
     **/

    public void reset() throws ConvException {
        // boolean engineInitSucceed = false;
        try {
            if (scriptEngine != null && !SchemeConf.getInstance().getCallbackScriptPath().isEmpty()) {
                SimpleScriptContext sc = new SimpleScriptContext();
                scriptEngine.setContext(sc);
                Bindings scope = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
                if (scope == null) {
                    scope = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
                }
                // scriptEngine.put("gOurInstance", DataSrcImpl.getOurInstance());
                // scriptEngine.put("gSchemeConf", SchemeConf.getInstance());
                scope.put("gOurInstance", DataSrcImpl.getOurInstance());
                scope.put("gSchemeConf", SchemeConf.getInstance());
                String bootStrapCodes = IOUtils.toString(
                        new FileInputStream(new File(SchemeConf.getInstance().getCallbackScriptPath())),
                        "UTF-8");

                scriptEngine.eval("var global = global || this;", scope);
                // if (scriptEngine instanceof RhinoScriptEngine) {
                // initRhinoEngine(sc);
                // }

                scriptEngine.eval(bootStrapCodes, scope);
                if (undefinedObject == null) {
                    undefinedObject = scriptEngine.eval("(function(){ return undefined;})()");
                }

                invocable = (Invocable) scriptEngine;
            }

            if (undefinedClass == null) {
                try {
                    undefinedClass = Class.forName("org.openjdk.nashorn.internal.runtime.Undefined");
                } catch (ClassNotFoundException _e) {
                    // Ignore exception
                }

                if (undefinedClass == null) {
                    try {
                        undefinedClass = Class.forName("jdk.nashorn.internal.runtime.Undefined");
                    } catch (ClassNotFoundException _e) {
                        // Ignore exception
                    }
                }
            }

        } catch (ScriptException e) {
            e.printStackTrace();
            throw new ConvException(e.getMessage());
        } catch (IOException e) {
            throw new ConvException(e.getMessage());
        }
    }

    public void initNextTable() throws ConvException {
        try {
            // if (scriptEngine instanceof RhinoScriptEngine) {
            // ((RhinoScriptEngine) scriptEngine).invokeFunction("initDataSource");
            // return;
            // }
            if (invocable != null) {
                invocable.invokeFunction("initDataSource");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw (new ConvException(e.getMessage()));
        }
    }

    public static DataETProcessor getInstance() throws ConvException {
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
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "("
                                    + obj.getClass().getName() + ")");
                }
                break;
            case FLOAT:
                if (obj instanceof Number) {
                    val = ((Number) obj).floatValue();
                } else if (obj instanceof String) {
                    val = Float.valueOf((String) obj);
                } else {
                    throw new ConvException(
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "("
                                    + obj.getClass().getName() + ")");
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
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "("
                                    + obj.getClass().getName() + ")");
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
                            fd.getFullName() + " expected " + fd.getType().toString() + ", got " + obj.toString() + "("
                                    + obj.getClass().getName() + ")");
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

    private boolean isUndefinedOrNull(Object obj) {
        if (obj == null) {
            return true;
        }

        if (undefinedObject != null) {
            if (obj.equals(undefinedObject)) {
                return true;
            }

            if (!(undefinedObject instanceof String) && obj.getClass().equals(undefinedObject.getClass())) {
                return true;
            }
        }

        if (undefinedClass != null) {
            if (undefinedClass.isInstance(obj)) {
                return true;
            }
        }

        return false;
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
                if ((isUndefinedOrNull(curValue)))
                    continue;
                if (fd.isMapField()) {
                    for (Map.Entry<?, ?> mapItem : ((SpecialInnerHashMap<?, ?>) curValue).entrySet()) {
                        if (isUndefinedOrNull(mapItem.getKey()) || isUndefinedOrNull(mapItem.getValue())) {
                            continue;
                        }
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
                        if (isUndefinedOrNull(arrItem)) {
                            continue;
                        }
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
                        String.format("FillMap2PbMsg failed\nFullName: %s\n%s\n", fd.getFullName(), e.getMessage()));
            }
        }
    }

    public DynamicMessage.Builder dumpPbMessage(Descriptor currentMsgDesc, DataDstWriterNode node,
            DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(currentMsgDesc);
        HashMap<String, Object> msgMap = new HashMap<>();
        if (dumpMapMessage(msgMap, node, rowContext, fieldPath) == false) {
            return null;
        }
        fillMap2Message(currentMsgDesc, msgMap, builder);
        return builder;
    }

    public boolean dumpMapMessage(HashMap<String, Object> builder, DataDstWriterNode node, DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        boolean dumpSucceed = dumpMessage(builder, node, rowContext, fieldPath);
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
                Object ret;
                // if (scriptEngine instanceof RhinoScriptEngine) {
                // ret = ((RhinoScriptEngine)
                // scriptEngine).invokeFunction("currentMessageCallback", builder,
                // node.getTypeDescriptor());
                // } else {
                ret = invocable.invokeFunction("currentMessageCallback", builder,
                        node.getTypeDescriptor());
                // }
                if (ret instanceof Boolean && ret.equals(false)) {
                    throw new ConvException("Script return " + ret);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ConvException(e.getMessage());
            }
        }
        return dumpSucceed;
    }

}
