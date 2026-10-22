package org.xresloader.core.data.dst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.xresloader.Xresloader;
import org.xresloader.core.ProgramOptions;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstChildrenNode;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstEnumDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstEnumValueDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.err.ConvException;
import org.xresloader.core.data.et.DataETProcessor;
import org.xresloader.core.data.src.DataContainer;
import org.xresloader.core.data.src.DataSrcImpl;
import org.xresloader.core.data.vfy.DataValidatorCache;
import org.xresloader.core.data.vfy.DataValidatorFactory;
import org.xresloader.core.data.vfy.DataVerifyImpl.ValidatorTokens;
import org.xresloader.core.data.vfy.DataVerifyImpl;
import org.xresloader.core.data.vfy.DataVerifyPbEnum;
import org.xresloader.core.data.vfy.DataVerifyPbEnumValue;
import org.xresloader.core.data.vfy.DataVerifyPbMsgField;
import org.xresloader.core.data.vfy.DataVerifyPbMsgOneField;
import org.xresloader.core.data.vfy.DataVerifyPbOneof;
import org.xresloader.core.engine.ExcelEngine.DataItemGridWrapper;
import org.xresloader.core.engine.ExcelEngine;
import org.xresloader.core.engine.IdentifyDescriptor;
import org.xresloader.core.engine.InputFileCache;
import org.xresloader.core.engine.SharedCache;
import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.pb.PbHeaderV3;
import org.xresloader.ue.XresloaderUe;

import com.google.protobuf.AnyProto;
import com.google.protobuf.ApiProto;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Duration;
import com.google.protobuf.DurationProto;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.EmptyProto;
import com.google.protobuf.FieldMaskProto;
import com.google.protobuf.SourceContextProto;
import com.google.protobuf.StructProto;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampProto;
import com.google.protobuf.TypeProto;
import com.google.protobuf.UninitializedMessageException;
import com.google.protobuf.WrappersProto;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;

/**
 * Created by owent on 2014/10/10.
 */
public class DataDstPb extends DataDstImpl {
    private record PbAliasNode<T>(T element, List<String> names) {}

    private static class ParseResult {
        public DynamicMessage value = null;
        public String origin = null;
    }

    private record PbProtoWithFullName<T>(T element, String fullName) {}

    /***
     * protobuf 的描述信息生成是按文件的，所以要缓存并先生成依赖，再生成需要的文件描述数据
     */
    private static class PbInfoSet {
        // =========================== pb 文件描述集 ==========================
        /*** 已加载文件集合，用于文件读取去重(.pb文件) ***/
        public HashSet<String> fileNames = new HashSet<>();
        /*** 描述信息-已加载文件描述集，用于文件描述去重(.proto文件) ***/
        public Map<String, DescriptorProtos.FileDescriptorProto> files = new HashMap<>();
        /*** 描述信息-消息描述集合 ***/
        public Map<String, PbAliasNode<DescriptorProtos.DescriptorProto>> messages = new HashMap<>();
        public Map<String, PbAliasNode<PbProtoWithFullName<DescriptorProtos.FieldDescriptorProto>>> messageFields = new HashMap<>();
        /*** 描述信息-枚举描述集合 ***/
        public Map<String, PbAliasNode<DescriptorProtos.EnumDescriptorProto>> enums = new HashMap<>();
        public Map<String, PbAliasNode<PbProtoWithFullName<DescriptorProtos.EnumValueDescriptorProto>>> enumValues = new HashMap<>();
        /*** 描述信息-oneof描述集合 ***/
        public Map<String, PbAliasNode<DescriptorProtos.OneofDescriptorProto>> oneofs = new HashMap<>();

        // ========================== 配置描述集 ==========================
        /*** 类型信息-文件描述器集合 ***/
        public Map<String, Descriptors.FileDescriptor> fileDescs = new HashMap<>();
        public HashSet<String> fileDescsFailed = new HashSet<>();
        /*** 类型信息-Message描述器集合 ***/
        public Map<String, PbAliasNode<Descriptors.Descriptor>> messageDescs = new HashMap<>();
        private boolean sharedProtocol;
        public HashMap<String, DataDstEnumDescriptor> enumDescs = new HashMap<>();
        /*** 类型信息-快速整数别名集合 ***/
        public HashMap<String, Integer> quickIntegerValuesAlias = new HashMap<>();
        public HashSet<String> quickIntegerValuesContainer = new HashSet<>();

        // ========================== 验证器 ==========================
        DataValidatorCache validatorCache = new DataValidatorCache();

        // ========================== 内建AST类型缓存 ==========================
        HashMap<String, DataDstTypeDescriptor> dataDstDescs = new HashMap<>();

        public PbInfoSet() {
            setupValidatorCreator();
        }

        // Only incremental loads detach. Normal tasks read the shared maps without copying them.
        private void makeProtocolMutable() {
            if (!sharedProtocol) {
                return;
            }
            files = new HashMap<>(files);
            messages = new HashMap<>(messages);
            messageFields = new HashMap<>(messageFields);
            enums = new HashMap<>(enums);
            enumValues = new HashMap<>(enumValues);
            oneofs = new HashMap<>(oneofs);
            fileDescs = new HashMap<>(fileDescs);
            messageDescs = new HashMap<>(messageDescs);
            sharedProtocol = false;
        }

        private void setupValidatorCreator() {
            validatorCache.registerCreator(this.getClass().getTypeName(), (DataVerifyImpl.ValidatorTokens tokens) -> {
                return this.createPbValidator(tokens);
            });
        }

        private DataVerifyImpl createPbValidator(DataVerifyImpl.ValidatorTokens tokens) {
            if (tokens == null) {
                return null;
            }

            if (tokens.isFunctionMode()) {
                return null;
            }

            String name = tokens.getName();

            DescriptorProtos.EnumDescriptorProto enumDesc = getAliasListElement(name, this.enums,
                    "enum type");
            if (enumDesc != null) {
                return new DataVerifyPbEnum(enumDesc);
            }

            PbProtoWithFullName<DescriptorProtos.EnumValueDescriptorProto> enumValueDesc = getAliasListElement(name,
                    this.enumValues,
                    "enum value type");
            if (enumValueDesc != null) {
                return new DataVerifyPbEnumValue(enumValueDesc.element, enumValueDesc.fullName);
            }

            DescriptorProtos.DescriptorProto msgDesc = getAliasListElement(name, this.messages,
                    "message type");
            if (msgDesc != null) {
                return new DataVerifyPbMsgField(msgDesc);
            }

            PbProtoWithFullName<DescriptorProtos.FieldDescriptorProto> messageFieldDesc = getAliasListElement(name,
                    this.messageFields,
                    "message field type");
            if (messageFieldDesc != null) {
                return new DataVerifyPbMsgOneField(messageFieldDesc.element, messageFieldDesc.fullName);
            }

            DescriptorProtos.OneofDescriptorProto oneofDesc = getAliasListElement(name,
                    this.oneofs, "oneof type");
            if (oneofDesc != null) {
                int messageBound = name.lastIndexOf('.');
                if (messageBound > 0 && messageBound < name.length()) {
                    msgDesc = getAliasListElement(name.substring(0, messageBound),
                            this.messages, "message type");
                } else {
                    String oneofFullName = getAliasListElementFullName(name, this.oneofs,
                            "oneof type");
                    messageBound = oneofFullName.lastIndexOf('.');
                    if (messageBound > 0 && messageBound < oneofFullName.length()) {
                        msgDesc = getAliasListElement(oneofFullName.substring(0, messageBound),
                                this.messages, "message type");
                    }
                }

                if (msgDesc != null) {
                    return new DataVerifyPbOneof(oneofDesc, msgDesc);
                }
            }
            return null;
        }
    }

    private static class SetupValidatorResult {
        public boolean success = true;
        public LinkedList<DataVerifyImpl> validator = null;
    }

    private Descriptors.Descriptor currentMsgDesc = null;
    private static com.google.protobuf.ExtensionRegistryLite pbExtensions = null;
    private static final ThreadLocal<PbInfoSet> taskPbs = ThreadLocal.withInitial(PbInfoSet::new);
    private static final SharedCache<InputFileCache.FileKey, DescriptorProtos.FileDescriptorSet> inputPbs =
            new SharedCache<>();

    private record SchemaKey(List<InputFileCache.FileKey> inputs, boolean allowUnknownDependencies) {}

    /** Only protocol input indexes are shared. Bound fields and validators remain in taskPbs. */
    private static final class ProtocolSchema {
        private final Map<String, DescriptorProtos.FileDescriptorProto> files;
        private final Map<String, PbAliasNode<DescriptorProtos.DescriptorProto>> messages;
        private final Map<String, PbAliasNode<PbProtoWithFullName<DescriptorProtos.FieldDescriptorProto>>> messageFields;
        private final Map<String, PbAliasNode<DescriptorProtos.EnumDescriptorProto>> enums;
        private final Map<String, PbAliasNode<PbProtoWithFullName<DescriptorProtos.EnumValueDescriptorProto>>> enumValues;
        private final Map<String, PbAliasNode<DescriptorProtos.OneofDescriptorProto>> oneofs;
        private final Map<String, Descriptors.FileDescriptor> fileDescs;
        private final Map<String, PbAliasNode<Descriptors.Descriptor>> messageDescs;

        ProtocolSchema(PbInfoSet source) {
            files = java.util.Collections.unmodifiableMap(new HashMap<>(source.files));
            messages = java.util.Collections.unmodifiableMap(new HashMap<>(source.messages));
            messageFields = java.util.Collections.unmodifiableMap(new HashMap<>(source.messageFields));
            enums = java.util.Collections.unmodifiableMap(new HashMap<>(source.enums));
            enumValues = java.util.Collections.unmodifiableMap(new HashMap<>(source.enumValues));
            oneofs = java.util.Collections.unmodifiableMap(new HashMap<>(source.oneofs));
            fileDescs = java.util.Collections.unmodifiableMap(new HashMap<>(source.fileDescs));
            messageDescs = java.util.Collections.unmodifiableMap(new HashMap<>(source.messageDescs));
        }

        void install(PbInfoSet target) {
            target.files = files;
            target.messages = messages;
            target.messageFields = messageFields;
            target.enums = enums;
            target.enumValues = enumValues;
            target.oneofs = oneofs;
            target.fileDescs = fileDescs;
            target.messageDescs = messageDescs;
            target.sharedProtocol = true;
        }
    }

    private static final SharedCache<SchemaKey, ProtocolSchema> protocolSchemas = new SharedCache<>();

    private static boolean loadProtocolFiles(boolean allowUnknownDependencies) {
        PbInfoSet target = taskPbs.get();
        String[] paths = ProgramOptions.getInstance().protocolFile;
        if (target.fileNames.containsAll(Arrays.asList(paths))) {
            return true;
        }
        // Nonempty or incomplete task contexts retain the original incremental/error behavior.
        if (!target.fileNames.isEmpty()) {
            for (String path : paths) {
                if (!loadPbFile(target, path, true, allowUnknownDependencies, null)) {
                    return false;
                }
            }
            return true;
        }
        try {
            var inputs = new ArrayList<InputFileCache.FileKey>();
            for (String path : paths) {
                inputs.add(InputFileCache.key(new File(path)));
            }
            boolean[] loaded = {true};
            ProtocolSchema schema = protocolSchemas.getOrLoad(new SchemaKey(List.copyOf(inputs), allowUnknownDependencies), () -> {
                // Only this ordered input set is locked; other schema keys can build concurrently.
                for (String path : paths) {
                    if (!loadPbFile(target, path, true, allowUnknownDependencies, null)) {
                        loaded[0] = false;
                        return null;
                    }
                }
                // Failed or incomplete linking must be retried and diagnosed in every task.
                if (!target.fileDescsFailed.isEmpty() || target.fileDescs.size() != target.files.size()) {
                    return null;
                }
                return new ProtocolSchema(target);
            });
            // The building task keeps its original map traversal order (observable in constant dumps).
            // Cache hits share the published maps directly, without another per-task putAll copy.
            if (schema != null && target.fileNames.isEmpty()) {
                schema.install(target);
                target.fileNames.addAll(Arrays.asList(paths));
            }
            return loaded[0];
        } catch (IOException exception) {
            // Preserve the detailed legacy diagnostic and failure result for unavailable inputs.
            for (String path : paths) {
                if (!loadPbFile(target, path, true, allowUnknownDependencies, null)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void clearThreadState() {
        taskPbs.remove();
        DataDstWriterNode.clearThreadState();
    }
    private static HashMap<String, Descriptors.FileDescriptor> innerFileDescs = null;

    static <T> void appendAliasList(String shortName, String fullName, Map<String, PbAliasNode<T>> hashmap,
            T ele) {
        while (!fullName.isEmpty() && fullName.charAt(0) == '.') {
            fullName = fullName.substring(1);
        }
        if (!shortName.isEmpty() && !shortName.equals(fullName)) {
            PbAliasNode<T> previous = hashmap.get(shortName);
            var names = new ArrayList<String>();
            if (previous != null && previous.names != null) {
                names.addAll(previous.names);
            }
            if (!fullName.isEmpty()) {
                names.add(fullName);
            }
            hashmap.put(shortName, new PbAliasNode<>(ele, List.copyOf(names)));
        }
        if (!fullName.isEmpty()) {
            PbAliasNode<T> previous = hashmap.get(fullName);
            hashmap.put(fullName, new PbAliasNode<>(ele, previous == null ? null : previous.names));
        }
    }

    static <T> T getAliasListElement(String name, Map<String, PbAliasNode<T>> hashmap, String typeName) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }
        PbAliasNode<T> ls = hashmap.getOrDefault(name, null);
        if (null == ls || null == ls.element) {
            return null;
        }

        if (null == ls.names || ls.names.size() <= 1) {
            return ls.element;
        }

        ProgramOptions.getLogger().error(
                "There is more than one %s \"%s\" matched, please use full name. available names:", typeName, name);
        for (String fullName : ls.names) {
            ProgramOptions.getLogger().error("\t%s", fullName);
        }

        return null;
    }

    static <T> String getAliasListElementFullName(String name, Map<String, PbAliasNode<T>> hashmap,
            String typeName) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }
        PbAliasNode<T> ls = hashmap.getOrDefault(name, null);
        if (null == ls || null == ls.element) {
            return name;
        }

        if (null != ls.names && ls.names.size() > 1) {
            ProgramOptions.getLogger().error(
                    "There is more than one %s \"%s\" matched, please use full name. available names:", typeName,
                    name);
            for (String fullName : ls.names) {
                ProgramOptions.getLogger().error("\t%s", fullName);
            }
        }

        if (null != ls.names && ls.names.size() == 1) {
            return ls.names.get(0);
        }

        return name;
    }

    private static synchronized com.google.protobuf.ExtensionRegistryLite getExtensionRegistry() {
        if (null != pbExtensions) {
            return pbExtensions;
        }

        var extensions = com.google.protobuf.ExtensionRegistryLite.newInstance();
        Xresloader.registerAllExtensions(extensions);
        XresloaderUe.registerAllExtensions(extensions);
        pbExtensions = extensions.getUnmodifiable();

        return pbExtensions;
    }

    private static Integer getQuickIntegerValue(PbInfoSet pbs, String name) {
        while (name.length() > 0 && name.charAt(0) == '.') {
            name = name.substring(1);
        }

        int dotPos = name.lastIndexOf('.');
        if (dotPos < 0) {
            return null;
        }

        var ret = pbs.quickIntegerValuesAlias.getOrDefault(name, null);
        if (ret != null) {
            return ret;
        }

        String container = name.substring(0, dotPos);
        if (pbs.quickIntegerValuesContainer.contains(container)) {
            return null;
        }
        pbs.quickIntegerValuesContainer.add(container);

        // Try enum
        var enumDesc = getAliasListElement(container, pbs.enums, "enum type");
        if (enumDesc != null) {
            for (var enumValue : enumDesc.getValueList()) {
                pbs.quickIntegerValuesAlias.put(String.format("%s.%s", container, enumValue.getName()),
                        enumValue.getNumber());
            }
            return pbs.quickIntegerValuesAlias.getOrDefault(name, null);
        }

        // Try message
        var msgDesc = getAliasListElement(container, pbs.messages, "message type");
        if (msgDesc != null) {
            for (var fieldDesc : msgDesc.getFieldList()) {
                pbs.quickIntegerValuesAlias.put(String.format("%s.%s", container, fieldDesc.getName()),
                        fieldDesc.getNumber());
            }
            return pbs.quickIntegerValuesAlias.getOrDefault(name, null);
        }

        return null;
    }

    private static Integer tryParseQuickIntegerValue(PbInfoSet pbs, String name) {
        name = name.trim();
        if (name.isEmpty()) {
            return null;
        }

        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            try {
                return Integer.valueOf(name);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return getQuickIntegerValue(pbs, name);
    }

    static void loadPbMessage(PbInfoSet pbs, DescriptorProtos.DescriptorProto mdp, String packageName,
            Map<String, PbAliasNode<DescriptorProtos.DescriptorProto>> hashmap) {
        String fullName = String.format("%s.%s", packageName, mdp.getName());
        appendAliasList(mdp.getName(), fullName, pbs.messages, mdp);

        // fields
        for (DescriptorProtos.FieldDescriptorProto fdp : mdp.getFieldList()) {
            String fieldFullName = String.format("%s.%s", fullName, fdp.getName());
            appendAliasList(fdp.getName(), fieldFullName, pbs.messageFields,
                    new PbProtoWithFullName<>(fdp, fieldFullName));

        }

        // nest messages
        for (DescriptorProtos.DescriptorProto subMdp : mdp.getNestedTypeList()) {
            loadPbMessage(pbs, subMdp, fullName, hashmap);
        }

        // nest enums
        for (DescriptorProtos.EnumDescriptorProto edp : mdp.getEnumTypeList()) {
            String enumFullName = String.format("%s.%s", fullName, edp.getName());
            appendAliasList(edp.getName(), String.format("%s.%s", fullName, edp.getName()), pbs.enums, edp);

            // enum values
            for (DescriptorProtos.EnumValueDescriptorProto evdp : edp.getValueList()) {
                String valueFullName = String.format("%s.%s", enumFullName, evdp.getName());
                appendAliasList(evdp.getName(), valueFullName, pbs.enumValues,
                        new PbProtoWithFullName<>(evdp, valueFullName));
            }
        }

        // oneof
        for (DescriptorProtos.OneofDescriptorProto oneofDesc : mdp.getOneofDeclList()) {
            appendAliasList(oneofDesc.getName(),
                    String.format("%s.%s.%s", packageName, mdp.getName(), oneofDesc.getName()), pbs.oneofs,
                    oneofDesc);
        }
    }

    static boolean loadPbFile(PbInfoSet pbs, String filePath, boolean buildMsg, boolean allowUnknownDependencies,
            com.google.protobuf.ExtensionRegistryLite exts) {
        if (pbs.fileNames.contains(filePath)) {
            return true;
        }

        try {
            if (exts == null) {
                exts = getExtensionRegistry();
            }

            // 文件描述集读取
            DescriptorProtos.FileDescriptorSet fds;
            if (exts == getExtensionRegistry()) {
                InputFileCache.FileKey key = InputFileCache.key(new File(filePath));
                fds = inputPbs.getOrLoad(key, () -> {
                    try (InputStream input = InputFileCache.open(new File(key.path()))) {
                        return DescriptorProtos.FileDescriptorSet.parseFrom(input, getExtensionRegistry());
                    }
                });
            } else {
                // Custom extension registries are specific to descriptor-print tasks.
                try (InputStream input = InputFileCache.open(new File(filePath))) {
                    fds = DescriptorProtos.FileDescriptorSet.parseFrom(input, exts);
                }
            }
            pbs.makeProtocolMutable();
            pbs.fileNames.add(filePath);
            // 保存文件名和文件描述Proto的关系
            for (DescriptorProtos.FileDescriptorProto fdp : fds.getFileList()) {
                if (pbs.files.containsKey(fdp.getName())) {
                    continue;
                }
                pbs.files.put(fdp.getName(), fdp);

                if (buildMsg) {
                    for (DescriptorProtos.EnumDescriptorProto edp : fdp.getEnumTypeList()) {
                        String enumFullName = String.format("%s.%s", fdp.getPackage(), edp.getName());
                        appendAliasList(edp.getName(), enumFullName,
                                pbs.enums, edp);

                        // enum values
                        for (DescriptorProtos.EnumValueDescriptorProto evdp : edp.getValueList()) {
                            String valueFullName = String.format("%s.%s", enumFullName, evdp.getName());
                            appendAliasList(evdp.getName(), valueFullName, pbs.enumValues,
                                    new PbProtoWithFullName<>(evdp, valueFullName));
                        }
                    }

                    for (DescriptorProtos.DescriptorProto mdp : fdp.getMessageTypeList()) {
                        loadPbMessage(pbs, mdp, fdp.getPackage(), pbs.messages);
                    }
                }
            }

            // 初始化
            if (buildMsg) {
                for (HashMap.Entry<String, DescriptorProtos.FileDescriptorProto> fme : pbs.files.entrySet()) {
                    initPbFiles(pbs, fme.getKey(), allowUnknownDependencies);
                }
            }

        } catch (FileNotFoundException e) {
            ProgramOptions.getLogger().error("Read protocol file \"%s\" failed. %s",
                    String.join(",", ProgramOptions.getInstance().protocolFile), e.getMessage());
            return false;
        } catch (IOException e) {
            ProgramOptions.getLogger().error("Parse protocol file \"%s\" failed. %s",
                    String.join(",", ProgramOptions.getInstance().protocolFile), e.getMessage());
            return false;
        }

        // 载入完成,swap
        return true;
    }

    static Descriptors.Descriptor getMessageProto(PbInfoSet pbs, String protoName) {
        return getAliasListElement(protoName, pbs.messageDescs, "protocol message");
    }

    static Descriptors.EnumValueDescriptor getEnumValue(PbInfoSet pbs, Descriptors.EnumDescriptor enumDesc,
            Integer val) {
        DataDstEnumDescriptor cacheSet = mutableDataDstEnumDescriptor(pbs, enumDesc);
        if (null == cacheSet) {
            return null;
        }

        DataDstEnumValueDescriptor res = cacheSet.getValueById(val);
        if (null == res) {
            return null;
        }
        return (Descriptors.EnumValueDescriptor) res.getRawDescriptor();
    }

    static synchronized Descriptors.FileDescriptor tryGetInnerDetailDesc(String name) {
        if (innerFileDescs != null) {
            return innerFileDescs.getOrDefault(name.replace('\\', '/').toLowerCase(), null);
        }

        Descriptors.FileDescriptor[] innerDescs = new Descriptors.FileDescriptor[] { Xresloader.getDescriptor(),
                XresloaderUe.getDescriptor(), PbHeaderV3.getDescriptor(), DescriptorProtos.getDescriptor(),
                DurationProto.getDescriptor(), TimestampProto.getDescriptor(), AnyProto.getDescriptor(),
                ApiProto.getDescriptor(), EmptyProto.getDescriptor(), FieldMaskProto.getDescriptor(),
                StructProto.getDescriptor(), TypeProto.getDescriptor(), WrappersProto.getDescriptor(),
                SourceContextProto.getDescriptor(), };

        innerFileDescs = new HashMap<>();
        for (Descriptors.FileDescriptor innerFileDesc : innerDescs) {
            innerFileDescs.put(innerFileDesc.getName().replace('\\', '/').toLowerCase(), innerFileDesc);
        }

        return innerFileDescs.getOrDefault(name.toLowerCase(), null);
    }

    static Descriptors.FileDescriptor initPbFiles(PbInfoSet pbs, String name, boolean allowUnknownDependencies) {
        Descriptors.FileDescriptor ret = pbs.fileDescs.getOrDefault(name, null);
        if (null != ret) {
            return ret;
        }

        if (pbs.fileDescsFailed.contains(name)) {
            return null;
        }

        DescriptorProtos.FileDescriptorProto fdp = pbs.files.getOrDefault(name, null);
        if (null == fdp) {
            // Inner proto files
            String standardName = name.replace('\\', '/');
            Descriptors.FileDescriptor innerFileDesc = tryGetInnerDetailDesc(standardName);
            if (null != innerFileDesc) {
                return innerFileDesc;
            }

            if (standardName.equalsIgnoreCase("pb_header.proto")) {
                return PbHeaderV3.getDescriptor();
            }

            if (allowUnknownDependencies) {
                ProgramOptions.getLogger().warn("Protocol file descriptor %s not found.", name);
            } else {
                ProgramOptions.getLogger().error("Protocol file descriptor %s not found.", name);
            }

            pbs.fileDescsFailed.add(name);
            return null;
        }

        ArrayList<Descriptors.FileDescriptor> deps = new ArrayList<>();
        ArrayList<String> failedDeps = new ArrayList<>();
        deps.ensureCapacity(fdp.getDependencyCount());
        failedDeps.ensureCapacity(fdp.getDependencyCount());
        for (int i = 0; i < fdp.getDependencyCount(); ++i) {
            Descriptors.FileDescriptor dep = initPbFiles(pbs, fdp.getDependency(i), allowUnknownDependencies);
            if (null == dep) {
                if (allowUnknownDependencies) {
                    failedDeps.add(fdp.getDependency(i));
                } else {
                    ProgramOptions.getLogger().error("Initialize protocol file descriptor %s failed. dependency %s",
                            name, fdp.getDependency(i));
                    return null;
                }
            } else {
                deps.add(dep);
            }
        }

        if (!failedDeps.isEmpty()) {
            ProgramOptions.getLogger().warn(
                    "Initialize protocol file descriptor %s without dependency %s, maybe missing some descriptor(s).",
                    name, String.join(",", failedDeps));
        }

        try {
            Descriptors.FileDescriptor[] a = new Descriptors.FileDescriptor[deps.size()];
            ret = Descriptors.FileDescriptor.buildFrom(fdp, deps.toArray(a), allowUnknownDependencies);
            pbs.fileDescs.put(name, ret);
            for (Descriptors.Descriptor md : ret.getMessageTypes()) {
                appendAliasList(md.getName(), String.format("%s.%s", fdp.getPackage(), md.getName()),
                        pbs.messageDescs, md);
            }

            return ret;
        } catch (Descriptors.DescriptorValidationException e) {
            ProgramOptions.getLogger().error("Initialize protocol file descriptor %s failed. %s", name, e.getMessage());
            return null;
        }
    }

    private static boolean setupExtension(DataDstTypeDescriptor parentMessage, DataDstFieldDescriptor childField,
            Descriptors.FieldDescriptor fd) {
        String verifierExpr = null;
        if (fd.getOptions().hasExtension(Xresloader.validator)) {
            verifierExpr = fd.getOptions().getExtension(Xresloader.validator);
        }
        // 兼容旧版本
        if (fd.getOptions().hasExtension(Xresloader.verifier)) {
            if (verifierExpr == null) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.verifier);
            } else {
                verifierExpr = String.join("|", verifierExpr, fd.getOptions().getExtension(Xresloader.verifier));
            }
        }
        if (verifierExpr != null && !verifierExpr.isEmpty()) {
            childField.mutableExtension().validator = verifierExpr;
        }
        boolean ret = true;
        var gen = setupValidator(null, verifierExpr, fd);
        if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
            for (DataVerifyImpl vfy : gen.validator) {
                childField.addValidator(vfy);
            }
        } else {
            childField.resetValidator();
        }

        do {
            if (!fd.isMapField()) {
                break;
            }
            if (!fd.getOptions().hasExtension(Xresloader.mapKeyValidator) &&
                    !fd.getOptions().hasExtension(Xresloader.mapValueValidator)) {
                break;
            }

            DataDstTypeDescriptor mapTypeDesc = childField.getTypeDescriptor();
            if (null == mapTypeDesc) {
                break;
            }

            DataDstFieldDescriptor mapKeyField = null;
            DataDstFieldDescriptor mapValueField = null;
            for (DataDstFieldDescriptor mapField : mapTypeDesc.getSortedFields()) {
                if (mapField.getName().equalsIgnoreCase("key")) {
                    mapKeyField = mapField;
                } else if (mapField.getName().equalsIgnoreCase("value")) {
                    mapValueField = mapField;
                }
            }
            if (mapKeyField == null || mapValueField == null) {
                break;
            }

            if (fd.getOptions().hasExtension(Xresloader.mapKeyValidator)) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.mapKeyValidator);
                if (verifierExpr != null && !verifierExpr.isEmpty()) {
                    mapKeyField.mutableExtension().validator = verifierExpr;
                    gen = setupValidator(null, verifierExpr, null);
                    if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
                        for (DataVerifyImpl vfy : gen.validator) {
                            mapKeyField.addValidator(vfy);
                        }
                    } else {
                        mapKeyField.resetValidator();
                    }
                }
            }

            if (fd.getOptions().hasExtension(Xresloader.mapValueValidator)) {
                verifierExpr = fd.getOptions().getExtension(Xresloader.mapValueValidator);
                if (verifierExpr != null && !verifierExpr.isEmpty()) {
                    mapValueField.mutableExtension().validator = verifierExpr;
                    gen = setupValidator(null, verifierExpr, null);
                    if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
                        for (DataVerifyImpl vfy : gen.validator) {
                            mapValueField.addValidator(vfy);
                        }
                    } else {
                        mapValueField.resetValidator();
                    }
                }
            }
        } while (false);

        if (fd.getOptions().hasExtension(Xresloader.fieldDescription)) {
            childField.mutableExtension().description = fd.getOptions().getExtension(Xresloader.fieldDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldRatio)) {
            childField.mutableExtension().ratio = fd.getOptions().getExtension(Xresloader.fieldRatio);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldSeparator)) {
            childField.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.fieldSeparator);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldNotNull)) {
            childField.mutableExtension().notNull = fd.getOptions().getExtension(Xresloader.fieldNotNull);
        }

        if (fd.getOptions().hasExtension(Xresloader.fieldAllowMissingInPlainMode)) {
            childField.mutableExtension().allowMissingInPlainMode = fd.getOptions()
                    .getExtension(Xresloader.fieldAllowMissingInPlainMode);
        }

        if (fd.getOptions().getExtensionCount(Xresloader.fieldUniqueTag) > 0) {
            var ext = childField.mutableExtension();
            ext.uniqueTags = new ArrayList<>();
            ext.uniqueTags.ensureCapacity(fd.getOptions().getExtensionCount(Xresloader.fieldUniqueTag));
            for (String tag : fd.getOptions().getExtension(Xresloader.fieldUniqueTag)) {
                ext.uniqueTags.add(tag);
            }
        }

        if (fd.getOptions().getExtensionCount(Xresloader.fieldTag) > 0) {
            var ext = childField.mutableExtension();
            ext.fieldTags = new HashSet<>();
            for (String tag : fd.getOptions().getExtension(Xresloader.fieldTag)) {
                ext.fieldTags.add(tag);
            }
        }

        // origin refer
        if (fd.getOptions().hasExtension(Xresloader.fieldOriginValue)) {
            String originValue = fd.getOptions().getExtension(Xresloader.fieldOriginValue);
            DataDstFieldDescriptor refer = parentMessage.fields.getOrDefault(originValue, null);
            do {
                if (refer == null) {
                    ProgramOptions.getLogger().error(
                            "field_origin_value \"%s\" of \"%s\" not found, we will ignore this plugin", originValue,
                            fd.getFullName());
                    break;
                }
                if (refer.getType() != DataDstWriterNode.JAVA_TYPE.STRING) {
                    ProgramOptions.getLogger().warn(
                            "field_origin_value \"%s\" of \"%s\" is not a string, we will ignore this plugin",
                            originValue, fd.getFullName());
                    break;
                }

                if (childField.isList() && !refer.isList()) {
                    ProgramOptions.getLogger().warn(
                            "\"%s\" is repeated but field_origin_value \"%s\" is not, we will ignore this plugin",
                            fd.getFullName(), originValue);
                    break;
                }

                if (!childField.isList() && refer.isList()) {
                    ProgramOptions.getLogger().warn(
                            "\"%s\" is not repeated but field_origin_value \"%s\" is a repeated field, we will ignore this plugin",
                            fd.getFullName(), originValue);
                    break;
                }

                childField.setReferOriginField(refer);
            } while (false);
        }

        // setup UE extension
        if (fd.getOptions().hasExtension(XresloaderUe.keyTag)) {
            childField.mutableExtension().mutableUE().keyTag = fd.getOptions().getExtension(XresloaderUe.keyTag);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueTypeName)) {
            childField.mutableExtension().mutableUE().ueTypeName = fd.getOptions()
                    .getExtension(XresloaderUe.ueTypeName);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueTypeIsClass)) {
            childField.mutableExtension().mutableUE().ueTypeIsClass = fd.getOptions()
                    .getExtension(XresloaderUe.ueTypeIsClass);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueOriginTypeName)) {
            childField.mutableExtension().mutableUE().ueOriginTypeName = fd.getOptions()
                    .getExtension(XresloaderUe.ueOriginTypeName);
        }

        if (fd.getOptions().hasExtension(XresloaderUe.ueOriginTypeDefaultValue)) {
            childField.mutableExtension().mutableUE().ueOriginTypeDefaultValue = fd.getOptions()
                    .getExtension(XresloaderUe.ueOriginTypeDefaultValue);
        }

        // setup list extension
        if (fd.isRepeated()) {
            if (fd.getOptions().hasExtension(Xresloader.fieldListStripOption)) {
                switch (fd.getOptions().getExtension(Xresloader.fieldListStripOption).getNumber()) {
                    case Xresloader.ListStripOption.LIST_STRIP_DEFAULT_VALUE -> {
                        childField.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.DEFAULT;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_NOTHING_VALUE -> {
                        childField.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_NOTHING;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_TAIL_VALUE -> {
                        childField.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_TAIL;
                    }
                    case Xresloader.ListStripOption.LIST_STRIP_ALL_VALUE -> {
                        childField.mutableExtension()
                                .mutableList().stripOption = DataDstWriterNode.ListStripRule.STRIP_ALL;
                    }
                }
            }

            if (fd.getOptions().hasExtension(Xresloader.fieldListMinSize)) {
                var intValue = tryParseQuickIntegerValue(taskPbs.get(),
                        fd.getOptions().getExtension(Xresloader.fieldListMinSize));
                if (null == intValue) {
                    ProgramOptions.getLogger().error(
                            "\"%s\" has invalid extension settings field_list_min_size (%s)",
                            fd.getFullName(),
                            fd.getOptions().getExtension(Xresloader.fieldListMinSize));
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                } else {
                    childField.mutableExtension()
                            .mutableList().minSize = intValue;
                }
            }
            if (fd.getOptions().hasExtension(Xresloader.fieldListMaxSize)) {
                var intValue = tryParseQuickIntegerValue(taskPbs.get(),
                        fd.getOptions().getExtension(Xresloader.fieldListMaxSize));
                if (null == intValue) {
                    ProgramOptions.getLogger().error(
                            "\"%s\" has invalid extension settings field_list_max_size (%s)",
                            fd.getFullName(),
                            fd.getOptions().getExtension(Xresloader.fieldListMaxSize));
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                } else {
                    childField.mutableExtension()
                            .mutableList().maxSize = intValue;
                }

                if (childField.mutableExtension()
                        .mutableList().maxSize != 0
                        && childField.mutableExtension()
                                .mutableList().maxSize < childField.mutableExtension()
                                        .mutableList().minSize) {
                    ProgramOptions.getLogger().error(
                            "\"%s\" has invalid extension settings(field_list_max_size (%d) is smaller than field_list_min_size (%d)",
                            fd.getFullName(),
                            childField.mutableExtension()
                                    .mutableList().maxSize,
                            childField.mutableExtension()
                                    .mutableList().minSize);
                    ret = false;
                    if (gen != null) {
                        gen.success = false;
                    }
                }
            }
            if (fd.getOptions().hasExtension(Xresloader.fieldListStrictSize)) {
                childField.mutableExtension()
                        .mutableList().strictSize = fd.getOptions().getExtension(Xresloader.fieldListStrictSize);
            }
        }

        if (gen == null) {
            return ret;
        }
        return gen.success;
    }

    private static boolean setupExtension(@SuppressWarnings("unused") DataDstTypeDescriptor parentMessage,
            DataDstOneofDescriptor childField,
            Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        var gen = setupValidator(null, container, fd);
        if (gen != null && gen.validator != null && !gen.validator.isEmpty()) {
            for (DataVerifyImpl vfy : gen.validator) {
                childField.addValidator(vfy);
            }
        } else {
            childField.resetValidator();
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofDescription)) {
            childField.mutableExtension().description = fd.getOptions().getExtension(Xresloader.oneofDescription);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofSeparator)) {
            childField.mutableExtension().plainSeparator = fd.getOptions().getExtension(Xresloader.oneofSeparator);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofNotNull)) {
            childField.mutableExtension().notNull = fd.getOptions().getExtension(Xresloader.oneofNotNull);
        }

        if (fd.getOptions().hasExtension(Xresloader.oneofAllowMissingInPlainMode)) {
            childField.mutableExtension().allowMissingInPlainMode = fd.getOptions()
                    .getExtension(Xresloader.oneofAllowMissingInPlainMode);
        }

        if (fd.getOptions().getExtensionCount(Xresloader.oneofTag) > 0) {
            var ext = childField.mutableExtension();
            ext.fieldTags = new HashSet<>();
            for (String tag : fd.getOptions().getExtension(Xresloader.oneofTag)) {
                ext.fieldTags.add(tag);
            }
        }

        if (gen == null) {
            return true;
        }
        return gen.success;
    }

    private static SetupValidatorResult setupValidator(LinkedList<DataVerifyImpl> result,
            @SuppressWarnings("unused") Descriptors.Descriptor container,
            Descriptors.OneofDescriptor fd) {
        SetupValidatorResult ret = new SetupValidatorResult();

        if (result == null) {
            result = new LinkedList<>();
        }
        ret.success = true;
        ret.validator = result;

        String rule = fd.getFullName();
        if (rule.length() > 0 && rule.charAt(0) == '.') {
            rule = rule.substring(1);
        }

        DataVerifyImpl vfy = DataValidatorFactory.createSymbolValidator(taskPbs.get().validatorCache, rule);
        if (vfy == null) {
            ProgramOptions.getLogger().error("Setup type validator for oneof descriptor %s failed", rule);
            ret.success = false;
            return ret;
        }

        result.add(vfy);
        return ret;
    }

    private static SetupValidatorResult setupValidator(LinkedList<DataVerifyImpl> result, String verifier,
            @SuppressWarnings("unused") Descriptors.FieldDescriptor fd) {
        if (verifier == null) {
            verifier = "";
        } else {
            verifier = verifier.trim();
        }

        if (verifier.isEmpty()) {
            return null;
        }

        SetupValidatorResult ret = new SetupValidatorResult();
        if (result == null) {
            result = new LinkedList<>();
        }
        ret.success = true;
        ret.validator = result;

        if (!verifier.isEmpty()) {
            List<ValidatorTokens> tokens = DataValidatorFactory.buildValidatorTokens(verifier);
            for (ValidatorTokens token : tokens) {
                DataVerifyImpl vfy = DataValidatorFactory.createValidator(taskPbs.get().validatorCache, token);
                if (vfy == null) {
                    String fullName = "";
                    if (fd != null) {
                        fullName = fd.getFullName();
                    }
                    ProgramOptions.getLogger().error("Setup validator %s for field %s failed", token.toString(),
                            fullName);
                    ret.success = false;
                } else {
                    result.add(vfy);
                }
            }
        }

        return ret;
    }

    private void setupNodeIdentify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            Descriptors.FieldDescriptor fd) throws ConvException {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetValidator();

        if (null != identify.dataSourceFieldValidator && !identify.dataSourceFieldValidator.isEmpty()) {
            SetupValidatorResult gen = setupValidator(null, identify.dataSourceFieldValidator, fd);
            if (gen != null) {
                if (gen.validator != null && !gen.validator.isEmpty()) {
                    for (DataVerifyImpl vfy : gen.validator) {
                        identify.addValidator(vfy);
                    }
                }
                if (gen.success == false) {
                    String fullName;
                    if (fd != null) {
                        fullName = fd.getFullName();
                    } else {
                        fullName = node.getFullName();
                    }
                    this.logErrorMessage("Setup validator for %s failed, field name: %s",
                            identify.name, fullName);
                    throw new ConvException(this.getLastErrorMessage());
                }
            } else {
                identify.resetValidator();
            }
        }

        // merge verifier from field descriptor
        if (child.innerFieldDesc != null && child.innerFieldDesc.hasValidator()) {
            for (DataVerifyImpl vfy : child.innerFieldDesc.getValidator()) {
                identify.addValidator(vfy);
            }
        }
    }

    private void setupNodeIdentify(DataDstWriterNode node, DataDstChildrenNode child, IdentifyDescriptor identify,
            @SuppressWarnings("unused") Descriptors.OneofDescriptor fd) throws ConvException {
        node.identify = identify;

        identify.referToWriterNode = node;
        identify.resetValidator();

        // Data source field verifier is ignored in oneof descriptor

        // merge verifier from oneof descriptor
        if (child.innerOneofDesc != null && child.innerOneofDesc.hasValidator()) {
            for (DataVerifyImpl vfy : child.innerOneofDesc.getValidator()) {
                identify.addValidator(vfy);
            }
        }
    }

    @Override
    public boolean init() {
        if (!loadProtocolFiles(ProgramOptions.getInstance().protocolIgnoreUnknownDependency)) {
            return false;
        }

        currentMsgDesc = getMessageProto(taskPbs.get(), SchemeConf.getInstance().getProtoName());
        if (null == currentMsgDesc) {
            this.logErrorMessage("Can not find protocol message %s", SchemeConf.getInstance().getProtoName());
            return false;
        }

        // Setup custom validators
        String[] customValidatorRules = ProgramOptions.getInstance().customValidatorRules;
        if (customValidatorRules != null) {
            for (String ruleFilePath : customValidatorRules) {
                if (null == DataValidatorFactory.loadFromFile(taskPbs.get().validatorCache, ruleFilePath)) {
                    this.logErrorMessage("Can not load validator rule file %s", ruleFilePath);
                    return false;
                }
            }
        }

        // 初始化数据验证器工厂，允许创建验证器
        DataValidatorFactory.initialize();

        return true;
    }

    /**
     * @return 协议处理器名字
     */
    @Override
    public String name() {
        return "protobuf";
    }

    private static boolean checkFieldIsRequired(Descriptors.FieldDescriptor field) {
        if (field == null) {
            return false;
        }

        if (field.isRequired()) {
            return true;
        }

        if (field.getOptions().hasExtension(Xresloader.fieldRequired)) {
            return field.getOptions().getExtension(Xresloader.fieldRequired);
        }

        return false;
    }

    private static void buildDataDstDescriptorMessage(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstTypeDescriptor innerDesc) throws ConvException {
        if (null == pbDesc || null == innerDesc) {
            return;
        }

        innerDesc.fields = new HashMap<>();
        innerDesc.oneofs = new HashMap<>();

        for (Descriptors.FieldDescriptor field : pbDesc.getFields()) {
            Descriptors.Descriptor fieldPbDesc = null;
            if (field.getJavaType() == JavaType.MESSAGE) {
                fieldPbDesc = field.getMessageType();
            }

            DataDstWriterNode.FIELD_LABEL_TYPE innerLabel = DataDstWriterNode.FIELD_LABEL_TYPE.OPTIONAL;
            if (field.isRepeated()) {
                innerLabel = DataDstWriterNode.FIELD_LABEL_TYPE.LIST;
            } else if (checkFieldIsRequired(field)) {
                innerLabel = DataDstWriterNode.FIELD_LABEL_TYPE.REQUIRED;
            }

            DataDstFieldDescriptor innerField = new DataDstFieldDescriptor(
                    mutableDataDstDescriptor(pbs, fieldPbDesc, pbTypeToInnerType(field.getType()),
                            pbTypeToTypeLimit(field), mutableDataDstEnumDescriptor(pbs, field)),
                    field.getNumber(),
                    field.getName(), innerLabel, field);
            innerDesc.fields.put(field.getName(), innerField);
        }

        for (Descriptors.OneofDescriptor oneof : pbDesc.getOneofs()) {
            HashMap<String, DataDstFieldDescriptor> fields = new HashMap<>();
            for (Descriptors.FieldDescriptor field : oneof.getFields()) {
                DataDstFieldDescriptor findField = innerDesc.fields.getOrDefault(field.getName(), null);
                if (findField != null) {
                    fields.put(field.getName(), findField);
                }
            }
            DataDstOneofDescriptor innerField = new DataDstOneofDescriptor(innerDesc, fields, oneof.getIndex(),
                    oneof.getName(), oneof, pbTypeToTypeLimit(oneof));
            innerDesc.oneofs.put(oneof.getName(), innerField);
        }

        // 最后设置插件
        for (Descriptors.FieldDescriptor field : pbDesc.getFields()) {
            DataDstFieldDescriptor innerField = innerDesc.fields.getOrDefault(field.getName(), null);
            if (null != innerField) {
                if (!setupExtension(innerDesc, innerField, field)) {
                    throw new ConvException(
                            String.format("Setup extension failed, field name: %s", field.getFullName()));
                }
            }
        }

        for (Descriptors.OneofDescriptor oneof : pbDesc.getOneofs()) {
            DataDstOneofDescriptor innerField = innerDesc.oneofs.getOrDefault(oneof.getName(), null);
            if (null != innerField) {
                if (!setupExtension(innerDesc, innerField, pbDesc, oneof)) {
                    throw new ConvException(
                            String.format("Setup extension failed, field name: %s", oneof.getFullName()));
                }
            }
        }
    }

    private static DataDstEnumDescriptor mutableDataDstEnumDescriptor(PbInfoSet pbs,
            Descriptors.EnumDescriptor enumDesc) {
        if (null == enumDesc) {
            return null;
        }

        String name = enumDesc.getFullName();
        while (!name.isEmpty() && '.' == name.charAt(0)) {
            name = name.substring(1);
        }

        DataDstEnumDescriptor cacheSet = pbs.enumDescs.getOrDefault(name, null);
        if (cacheSet == null) {
            cacheSet = new DataDstEnumDescriptor(name, enumDesc.getName(), enumDesc);

            for (Descriptors.EnumValueDescriptor enumVal : enumDesc.getValues()) {
                cacheSet.addValue(new DataDstEnumValueDescriptor(cacheSet, enumVal.getNumber(),
                        enumVal.getName(),
                        enumVal));
            }
            pbs.enumDescs.put(name, cacheSet);
        }

        return cacheSet;
    }

    private static DataDstEnumDescriptor mutableDataDstEnumDescriptor(PbInfoSet pbs,
            Descriptors.FieldDescriptor field) {
        if (field == null) {
            return null;
        }

        if (field.getJavaType() != JavaType.ENUM) {
            return null;
        }

        return mutableDataDstEnumDescriptor(pbs, field.getEnumType());
    }

    private static DataDstTypeDescriptor mutableDataDstDescriptor(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type, DataVerifyImpl typeValidator,
            DataDstEnumDescriptor referEnum) throws ConvException {
        String key;
        if (null == pbDesc) {
            key = type.toString();
        } else {
            key = pbDesc.getFullName();
        }
        if (typeValidator != null) {
            key = String.format("%s:%s", key, typeValidator.getName());
        }
        DataDstTypeDescriptor ret = pbs.dataDstDescs.getOrDefault(key, null);
        if (ret != null) {
            return ret;
        }

        if (pbDesc == null) {
            ret = DataDstWriterNode.getDefaultMessageDescriptor(type, typeValidator, referEnum);
        } else {
            DataDstWriterNode.SPECIAL_MESSAGE_TYPE smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.NONE;
            if (pbDesc.getOptions().getMapEntry()) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.MAP;
            } else if (pbDesc.getFullName().equals(Timestamp.getDescriptor().getFullName())) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT;
            } else if (pbDesc.getFullName().equals(Duration.getDescriptor().getFullName())) {
                smt = DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION;
            }
            ret = new DataDstTypeDescriptor(type, pbDesc.getFile().getPackage(), pbDesc.getName(), pbDesc, smt,
                    typeValidator, referEnum);
        }
        pbs.dataDstDescs.put(key, ret);

        if (null == pbDesc) {
            return ret;
        }

        // extensions
        if (pbDesc.getOptions().hasExtension(Xresloader.msgDescription)) {
            ret.mutableExtension().description = pbDesc.getOptions().getExtension(Xresloader.msgDescription);
        }

        if (pbDesc.getOptions().hasExtension(Xresloader.msgSeparator)) {
            ret.mutableExtension().plainSeparator = pbDesc.getOptions().getExtension(Xresloader.msgSeparator);
        }

        // extensions for UE
        if (pbDesc.getOptions().hasExtension(XresloaderUe.helper)) {
            ret.mutableExtension().mutableUE().helper = pbDesc.getOptions().getExtension(XresloaderUe.helper);
        }

        if (pbDesc.getOptions().hasExtension(XresloaderUe.notDataTable)) {
            ret.mutableExtension().mutableUE().notDataTable = pbDesc.getOptions()
                    .getExtension(XresloaderUe.notDataTable);
        }

        if (pbDesc.getOptions().hasExtension(XresloaderUe.defaultLoader)) {
            XresloaderUe.loader_mode mode = pbDesc.getOptions().getExtension(XresloaderUe.defaultLoader);
            if (mode == XresloaderUe.loader_mode.EN_LOADER_MODE_ENABLE) {
                ret.mutableExtension().mutableUE().enableDefaultLoader = true;
            } else if (mode == XresloaderUe.loader_mode.EN_LOADER_MODE_DISABLE) {
                ret.mutableExtension().mutableUE().enableDefaultLoader = false;
            }
        }

        if (pbDesc.getOptions().getExtensionCount(XresloaderUe.includeHeader) > 0) {
            ret.mutableExtension().mutableUE().includeHeader = pbDesc.getOptions()
                    .getExtension(XresloaderUe.includeHeader);
        }

        buildDataDstDescriptorMessage(pbs, pbDesc, ret);
        return ret;
    }

    private static DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, DataDstWriterNode.JAVA_TYPE type,
            DataVerifyImpl typeValidator, DataDstEnumDescriptor referEnum,
            int listIndex) throws ConvException {
        return DataDstWriterNode.create(null, mutableDataDstDescriptor(pbs, null, type, typeValidator, referEnum),
                listIndex);
    }

    private static DataDstWriterNode createMessageWriterNode(PbInfoSet pbs, Descriptors.Descriptor pbDesc,
            DataDstWriterNode.JAVA_TYPE type, DataVerifyImpl typeValidator, DataDstEnumDescriptor referEnum,
            int listIndex)
            throws ConvException {
        if (null == pbDesc) {
            return createMessageWriterNode(pbs, type, typeValidator, referEnum, listIndex);
        }

        return DataDstWriterNode.create(pbDesc, mutableDataDstDescriptor(pbs, pbDesc, type, typeValidator, null),
                listIndex);
    }

    private static DataDstWriterNode createOneofWriterNode(@SuppressWarnings("unused") PbInfoSet pbs,
            DataDstOneofDescriptor oneofDesc) {
        if (null == oneofDesc) {
            return null;
        }

        // DataDstWriterNode.JAVA_TYPE.ONEOF
        return DataDstWriterNode.create(oneofDesc.getRawDescriptor(), oneofDesc);
    }

    @Override
    public final DataDstWriterNode compile() throws ConvException {
        DataDstWriterNode ret = createMessageWriterNode(taskPbs.get(), currentMsgDesc, DataDstWriterNode.JAVA_TYPE.MESSAGE,
                null, null,
                -1);
        if (test(ret, new LinkedList<>())) {
            return ret;
        }

        throw new ConvException(String.format(
                "Protocol %s compile mapping relationship failed, maybe can not find any message's field in KeyRow",
                name()));
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public final byte[] build(DataDstImpl src) throws ConvException {
        // 初始化header
        PbHeaderV3.xresloader_datablocks.Builder blocks = PbHeaderV3.xresloader_datablocks.newBuilder();
        PbHeaderV3.xresloader_header.Builder header = blocks.getHeaderBuilder();
        header.setXresVer(ProgramOptions.getInstance().getVersion());
        header.setDataVer(ProgramOptions.getInstance().getDataVersion());
        header.setHashCode("");
        ArrayList<String> descriptionList = new ArrayList<>();

        // 校验码
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            this.logErrorMessage("failed to find sha-256 algorithm.");
            header.setHashCode("");
        }

        DataTableContext tableContext = new DataTableContext();
        // 数据
        int count = 0;
        while (DataSrcImpl.getOurInstance().nextTable()) {
            // 生成描述集
            DataDstWriterNode desc = src.compile();
            int previousRowNum = blocks.getDataBlockCount();

            int tolerateContinueEmptyRows = ProgramOptions.getInstance().tolerateContinueEmptyRows;
            int currentContinueEmptyRows = 0;
            while (DataSrcImpl.getOurInstance().nextRow()) {
                DataItemGridWrapper dataItemGrid = DataSrcImpl.getOurInstance().getCurrentDataItemGrid();
                DataRowContext rowContext = new DataRowContext(DataSrcImpl.getOurInstance().getCurrentFileName(),
                        DataSrcImpl.getOurInstance().getCurrentTableName(), dataItemGrid);

                ByteString data = convData(desc, tableContext, rowContext);
                // Empty ByteString is allowed because maybe all fields are default value.
                if (null != data && !rowContext.shouldIgnore()) {
                    ++count;
                    blocks.addDataBlock(data);

                    if (null != sha256) {
                        sha256.update(data.toByteArray());
                    }

                    tableContext.addUniqueCache(rowContext);
                    currentContinueEmptyRows = 0;
                } else {
                    currentContinueEmptyRows++;
                    if (currentContinueEmptyRows > tolerateContinueEmptyRows) {
                        throw new ConvException(String.format(
                                "Too many empty row detected, maybe some cells in file \"%s\" , sheet \"%s\" is set by mistake.Or you can use --tolerate-max-empty-rows to change the bound if it's not a mistake.",
                                DataSrcImpl.getOurInstance().getCurrentFileName(),
                                DataSrcImpl.getOurInstance().getCurrentTableName()));
                    }
                }
            }

            if (desc.getMessageExtension().description != null) {
                descriptionList
                        .add(ProgramOptions.getInstance().getDataSourceMapping(desc.getMessageExtension().description,
                                false));
            }

            blocks.setDataMessageType(desc.getFullName());

            PbHeaderV3.xresloader_data_source.Builder dataSource = header.addDataSourceBuilder();
            dataSource.setFile(ProgramOptions.getInstance()
                    .getDataSourceMapping(DataSrcImpl.getOurInstance().getCurrentFileName(), true));
            dataSource.setSheet(ProgramOptions.getInstance()
                    .getDataSourceMapping(DataSrcImpl.getOurInstance().getCurrentTableName(), false));
            dataSource.setCount(blocks.getDataBlockCount() - previousRowNum);
        }

        String validateResult = tableContext.checkUnique();
        if (validateResult != null && !validateResult.isEmpty()) {
            throw new ConvException(validateResult);
        }

        header.setCount(count);
        if (null != sha256) {
            header.setHashCode("sha256:" + Hex.encodeHexString(sha256.digest()));
        }

        if (!descriptionList.isEmpty()) {
            header.setDescription(String.join(getSystemEndl(), descriptionList));
        }

        // 写出
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        try {
            blocks.build().writeTo(writer);
        } catch (IOException e) {
            e.printStackTrace();
            this.logErrorMessage("try to serialize protobuf data failed. %s", e.getStackTrace(), e.getMessage());
            ProgramOptions.getLogger().error("%s", blocks.getInitializationErrorString());
        }
        return writer.toByteArray();
    }

    static DataDstWriterNode.JAVA_TYPE pbTypeToInnerType(Descriptors.FieldDescriptor.Type t) {
        return switch (t) {
            case DOUBLE -> DataDstWriterNode.JAVA_TYPE.DOUBLE;
            case FLOAT -> DataDstWriterNode.JAVA_TYPE.FLOAT;
            case INT32, FIXED32, UINT32, SFIXED32, SINT32 -> DataDstWriterNode.JAVA_TYPE.INT;
            case INT64, UINT64, FIXED64, SFIXED64, SINT64 -> DataDstWriterNode.JAVA_TYPE.LONG;
            case BOOL -> DataDstWriterNode.JAVA_TYPE.BOOLEAN;
            case STRING -> DataDstWriterNode.JAVA_TYPE.STRING;
            case GROUP, BYTES -> DataDstWriterNode.JAVA_TYPE.BYTES;
            case MESSAGE -> DataDstWriterNode.JAVA_TYPE.MESSAGE;
            case ENUM -> DataDstWriterNode.JAVA_TYPE.INT;
            default -> DataDstWriterNode.JAVA_TYPE.INT;
        };
    }

    private static DataVerifyImpl pbTypeToTypeLimit(Descriptors.FieldDescriptor t) throws ConvException {
        return switch (t.getType()) {
            case INT32, FIXED32, SFIXED32, SINT32 -> DataDstWriterNode.getDefaultTypeValidatorInt32();
            case UINT32 -> DataDstWriterNode.getDefaultTypeValidatorUInt32();
            case UINT64 -> DataDstWriterNode.getDefaultTypeValidatorUInt64();
            case ENUM -> {
                String autoValidatorRule = t.getEnumType().getFullName();
                if (autoValidatorRule.length() > 0 && autoValidatorRule.charAt(0) == '.') {
                    autoValidatorRule = autoValidatorRule.substring(1);
                }

                DataVerifyImpl vfy = DataValidatorFactory.createSymbolValidator(taskPbs.get().validatorCache,
                        autoValidatorRule);
                if (vfy == null) {
                    throw new ConvException(
                            String.format("Setup type validator for oneof descriptor %s failed", autoValidatorRule));
                }
                yield vfy;
            }
            default -> null;
        };
    }

    private static DataVerifyImpl pbTypeToTypeLimit(Descriptors.OneofDescriptor t) throws ConvException {
        String autoValidatorRule = t.getFullName();
        if (autoValidatorRule.length() > 0 && autoValidatorRule.charAt(0) == '.') {
            autoValidatorRule = autoValidatorRule.substring(1);
        }

        DataVerifyImpl vfy = DataValidatorFactory.createSymbolValidator(taskPbs.get().validatorCache, autoValidatorRule);
        if (vfy == null) {
            throw new ConvException(
                    String.format("Setup type validator for oneof descriptor %s failed", autoValidatorRule));
        }
        return vfy;
    }

    private void filterMissingFields(LinkedList<String> missingFields, HashMap<String, String> oneofField,
            Descriptors.FieldDescriptor fd, boolean isMissing) throws ConvException {
        if (missingFields == null && oneofField == null) {
            return;
        }

        Descriptors.OneofDescriptor oneof = fd.getContainingOneof();
        if (isMissing && oneof == null && missingFields != null) {
            missingFields.push(fd.getName());
        }

        if (!isMissing && oneof != null && oneofField.containsKey(oneof.getFullName())) {
            String oldField = oneofField.get(oneof.getFullName());
            if (oldField != null) {
                this.logErrorMessage(
                        "Field \"%s\" in oneof descriptor \"%s\" already exists, can not add another field \"%s\" with the same oneof descriptor",
                        oldField, oneof.getFullName(), fd.getName());
                throw new ConvException(getLastErrorMessage());
            }
            oneofField.replace(oneof.getFullName(), fd.getName());
        }
    }

    /**
     * 测试并生成数据结构
     *
     * @param node      待填充的节点
     * @param nameList 当前命名列表
     * @return 查找到对应的数据源映射关系并非空则返回true，否则返回false
     */
    private boolean test(DataDstWriterNode node, LinkedList<String> nameList) throws ConvException {
        return testMessage(node, nameList);
    }

    private boolean testMessage(DataDstWriterNode node, LinkedList<String> nameList) throws ConvException {
        String prefix = String.join(".", nameList);
        boolean ret = false;
        Descriptors.Descriptor desc = (Descriptors.Descriptor) node.privateData;
        if (null == desc) {
            return ret;
        }

        LinkedList<String> missingFields = null;
        HashMap<String, String> oneofField = new HashMap<>();

        boolean requireMappingAllFields = ProgramOptions.getInstance().requireMappingAllFields
                || (desc.getOptions().hasExtension(Xresloader.msgRequireMappingAll)
                        && desc.getOptions().getExtension(Xresloader.msgRequireMappingAll));
        if (requireMappingAllFields) {
            missingFields = new LinkedList<>();
        }

        for (Descriptors.OneofDescriptor oneof : desc.getOneofs()) {
            oneofField.put(oneof.getFullName(), null);
        }
        boolean enableAliasMapping = ProgramOptions.getInstance().enableAliasMapping;

        DataSrcImpl dataSrc = DataSrcImpl.getOurInstance();
        for (Descriptors.FieldDescriptor fd : desc.getFields()) {
            DataDstChildrenNode child = null;
            ArrayList<String> fieldAlias = null;
            if (enableAliasMapping && fd.getOptions().getExtensionCount(Xresloader.fieldAlias) > 0) {
                fieldAlias = new ArrayList<>();
                fieldAlias.ensureCapacity(fd.getOptions().getExtensionCount(Xresloader.fieldAlias));
                for (String aliasName : fd.getOptions().getExtension(Xresloader.fieldAlias)) {
                    String aliasNameStriped = aliasName.strip();
                    if (!aliasNameStriped.isEmpty()) {
                        fieldAlias.add(aliasNameStriped);
                    }
                }
            }
            switch (fd.getType()) {
                // 复杂类型还需要检测子节点
                case MESSAGE:
                    if (fd.isRepeated()) {
                        int count = 0;
                        nameList.addLast("");
                        for (;; ++count) {
                            DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), fd.getMessageType(),
                                    DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd),
                                    mutableDataDstEnumDescriptor(taskPbs.get(), fd), count);
                            boolean testPassed = false;

                            nameList.removeLast();
                            // 检测使用的名字，message不允许混合别名。以防别名组合指数级膨胀
                            String selectName = "";
                            if (dataSrc.containsIdentifyMappingPrefix(
                                    DataDstWriterNode.makeChildPath(prefix, fd.getName(),
                                            count))) {
                                selectName = fd.getName();
                            } else if (null != fieldAlias) {
                                for (String aliasName : fieldAlias) {
                                    String testFieldName = aliasName.strip();
                                    if (testFieldName.isEmpty()) {
                                        continue;
                                    }

                                    if (dataSrc.containsIdentifyMappingPrefix(
                                            DataDstWriterNode.makeChildPath(prefix, testFieldName,
                                                    count))) {
                                        selectName = testFieldName;
                                        break;
                                    }
                                }
                            }
                            nameList.addLast(DataDstWriterNode.makeNodeName(selectName, count));
                            if (!selectName.isEmpty() && test(c, nameList)) {
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                ret = true;
                                testPassed = true;
                            }

                            if (!testPassed) {
                                // try plain mode - array item
                                String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                                IdentifyDescriptor col = dataSrc.getColumnByName(realName);
                                if (null == col && null != fieldAlias) {
                                    for (String aliasName : fieldAlias) {
                                        String testFieldName = aliasName.strip();
                                        if (testFieldName.isEmpty()) {
                                            continue;
                                        }
                                        String aliasFullName = DataDstWriterNode.makeChildPath(prefix,
                                                testFieldName,
                                                count);
                                        col = dataSrc.getColumnByName(aliasFullName);
                                        if (col != null) {
                                            break;
                                        }
                                    }
                                }

                                if (null != col) {
                                    child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                    setupNodeIdentify(c, child, col, fd);
                                    ret = true;
                                } else {
                                    break;
                                }
                            }
                        }
                        nameList.removeLast();

                        if (count > 0) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            if (child != null && child.innerFieldDesc != null) {
                                int minSize = child.innerFieldDesc.mutableExtension().mutableList().minSize;
                                int maxSize = child.innerFieldDesc.mutableExtension().mutableList().maxSize;
                                if (minSize > 0 && child.nodes.size() < minSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at least %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), minSize, child.nodes.size()));
                                }
                                if (maxSize > 0 && child.nodes.size() > maxSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at most %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), maxSize, child.nodes.size()));
                                }
                            }
                        } else {
                            // try plain mode - the whole array
                            String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = dataSrc.getColumnByName(realName);

                            if (null == col && null != fieldAlias) {
                                for (String aliasName : fieldAlias) {
                                    String testFieldName = aliasName.strip();
                                    if (testFieldName.isEmpty()) {
                                        continue;
                                    }
                                    String aliasFullName = DataDstWriterNode.makeChildPath(prefix,
                                            testFieldName);
                                    col = dataSrc.getColumnByName(aliasFullName);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), fd.getMessageType(),
                                        DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd),
                                        mutableDataDstEnumDescriptor(taskPbs.get(), fd), -1);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setupNodeIdentify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);
                            }
                        }
                    } else {
                        DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), fd.getMessageType(),
                                DataDstWriterNode.JAVA_TYPE.MESSAGE, pbTypeToTypeLimit(fd),
                                mutableDataDstEnumDescriptor(taskPbs.get(), fd), -1);
                        boolean testPassed = false;

                        // 检测使用的名字，message不允许混合别名。以防别名组合指数级膨胀
                        String selectName = "";
                        if (dataSrc.containsIdentifyMappingPrefix(
                                DataDstWriterNode.makeChildPath(prefix, fd.getName()))) {
                            selectName = fd.getName();
                        } else if (null != fieldAlias) {
                            for (String aliasName : fieldAlias) {
                                String testFieldName = aliasName.strip();
                                if (testFieldName.isEmpty()) {
                                    continue;
                                }

                                if (dataSrc.containsIdentifyMappingPrefix(
                                        DataDstWriterNode.makeChildPath(prefix, testFieldName))) {
                                    selectName = testFieldName;
                                    break;
                                }
                            }
                        }
                        nameList.addLast(DataDstWriterNode.makeNodeName(selectName));
                        if (!selectName.isEmpty() && test(c, nameList)) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            ret = true;
                            testPassed = true;
                        }

                        if (!testPassed) {
                            // try plain mode
                            String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = dataSrc.getColumnByName(realName);
                            if (null == col && null != fieldAlias) {
                                for (String aliasName : fieldAlias) {
                                    String testFieldName = aliasName.strip();
                                    if (testFieldName.isEmpty()) {
                                        continue;
                                    }
                                    String aliasFullName = DataDstWriterNode.makeChildPath(prefix, testFieldName);
                                    col = dataSrc.getColumnByName(aliasFullName);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setupNodeIdentify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);

                                if (checkFieldIsRequired(fd)) {
                                    // required 字段要dump默认数据
                                    child = node.addChild(fd.getName(), c, fd,
                                            DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                }
                            }
                        }
                        nameList.removeLast();
                    }
                    break;
                default: {
                    // list 类型
                    DataDstWriterNode.JAVA_TYPE innerType = pbTypeToInnerType(fd.getType());
                    if (fd.isRepeated()) {
                        int count = 0;
                        DataDstEnumDescriptor referEnum = mutableDataDstEnumDescriptor(taskPbs.get(), fd);
                        for (;; ++count) {
                            String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName(), count);
                            IdentifyDescriptor col = dataSrc.getColumnByName(realName);
                            if (null == col && null != fieldAlias) {
                                for (String aliasName : fieldAlias) {
                                    String testFieldName = aliasName.strip();
                                    if (testFieldName.isEmpty()) {
                                        continue;
                                    }
                                    String aliasFullName = DataDstWriterNode.makeChildPath(prefix, testFieldName,
                                            count);
                                    col = dataSrc.getColumnByName(aliasFullName);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), innerType,
                                        pbTypeToTypeLimit(fd), referEnum,
                                        count);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                                setupNodeIdentify(c, child, col, fd);
                                ret = true;
                            } else {
                                break;
                            }
                        }

                        if (count > 0) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            if (child != null && child.innerFieldDesc != null) {
                                int minSize = child.innerFieldDesc.mutableExtension().mutableList().minSize;
                                int maxSize = child.innerFieldDesc.mutableExtension().mutableList().maxSize;
                                if (minSize > 0 && child.nodes.size() < minSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at least %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), minSize, child.nodes.size()));
                                }
                                if (maxSize > 0 && child.nodes.size() > maxSize) {
                                    throw new ConvException(
                                            String.format(
                                                    "\"%s.%s\" for %s.%s require at most %d element(s), real got %d element(s).",
                                                    desc.getFullName(), fd.getName(),
                                                    prefix, fd.getName(), maxSize, child.nodes.size()));
                                }
                            }
                        } else {
                            // try plain mode
                            String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                            IdentifyDescriptor col = dataSrc.getColumnByName(realName);
                            if (null == col && null != fieldAlias) {
                                for (String aliasName : fieldAlias) {
                                    String testFieldName = aliasName.strip();
                                    if (testFieldName.isEmpty()) {
                                        continue;
                                    }
                                    String aliasFullName = DataDstWriterNode.makeChildPath(prefix, testFieldName);
                                    col = dataSrc.getColumnByName(aliasFullName);
                                    if (null != col) {
                                        break;
                                    }
                                }
                            }

                            if (null != col) {
                                DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), innerType,
                                        pbTypeToTypeLimit(fd), referEnum, -1);
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
                                setupNodeIdentify(c, child, col, fd);
                                ret = true;

                                filterMissingFields(missingFields, oneofField, fd, false);
                            } else {
                                filterMissingFields(missingFields, oneofField, fd, true);
                            }
                        }
                    } else {
                        // 非 list 类型
                        String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName());
                        IdentifyDescriptor col = dataSrc.getColumnByName(realName);
                        if (null == col && null != fieldAlias) {
                            for (String aliasName : fieldAlias) {
                                String testFieldName = aliasName.strip();
                                if (testFieldName.isEmpty()) {
                                    continue;
                                }
                                String aliasFullName = DataDstWriterNode.makeChildPath(prefix, testFieldName);
                                col = dataSrc.getColumnByName(aliasFullName);
                                if (null != col) {
                                    break;
                                }
                            }
                        }

                        if (null != col) {
                            filterMissingFields(missingFields, oneofField, fd, false);
                            DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), innerType,
                                    pbTypeToTypeLimit(fd), mutableDataDstEnumDescriptor(taskPbs.get(), fd), -1);
                            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            setupNodeIdentify(c, child, col, fd);
                            ret = true;
                        } else {
                            filterMissingFields(missingFields, oneofField, fd, true);
                            if (checkFieldIsRequired(fd)) {
                                DataDstWriterNode c = createMessageWriterNode(taskPbs.get(), innerType,
                                        pbTypeToTypeLimit(fd), mutableDataDstEnumDescriptor(taskPbs.get(), fd),
                                        -1);
                                // required 字段要dump默认数据
                                child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.STANDARD);
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 索引oneof
        for (Descriptors.OneofDescriptor fd : desc.getOneofs()) {
            if (node.getTypeDescriptor() == null) {
                this.logErrorMessage(
                        "Type descriptor \"%s\" not found, it's probably a BUG, please report to %s, current version: %s",
                        node.getFullName(), ProgramOptions.getReportUrl(), ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstOneofDescriptor oneofInnerDesc = node.getTypeDescriptor().oneofs.getOrDefault(fd.getName(), null);
            if (oneofInnerDesc == null) {
                this.logErrorMessage(
                        "Oneof descriptor \"%s\" not found in type descriptor \"%s\", it's probably a BUG, please report to %s, current version: %s",
                        fd.getFullName(), node.getFullName(), ProgramOptions.getReportUrl(),
                        ProgramOptions.getInstance().getVersion());
                throw new ConvException(getLastErrorMessage());
            }

            DataDstChildrenNode child;
            String realName = DataDstWriterNode.makeChildPath(prefix, fd.getName());
            IdentifyDescriptor col = dataSrc.getColumnByName(realName);

            if (null == col) {
                continue;
            }

            String oldField = oneofField.getOrDefault(fd.getFullName(), null);
            if (oldField != null) {
                this.logErrorMessage(
                        "Field \"%s\" in oneof descriptor \"%s\" already exists, can not add the oneof writer again",
                        oldField, fd.getFullName());
                throw new ConvException(getLastErrorMessage());
            }

            oneofField.replace(fd.getFullName(), fd.getName());

            DataDstWriterNode c = createOneofWriterNode(taskPbs.get(), oneofInnerDesc);
            // oneof field must be a plain field
            child = node.addChild(fd.getName(), c, fd, DataDstWriterNode.CHILD_NODE_TYPE.PLAIN);
            setupNodeIdentify(c, child, col, fd);
            ret = true;
        }

        if (requireMappingAllFields) {
            String missingFieldDesc = "";
            if (missingFields != null && !missingFields.isEmpty()) {
                missingFieldDesc = String.format(" fields %s", String.join(",", missingFields));
            }

            String missingOneofDesc = "";
            ArrayList<String> missingOneofs = new ArrayList<>();
            missingOneofs.ensureCapacity(oneofField.size());
            for (Map.Entry<String, String> oneofEntry : oneofField.entrySet()) {
                if (oneofEntry.getValue() == null) {
                    missingOneofs.add(oneofEntry.getKey());
                }
            }
            if (!missingOneofs.isEmpty()) {
                missingOneofDesc = String.format(" oneof %s", String.join(",", missingOneofs));
            }

            if (!missingFieldDesc.isEmpty() || !missingOneofDesc.isEmpty()) {
                if (prefix.isEmpty()) {
                    this.logErrorMessage("Message %s can not find%s%s in data source", desc.getFullName(),
                            missingFieldDesc, missingOneofDesc);
                } else {
                    this.logErrorMessage("Message %s in %s can not find%s%s in data source", desc.getFullName(), prefix,
                            missingFieldDesc, missingOneofDesc);
                }
                throw new ConvException(getLastErrorMessage());
            }
        }

        return ret;
    }

    private ByteString convData(DataDstWriterNode node, @SuppressWarnings("unused") DataTableContext tableContext,
            DataRowContext rowContext)
            throws ConvException {
        // Descriptors.Descriptor msg_desc = (Descriptors.Descriptor) node.privateData;
        DynamicMessage.Builder root;
        boolean validData;
        if (SchemeConf.getInstance().getCallbackScriptPath().isEmpty()) {
            root = DynamicMessage.newBuilder(currentMsgDesc);
            validData = dumpMessage(root, node, rowContext, currentMsgDesc.getName());
        } else {
            root = DataETProcessor.getInstance().dumpPbMessage(currentMsgDesc, node, rowContext,
                    currentMsgDesc.getName());
            validData = (root != null);
        }
        // 过滤空项
        if (!validData || root == null) {
            return null;
        }

        if (rowContext.shouldIgnore()) {
            ProgramOptions.getLogger().warn(
                    "File: %s, Sheet: %s, %s: %d%s",
                    rowContext.fileName, rowContext.tableName, rowContext.getDataItemIndexName(),
                    rowContext.dataItemGrid.getDataItemIndex() + 1,
                    rowContext.buildIgnoreIgnoreMessage(4));

            return null;
        }

        try {
            return root.build().toByteString();
        } catch (Exception e) {
            this.logErrorMessage("Serialize failed. %s", e.getMessage());
            return null;
        }
    }

    private Object getDefault(@SuppressWarnings("unused") DynamicMessage.Builder builder, DataDstFieldDescriptor field,
            int listIndex)
            throws ConvException {
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        Object val = null;
        if (field.isList() && field.getListExtension() != null
                && field.getListExtension().strictSize && field.getListExtension().minSize >= listIndex + 1) {
            this.logErrorMessage(
                    "Field \"%s\" in \"%s\" has set field_list_min_size %d, which is not allowed to be auto filled with default value.",
                    field.getName(), fd.getContainingType().getFullName(),
                    field.getListExtension().minSize);
            throw new ConvException(getLastErrorMessage());
        }
        switch (fd.getType()) {
            case DOUBLE -> val = 0.0;
            case FLOAT -> val = 0.0f;
            case INT32, FIXED32, UINT32, SFIXED32, SINT32 -> val = 0;
            case INT64, UINT64, FIXED64, SFIXED64, SINT64 -> val = Long.valueOf(0);
            case ENUM -> val = fd.getEnumType().findValueByNumber(0);
            case BOOL -> val = false;
            case STRING -> val = "";
            case GROUP -> val = new byte[0];
            case MESSAGE -> {
                if (null != field.getTypeDescriptor()) {
                    DynamicMessage.Builder subnode = DynamicMessage.newBuilder(fd.getMessageType());

                    // 仅仅Required需要导出默认值
                    for (DataDstFieldDescriptor subField : field.getTypeDescriptor().getSortedFields()) {
                        if (subField.isRequired()) {
                            dumpDefault(subnode, subField, 0);
                        } else if (subField.isList() && subField.getListExtension() != null
                                && subField.getListExtension().strictSize && subField.getListExtension().minSize > 0
                                && !subField.containsFieldTags(ProgramOptions.getInstance().ignoreFieldTags)) {
                            this.logErrorMessage(
                                    "Field \"%s\" in \"%s\" has set field_list_min_size %d, which is not allowed to be auto filled with default value.",
                                    subField.getName(), fd.getFullName(),
                                    subField.getListExtension().minSize);
                            throw new ConvException(getLastErrorMessage());
                        }
                    }

                    val = subnode.build();
                }
            }
            case BYTES -> val = new byte[0];
        }
        return val;
    }

    private Object getValueFromDataSource(DataDstWriterNode desc, DataDstFieldDescriptor field,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        Object val = null;
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();

        switch (fd.getJavaType()) {
            case INT -> {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = ret.value.intValue();
                }
            }

            case LONG -> {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
            }

            case FLOAT -> {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    val = ret.value.floatValue();
                }
            }

            case DOUBLE -> {
                DataContainer<Double> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0.0);
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
            }

            case BOOLEAN -> {
                DataContainer<Boolean> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, false);
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
            }

            case STRING -> {
                DataContainer<String> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != ret && ret.valid) {
                    val = ret.value;
                }
            }

            case BYTE_STRING -> {
                DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(desc.identify, "");
                if (null != res && res.valid) {
                    String encoding = SchemeConf.getInstance().getKey().getEncoding();
                    if (null == encoding || encoding.isEmpty()) {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes());
                    } else {
                        val = com.google.protobuf.ByteString.copyFrom(res.value.getBytes(Charset.forName(encoding)));
                    }
                }
            }
            case ENUM -> {
                DataContainer<Long> ret = DataSrcImpl.getOurInstance().getValue(desc.identify, 0L);
                if (null != ret && ret.valid) {
                    val = fd.getEnumType().findValueByNumber(ret.value.intValue());
                }

            }

            case MESSAGE -> {
                DynamicMessage.Builder node = DynamicMessage.newBuilder(fd.getMessageType());
                if (dumpMessage(node, desc, rowContext, fieldPath) || checkFieldIsRequired(fd)) {
                    try {
                        val = node.build();
                    } catch (UninitializedMessageException e) {
                        this.logErrorMessage("serialize %s(%s) failed. %s", fd.getFullName(),
                                fd.getMessageType().getName(), e.getMessage());
                    }
                }
            }

            default -> {
            }
        }
        return val;
    }

    private void dumpValue(DynamicMessage.Builder builder, DataDstFieldDescriptor field, Object val, int index,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        DataDstWriterNode.ListStripRule stripListRule = field.getListStripRule();
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();

        if (fd.isRepeated()
                && (stripListRule == DataDstWriterNode.ListStripRule.STRIP_NOTHING
                        || stripListRule == DataDstWriterNode.ListStripRule.STRIP_TAIL)) {
            int fillDefaultSize = builder.getRepeatedFieldCount(fd);
            for (int i = fillDefaultSize; i < index; ++i) {
                Object defaultVal = getDefault(builder, field, i);
                if (defaultVal != null) {
                    builder.addRepeatedField(fd, defaultVal);
                }
            }
        }

        if (JavaType.ENUM == fd.getJavaType()) {
            Descriptors.EnumValueDescriptor enumVal;
            if (val instanceof Descriptors.EnumValueDescriptor) {
                enumVal = (Descriptors.EnumValueDescriptor) val;
            } else {
                enumVal = getEnumValue(taskPbs.get(), fd.getEnumType(), (Integer) val);
            }

            if (null == enumVal) {
                return;
            }

            if (fd.isRepeated()) {
                if (index > 0 && builder.getRepeatedFieldCount(fd) > index) {
                    builder.setRepeatedField(fd, index, enumVal);
                } else {
                    builder.addRepeatedField(fd, enumVal);
                }

                if (null != rowContext && null != field.getUniqueTags()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, String.format("%s.%s", fieldPath, index),
                                enumVal.getNumber());
                    }
                }
            } else {
                builder.setField(fd, enumVal);

                if (null != rowContext && null != field.getUniqueTags()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath,
                                enumVal.getNumber());
                    }
                }
            }
        } else {
            if (fd.isRepeated()) {
                if (index > 0 && builder.getRepeatedFieldCount(fd) > index) {
                    builder.setRepeatedField(fd, index, val);
                } else {
                    builder.addRepeatedField(fd, val);
                }

                if (null != rowContext && null != field.getUniqueTags() && JavaType.MESSAGE != fd.getJavaType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, String.format("%s.%s", fieldPath, index),
                                val);
                    }
                }
            } else {
                builder.setField(fd, val);

                if (null != rowContext && null != field.getUniqueTags() && JavaType.MESSAGE != fd.getJavaType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, val);
                    }
                }
            }
        }
    }

    private void dumpDefault(DynamicMessage.Builder builder, DataDstOneofDescriptor oneof) {
        builder.clearOneof((Descriptors.OneofDescriptor) oneof.getRawDescriptor());
    }

    private void dumpDefault(DynamicMessage.Builder builder, DataDstFieldDescriptor field, int index)
            throws ConvException {
        if (field.isList() && index < 0) {
            return;
        }

        Object val = getDefault(builder, field, index);
        if (val != null) {
            dumpValue(builder, field, val, index, null, "");
        }
    }

    /**
     * 转储数据到builder
     *
     * @param builder 转储目标
     * @param node    message的描述结构
     * @return 有数据则返回true
     * @throws ConvException
     */
    private boolean dumpMessage(DynamicMessage.Builder builder, DataDstWriterNode node, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        boolean ret = false;

        for (Map.Entry<String, DataDstWriterNode.DataDstChildrenNode> c : node.getChildren().entrySet()) {
            boolean ignoreFieldTags = c.getValue().containsFieldTags(ProgramOptions.getInstance().ignoreFieldTags);

            if (c.getValue().isOneof()) {
                // dump oneof data
                boolean fieldHasValue = false;
                String subFieldPath = String.format("%s.%s", fieldPath, c.getKey());
                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getOneofDescriptor(), child, rowContext,
                            fieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }
                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("oneof %s is empty but set not null, we will ignore this row",
                                    subFieldPath));
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.STANDARD) {
                boolean fieldHasValue = false;

                if (null == c.getValue().innerFieldDesc) {
                    // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
                    continue;
                }

                for (int i = 0; i < c.getValue().nodes.size(); i++) {
                    DataDstWriterNode child = c.getValue().nodes.get(i);
                    String subFieldPath;
                    if (c.getValue().isList()) {
                        subFieldPath = String.format("%s.%s.%d", fieldPath, c.getKey(), i);
                    } else {
                        subFieldPath = String.format("%s.%s", fieldPath, c.getKey());
                    }

                    if (dumpStandardField(builder, child, c.getValue().innerFieldDesc, rowContext, subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }

                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("field %s.%s is empty but set not null, we will ignore this row",
                                    fieldPath, c.getKey()));
                }
            } else if (c.getValue().mode == DataDstWriterNode.CHILD_NODE_TYPE.PLAIN) {
                boolean fieldHasValue = false;
                String subFieldPath = String.format("%s.%s", fieldPath, c.getKey());

                for (DataDstWriterNode child : c.getValue().nodes) {
                    if (dumpPlainField(builder, child.identify, child.getFieldDescriptor(), child, rowContext,
                            subFieldPath)) {
                        ret = true;
                        fieldHasValue = true;
                    }
                }

                if (null != rowContext && !fieldHasValue && c.getValue().isNotNull()) {
                    rowContext.addIgnoreReason(
                            String.format("Field %s is empty but set not null, we will ignore this row",
                                    subFieldPath));
                }
            }

            // Check list size
            if (c.getValue().isList() && c.getValue().innerFieldDesc != null) {
                var listExt = c.getValue().innerFieldDesc.getListExtension();
                if (listExt != null && c.getValue().innerFieldDesc != null) {
                    Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) c.getValue().innerFieldDesc
                            .getRawDescriptor();
                    if (listExt.minSize > 0 && listExt.minSize > builder.getRepeatedFieldCount(fd)
                            && !ignoreFieldTags) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at least %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.minSize, builder.getRepeatedFieldCount(fd)));
                    }
                    if (listExt.maxSize > 0 && listExt.maxSize < builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at most %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.maxSize, builder.getRepeatedFieldCount(fd)));
                    }
                }
            }
        }

        return ret;
    }

    private boolean dumpStandardField(DynamicMessage.Builder builder, DataDstWriterNode desc,
            DataDstFieldDescriptor field, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (null == desc.identify && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
            // required 空字段填充默认值
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, desc.getListIndex());
            }

            return false;
        }

        Object val = getValueFromDataSource(desc, field, rowContext, fieldPath);

        if (null == val) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, desc.getListIndex());
            }

            return false;
        } else if (field.containsFieldTags(ProgramOptions.getInstance().ignoreFieldTags)) {
            if (field.isRequired() || field.isList()) {
                dumpDefault(builder, field, -1);
            }
            return true;
        }

        dumpValue(builder, field, val, desc.getListIndex(), rowContext, fieldPath);
        return true;
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (field == null) {
            return false;
        }

        if (null == ident) {
            if (DataDstWriterNode.getDefaultListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstOneofDescriptor field, @SuppressWarnings("unused") DataDstWriterNode maybeFromNode,
            String input,
            DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        if (field == null) {
            return false;
        }

        boolean ret = false;
        DataDstWriterNode.DataDstFieldDescriptor subField = null;
        do {
            Object[] res = parsePlainDataOneof(input, ident, field);
            if (null == res) {
                break;
            }

            if (res.length < 1) {
                return false;
            }

            subField = (DataDstWriterNode.DataDstFieldDescriptor) res[0];
            if (subField == null) {
                break;
            }

            if (res.length == 1) {
                dumpDefault(builder, subField, 0);
                ret = true;
                break;
            }

            // 非顶层，不用验证类型
            ret = dumpPlainField(builder, null, subField, null, (String) res[1], rowContext, fieldPath);
        } while (false);

        for (var siblingField : field.getSortedFields()) {
            if (siblingField.isNotNull() && siblingField != subField) {
                rowContext.addIgnoreReason(
                        String.format("field %s.%s is empty but set not null, we will ignore this row",
                                fieldPath, siblingField.getName()));
                break;
            }
        }

        return ret;
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode,
            DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (null == ident) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, 0);
            }
            return false;
        }

        DataContainer<String> res = DataSrcImpl.getOurInstance().getValue(ident, "");
        if (null == res || !res.valid) {
            if (field.isRequired()
                    || field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                dumpDefault(builder, field, 0);
            }
            return false;
        }

        return dumpPlainField(builder, ident, field, maybeFromNode, res.value, rowContext, fieldPath);
    }

    private boolean dumpPlainField(DynamicMessage.Builder builder, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataDstWriterNode maybeFromNode, String input,
            DataRowContext rowContext,
            String fieldPath)
            throws ConvException {
        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return false;
        }

        if ((null != maybeFromNode && null != maybeFromNode.identify && !field.isList())
                && field.getType() != DataDstWriterNode.JAVA_TYPE.MESSAGE) {
            // error type
            logErrorMessage("Plain type %s of %s.%s must be list", field.getType().toString(),
                    field.getTypeDescriptor().getFullName(), field.getName());
            return false;
        }

        boolean ignoreFieldTags = field.containsFieldTags(ProgramOptions.getInstance().ignoreFieldTags);
        if (field.isList()) {
            String[] groups;
            if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                groups = new String[] { input.trim() };
            } else {
                groups = splitPlainGroups(input.trim(), getPlainFieldSeparator(field));
            }
            Object parsedDatas = null;

            boolean ret = true;
            switch (field.getType()) {
                case INT -> {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        for (Long v : values) {
                            if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                                Descriptors.EnumValueDescriptor enumVal = getEnumValue(taskPbs.get(), fd.getEnumType(),
                                        v.intValue());
                                if (enumVal != null) {
                                    tmp.add(enumVal);
                                }
                            } else {
                                tmp.add(v.intValue());
                            }
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                case LONG -> {
                    Long[] values = parsePlainDataLong(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        tmp.addAll(Arrays.asList(values));
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                case FLOAT -> {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        for (Double v : values) {
                            tmp.add(v.floatValue());
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                case DOUBLE -> {
                    Double[] values = parsePlainDataDouble(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        tmp.addAll(Arrays.asList(values));
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                case BOOLEAN -> {
                    Boolean[] values = parsePlainDataBoolean(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        tmp.addAll(Arrays.asList(values));
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                case STRING, BYTES -> {
                    String[] values = parsePlainDataString(groups, ident, field);
                    ArrayList<Object> tmp = new ArrayList<>();
                    if (values != null) {
                        tmp.ensureCapacity(values.length);
                        tmp.addAll(Arrays.asList(values));
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }
                case MESSAGE -> {
                    ArrayList<DynamicMessage> tmp = new ArrayList<>();
                    tmp.ensureCapacity(groups.length);
                    DataDstFieldDescriptor referOriginField = null;
                    Descriptors.FieldDescriptor referOriginFd = null;
                    if (null != field.getReferOriginField()) {
                        referOriginField = field
                                .getReferOriginField();
                        referOriginFd = (Descriptors.FieldDescriptor) referOriginField.getRawDescriptor();
                    }
                    for (int i = 0; i < groups.length; ++i) {
                        String v = groups[i];
                        String[] subGroups = splitPlainGroups(v, getPlainMessageSeparator(field));
                        String subFieldPath = String.format("%s.%d", fieldPath, i);
                        ParseResult res = parsePlainDataMessage(subGroups, ident, field, rowContext,
                                subFieldPath);
                        if (res != null && res.value != null) {
                            tmp.add(res.value);
                            if (res.origin != null && referOriginField != null && !ignoreFieldTags) {
                                while (builder.getRepeatedFieldCount(referOriginFd) < i) {
                                    builder.addRepeatedField(referOriginFd, "");
                                }
                                dumpValue(builder, referOriginField, res.origin, i, rowContext, subFieldPath);
                            }
                        }
                    }
                    if (!tmp.isEmpty()) {
                        parsedDatas = tmp;
                    }
                }

                default -> {
                }
            }
            // oneof can not be repeated

            if (parsedDatas != null) {
                if (!(parsedDatas instanceof ArrayList<?>)) {
                    return false;
                }
                ArrayList<?> values = (ArrayList<?>) parsedDatas;

                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0) {
                    if (values.size() != 1) {
                        throw new ConvException(
                                String.format("Try to convert %s.%s.%d failed, too many element(s)(found %d).",
                                        field.getTypeDescriptor().getFullName(), field.getName(),
                                        maybeFromNode.getListIndex(), values.size()));
                    }

                    int index = maybeFromNode.getListIndex();
                    DataDstWriterNode.ListStripRule stripListRule = field.getListStripRule();
                    if (stripListRule == DataDstWriterNode.ListStripRule.STRIP_NOTHING
                            || stripListRule == DataDstWriterNode.ListStripRule.STRIP_TAIL) {
                        while (builder.getRepeatedFieldCount(fd) < index && !ignoreFieldTags) {
                            builder.addRepeatedField(fd, getDefault(builder, field, builder.getRepeatedFieldCount(fd)));
                        }
                    }

                    if (!ignoreFieldTags) {
                        if (index >= 0 && builder.getRepeatedFieldCount(fd) > index) {
                            builder.setRepeatedField(fd, index, values.get(0));
                        } else {
                            builder.addRepeatedField(fd, values.get(0));
                        }
                    }
                } else {
                    if (!ignoreFieldTags) {
                        for (int i = 0; i < values.size(); ++i) {
                            dumpValue(builder, field, values.get(i), i, rowContext,
                                    String.format("%s.%d", fieldPath, i));
                        }
                    }
                }

                if (null != rowContext && null != field.getUniqueTags()
                        && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType() && !values.isEmpty()) {
                    // Convert EnumValueDescriptor into Integer
                    if (values.get(0) instanceof Descriptors.EnumValueDescriptor) {
                        ArrayList<Integer> enumValues = new ArrayList<>();
                        enumValues.ensureCapacity(values.size());
                        for (Object valObj : values) {
                            enumValues.add(((Descriptors.EnumValueDescriptor) valObj).getNumber());
                        }

                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, fieldPath, enumValues);
                        }
                    } else {
                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, fieldPath, values);
                        }
                    }
                }
            } else if (field.getListStripRule() == DataDstWriterNode.ListStripRule.STRIP_NOTHING) {
                if (null != maybeFromNode && maybeFromNode.getListIndex() >= 0 && !ignoreFieldTags) {
                    int index = maybeFromNode.getListIndex();
                    while (builder.getRepeatedFieldCount(fd) <= index) {
                        builder.addRepeatedField(fd, getDefault(builder, field, builder.getRepeatedFieldCount(fd)));
                    }
                }
            } else {
                ret = false;
            }

            // Check list size
            {
                var listExt = field.getListExtension();
                if (listExt != null) {
                    if (listExt.minSize > 0 && listExt.minSize > builder.getRepeatedFieldCount(fd)
                            && !ignoreFieldTags) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at least %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.minSize, builder.getRepeatedFieldCount(fd)));
                    }
                    if (listExt.maxSize > 0 && listExt.maxSize < builder.getRepeatedFieldCount(fd)) {
                        throw new ConvException(
                                String.format(
                                        "Try to convert %s failed, require at most %d element(s), real got %d element(s).",
                                        fd.getFullName(),
                                        listExt.maxSize, builder.getRepeatedFieldCount(fd)));
                    }
                }
            }

            return ret;
        } else {
            Object val = null;

            switch (field.getType()) {
                case INT -> {
                    Long res = parsePlainDataLong(input.trim(), ident, field);
                    val = res.intValue();
                }

                case LONG -> {
                    val = parsePlainDataLong(input.trim(), ident, field);
                }

                case FLOAT -> {
                    Double res = parsePlainDataDouble(input.trim(), ident, field);
                    val = res.floatValue();
                }

                case DOUBLE -> {
                    val = parsePlainDataDouble(input.trim(), ident, field);
                }

                case BOOLEAN -> {
                    val = parsePlainDataBoolean(input.trim(), ident, field);
                }

                case STRING, BYTES -> {
                    val = parsePlainDataString(input.trim(), ident, field);
                }
                case MESSAGE -> {
                    String[] groups = splitPlainGroups(input.trim(), getPlainMessageSeparator(field));
                    ParseResult res = parsePlainDataMessage(groups, ident, field, rowContext, fieldPath);
                    if (res != null && res.value != null) {
                        val = res.value;
                        if (res.origin != null && field.getReferOriginField() != null && !ignoreFieldTags) {
                            dumpValue(builder,
                                    field.getReferOriginField(),
                                    res.origin, 0, rowContext, fieldPath);
                        }
                    } else if (field.isRequired()) {
                        dumpDefault(builder, field, 0);
                    }
                }

                default -> {
                }
            }

            if (val == null) {
                return false;
            }

            if (ignoreFieldTags) {
                if (field.isRequired() || field.isList()) {
                    dumpDefault(builder, field, -1);
                }
                return true;
            }

            if (fd.isRepeated() && val instanceof ArrayList<?>) {
                ArrayList<?> values = (ArrayList<?>) val;
                for (int i = 0; i < values.size(); ++i) {
                    String subFieldPath = String.format("%s.%d", fieldPath, i);

                    dumpValue(builder, field, values.get(i), i, rowContext, subFieldPath);
                    if (null != rowContext && null != field.getUniqueTags()
                            && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                        for (var tagKey : field.getUniqueTags()) {
                            rowContext.addUniqueCache(tagKey, subFieldPath, values.get(i));
                        }
                    }
                }
            } else {
                dumpValue(builder, field, val, 0, rowContext, fieldPath);

                if (null != rowContext && null != field.getUniqueTags()
                        && DataDstWriterNode.JAVA_TYPE.MESSAGE != field.getType()) {
                    for (var tagKey : field.getUniqueTags()) {
                        rowContext.addUniqueCache(tagKey, fieldPath, val);
                    }
                }
            }

            return true;
        }
    }

    private ParseResult parsePlainDataMessage(String[] inputs, IdentifyDescriptor ident,
            DataDstWriterNode.DataDstFieldDescriptor field, DataRowContext rowContext,
            String fieldPath) throws ConvException {
        if (field.getTypeDescriptor() == null || inputs == null || inputs.length == 0) {
            return null;
        }

        Descriptors.FieldDescriptor fd = (Descriptors.FieldDescriptor) field.getRawDescriptor();
        if (null == fd || fd.getJavaType() != MESSAGE) {
            // 不需要提示，如果从其他方式解包协议描述的时候可能有可选字段丢失的
            return null;
        }

        ArrayList<DataDstWriterNode.DataDstFieldDescriptor> children = field.getTypeDescriptor().getSortedFields();
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(fd.getMessageType());
        ParseResult ret = new ParseResult();

        // 几种特殊模式
        if (inputs.length == 1) {
            if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.TIMEPOINT == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDatetime(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    Descriptors.FieldDescriptor subfd = (Descriptors.FieldDescriptor) children.get(i)
                            .getRawDescriptor();
                    if (subfd.getName().equalsIgnoreCase("seconds")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.toEpochMilli() / 1000);
                    } else if (subfd.getName().equalsIgnoreCase("nanos")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.getNano());
                    }
                }

                ret.value = builder.build();
                ret.origin = inputs[0];
                return ret;
            } else if (org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE.DURATION == field
                    .getTypeDescriptor().getSpecialMessageType()) {
                Instant res = parsePlainDataDuration(inputs[0]);
                for (int i = 0; i < children.size(); ++i) {
                    Descriptors.FieldDescriptor subfd = (Descriptors.FieldDescriptor) children.get(i)
                            .getRawDescriptor();
                    if (subfd.getName().equalsIgnoreCase("seconds")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.toEpochMilli() / 1000);
                    } else if (subfd.getName().equalsIgnoreCase("nanos")
                            && !subfd.isRepeated()) {
                        builder.setField(subfd, res.getNano());
                    }
                }
                ret.value = builder.build();
                ret.origin = inputs[0];
                return ret;
            }
        }

        boolean hasData = false;
        HashSet<String> dumpedOneof = null;
        if (!field.getTypeDescriptor().getSortedOneofs().isEmpty()) {
            dumpedOneof = new HashSet<>();
        }

        int usedInputIdx = 0;
        int fieldSize = 0;
        int atLeastFieldSize = field.getTypeDescriptor().getSortedOneofs().size();
        HashMap<Integer, DataDstFieldDescriptor> missingFields = new HashMap<>();
        for (int i = 0; i < children.size(); ++i) {
            DataDstFieldDescriptor child = children.get(i);
            if (child.allowMissingInPlainMode()) {
                continue;
            }

            if (child.getReferOneof() != null) {
                if (child.getReferOneof().allowMissingInPlainMode()) {
                    continue;
                }
                atLeastFieldSize += 1;
            }

            missingFields.put(child.getIndex(), child);
        }

        for (int i = 0; i < children.size(); ++i) {
            DataDstFieldDescriptor child = children.get(i);

            if (child.getLinkedValueField() != null) {
                ++fieldSize;
                continue;
            }

            if (child.getReferOneof() != null) {
                if (dumpedOneof == null) {
                    throw new ConvException(String.format(
                            "Try to convert field %s of %s failed, found oneof descriptor but oneof set is not initialized.",
                            child.getName(), field.getTypeDescriptor().getFullName()));
                }
                if (dumpedOneof.contains(child.getReferOneof().getFullName())) {
                    continue;
                }

                if (usedInputIdx < inputs.length
                        && dumpPlainField(builder, null, child.getReferOneof(), null, inputs[usedInputIdx],
                                rowContext, fieldPath)) {
                    hasData = true;
                    dumpedOneof.add(child.getReferOneof().getFullName());

                    for (var subField : child.getReferOneof().getSortedFields()) {
                        missingFields.remove(subField.getIndex());
                    }
                } else {
                    if (child.isNotNull()) {
                        rowContext.addIgnoreReason(
                                String.format("oneof %s.%s is empty but set not null, we will ignore this row",
                                        fieldPath, child.getName()));
                    }
                }

                ++usedInputIdx;
                ++fieldSize;
            } else {
                String subFieldPath = String.format("%s.%s", fieldPath, child.getName());
                if (usedInputIdx < inputs.length
                        && dumpPlainField(builder, null, child, null, inputs[usedInputIdx], rowContext,
                                subFieldPath)) {
                    hasData = true;

                    missingFields.remove(child.getIndex());
                } else {
                    // list和map允许空
                    if ((child.isList() || child.isMap()) && (child.getListExtension() == null ||
                            child.getListExtension().minSize <= 0)) {
                        missingFields.remove(child.getIndex());
                    } else if (child.getType() == DataDstWriterNode.JAVA_TYPE.STRING
                            || child.getType() == DataDstWriterNode.JAVA_TYPE.BYTES
                            || child.getType() == DataDstWriterNode.JAVA_TYPE.MESSAGE) {
                        // 字符串类型允许空
                        // Message类型允许空，如果由字段缺失缺要求填充，子调用里会处理
                        missingFields.remove(child.getIndex());
                    }
                    if (child.isNotNull()) {
                        rowContext.addIgnoreReason(
                                String.format("field %s is empty but set not null, we will ignore this row",
                                        subFieldPath));
                    }
                }

                ++fieldSize;
                ++usedInputIdx;
            }
        }

        if (!missingFields.isEmpty()) {
            String message = String.format(
                    "Try to convert %s need at least %d fields, at most %d fields, but only provide %d fields.%s  > File: %s, Table: %s, Row: %d, Column: %d(%s)%s  > Missing fields: %s%s  > Data: %s",
                    field.getTypeDescriptor().getFullName(), atLeastFieldSize, fieldSize, inputs.length,
                    ProgramOptions.getEndl(),
                    rowContext.fileName, rowContext.tableName,
                    DataSrcImpl.getOurInstance().getLastRowNum() + 1,
                    DataSrcImpl.getOurInstance().getLastColumnNum() + 1,
                    ExcelEngine.getColumnName(DataSrcImpl.getOurInstance().getLastColumnNum() + 1),
                    ProgramOptions.getEndl(),
                    String.join(",", missingFields.values().stream().map(DataDstFieldDescriptor::getName)
                            .collect(Collectors.toList())),
                    ProgramOptions.getEndl(),
                    joinPlainInputHint(field.getTypeDescriptor().mutableExtension().plainSeparator, inputs));
            ProgramOptions.getLogger().warn(message);
        } else if (inputs.length > fieldSize) {
            String message = String.format(
                    "Try to convert %s need at least %d fields, at most %d fields, but provide %d fields.%s  > Data: %s",
                    field.getTypeDescriptor().getFullName(), atLeastFieldSize, fieldSize, inputs.length,
                    ProgramOptions.getEndl(), ProgramOptions.getEndl(),
                    joinPlainInputHint(field.getTypeDescriptor().mutableExtension().plainSeparator, inputs));
            throw new ConvException(message);
        }

        if (!hasData) {
            return null;
        }

        ret.value = builder.build();
        return ret;
    }

    /**
     * 生成常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    @SuppressWarnings("unchecked")
    @Override
    public HashMap<String, Object> buildConst() {
        if (!loadProtocolFiles(true)) {
            return null;
        }

        if (null == taskPbs.get().enums) {
            return null;
        }

        HashMap<String, Object> ret = new HashMap<>();

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : taskPbs.get().fileDescs.entrySet()) {
            if (fdp.getValue().getPackage().equals("google.protobuf")) {
                continue;
            }

            String[] names = null;
            HashMap<String, Object> fdRoot = ret;

            if (false == fdp.getValue().getPackage().isEmpty()) {
                names = fdp.getValue().getPackage().split("\\.");
            }

            if (null != names) {
                for (String seg : names) {
                    if (seg.isEmpty()) {
                        continue;
                    }
                    if (fdRoot.containsKey(seg)) {
                        Object node = fdRoot.get(seg);
                        if (node instanceof HashMap) {
                            fdRoot = (HashMap<String, Object>) node;
                        } else {
                            this.logErrorMessage("package name %s conflict(failed in %s).", fdp.getValue().getPackage(),
                                    seg);
                            break;
                        }
                    } else {
                        HashMap<String, Object> node = new HashMap<>();
                        fdRoot.put(seg, node);
                        fdRoot = node;
                    }
                }
            }

            // dump oneof and enum in message
            for (Descriptors.Descriptor msgDesc : fdp.getValue().getMessageTypes()) {
                DataDstPbHelper.dumpConstIntoHashMap(fdp.getValue().getPackage(), fdRoot, msgDesc);
            }

            for (Descriptors.EnumDescriptor enumDesc : fdp.getValue().getEnumTypes()) {
                DataDstPbHelper.dumpConstIntoHashMap(fdp.getValue().getPackage(), fdRoot, enumDesc);
            }
        }

        return ret;
    }

    /**
     * 转储常量数据
     *
     * @return 常量数据,不支持的时候返回空
     */
    @Override
    public final byte[] dumpConst(HashMap<String, Object> data) throws ConvException, IOException {
        // protobuf的常量输出直接复制描述文件就好了
        if (ProgramOptions.getInstance().protocolFile.length == 1
                && ProgramOptions.getInstance().protocolFile[0].equals(ProgramOptions.getInstance().protoDumpFile)) {
            return null;
        }

        try {
            DescriptorProtos.FileDescriptorSet.Builder fdsBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
            for (var fdp : taskPbs.get().files.entrySet()) {
                if (fdp.getValue().getPackage().equals("google.protobuf")) {
                    continue;
                }

                fdsBuilder.addFile(fdp.getValue());
            }

            return fdsBuilder.build().toByteArray();
        } catch (Exception e) {
            this.logErrorMessage("Serialize FileDescriptorSet failed: %s.", e.getMessage());
        }

        return null;
    }

    /**
     * 生成选项数据
     *
     * @return 选项数据,不支持的时候返回空
     */
    @Override
    public HashMap<String, Object> buildOptions(ProgramOptions.ProtoDumpType dumpType) {
        if (!loadProtocolFiles(true)) {
            return null;
        }

        com.google.protobuf.ExtensionRegistry customExtensions = com.google.protobuf.ExtensionRegistry.newInstance();

        Xresloader.registerAllExtensions(customExtensions);
        XresloaderUe.registerAllExtensions(customExtensions);

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : taskPbs.get().fileDescs.entrySet()) {
            for (Descriptors.FieldDescriptor subDesc : fdp.getValue().getExtensions()) {
                if (subDesc.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                    customExtensions.add(subDesc, DynamicMessage.newBuilder(subDesc.getMessageType()).build());
                } else {
                    customExtensions.add(subDesc);
                }
            }
        }

        HashMap<String, Object> ret = new HashMap<>();
        LinkedList<Object> files = new LinkedList<>();

        ArrayList<HashMap.Entry<String, Descriptors.FileDescriptor>> sortedArray = new ArrayList<>();
        sortedArray.ensureCapacity(taskPbs.get().fileDescs.size());
        sortedArray.addAll(taskPbs.get().fileDescs.entrySet());
        sortedArray.sort((l, r) -> {
            return l.getValue().getFullName().compareTo(r.getValue().getFullName());
        });

        for (HashMap.Entry<String, Descriptors.FileDescriptor> fdp : sortedArray) {
            if (fdp.getValue().getPackage().equals("google.protobuf")) {
                continue;
            }
            DataDstPbHelper.dumpOptionsIntoHashMap(dumpType, files, fdp.getValue(), customExtensions);
        }

        ret.put("files", files);

        return ret;
    }
}
