package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.xresloader.core.data.dst.DataDstPb;
import org.xresloader.core.scheme.SchemeConf;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.Descriptors;

@Timeout(30)
class ProtocolCacheTest {
    @TempDir Path directory;

    @AfterEach
    void cleanup() {
        DataDstPb.clearThreadState();
        ProgramOptions.clearThreadState();
        SchemeConf.clearThreadState();
    }

    private static Object field(Object value, String name) throws Exception {
        var field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }

    private static Object context() throws Exception {
        var field = DataDstPb.class.getDeclaredField("taskPbs");
        field.setAccessible(true);
        return ((ThreadLocal<?>) field.get(null)).get();
    }

    private static DescriptorProtos.FileDescriptorProto.Builder protocol(Type type) {
        return DescriptorProtos.FileDescriptorProto.newBuilder().setName("record.proto").setPackage("fixture")
                .setSyntax("proto2").addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("Record")
                        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder().setName("value").setNumber(1)
                                .setType(type).setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)))
                .addEnumType(DescriptorProtos.EnumDescriptorProto.newBuilder().setName("Kind")
                        .addValue(DescriptorProtos.EnumValueDescriptorProto.newBuilder().setName("ZERO").setNumber(0)));
    }

    private Path write(String name, DescriptorProtos.FileDescriptorProto... files) throws Exception {
        var set = DescriptorProtos.FileDescriptorSet.newBuilder();
        for (var file : files) {
            set.addFile(file);
        }
        Path path = directory.resolve(name);
        Files.write(path, set.build().toByteArray());
        return path;
    }

    private static DataDstPb initialize(boolean ignoreUnknown, Path... inputs) {
        ProgramOptions.getInstance().protocolFile = java.util.Arrays.stream(inputs).map(Path::toString).toArray(String[]::new);
        ProgramOptions.getInstance().protocolIgnoreUnknownDependency = ignoreUnknown;
        SchemeConf.getInstance().setProtoName("fixture.Record");
        var converter = new DataDstPb();
        assertTrue(converter.init());
        return converter;
    }

    private static Descriptors.Descriptor message(DataDstPb converter) throws Exception {
        return (Descriptors.Descriptor) field(converter, "currentMsgDesc");
    }

    @Test
    void tasksShareTheSameReadOnlyMapsButKeepTheirMutableState() throws Exception {
        Path input = write("shared.pb", protocol(Type.TYPE_INT32).build());
        // The cold builder retains its original traversal order; subsequent tasks share the published snapshot.
        initialize(false, input);
        cleanup();
        var barrier = new CyclicBarrier(4);
        var pool = Executors.newFixedThreadPool(4);
        try {
            var results = new ArrayList<Future<Object>>();
            for (int i = 0; i < 8; ++i) {
                Path spelling = i % 2 == 0 ? input : directory.resolve(".").resolve("shared.pb");
                results.add(pool.submit(() -> {
                    try {
                        barrier.await(10, TimeUnit.SECONDS);
                        initialize(false, spelling);
                        Object state = context();
                        barrier.await(10, TimeUnit.SECONDS);
                        return state;
                    } finally {
                        cleanup();
                    }
                }));
            }
            Object first = results.get(0).get(15, TimeUnit.SECONDS);
            for (var result : results) {
                Object state = result.get(15, TimeUnit.SECONDS);
                for (String name : List.of("files", "messages", "messageFields", "enums", "enumValues", "oneofs",
                        "fileDescs", "messageDescs")) {
                    assertSame(field(first, name), field(state, name), name);
                    assertThrows(UnsupportedOperationException.class, () -> ((Map<?, ?>) field(state, name)).clear());
                }
                if (state != first) {
                    for (String name : List.of("validatorCache", "dataDstDescs", "enumDescs", "quickIntegerValuesAlias",
                            "quickIntegerValuesContainer", "fileDescsFailed", "fileNames")) {
                        assertNotSame(field(first, name), field(state, name), name);
                    }
                }
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void inputOrderAndDependencyPolicyBelongToTheSchemaKey() throws Exception {
        Path integer = write("integer.pb", protocol(Type.TYPE_INT32).build());
        Path string = write("string.pb", protocol(Type.TYPE_STRING).build());
        assertEquals(Descriptors.FieldDescriptor.JavaType.INT, message(initialize(false, integer, string))
                .findFieldByName("value").getJavaType());
        cleanup();
        initialize(false, integer, string);
        Object ordered = field(context(), "fileDescs");
        cleanup();
        assertEquals(Descriptors.FieldDescriptor.JavaType.STRING, message(initialize(false, string, integer))
                .findFieldByName("value").getJavaType());
        assertNotSame(ordered, field(context(), "fileDescs"));
        cleanup();
        initialize(true, integer, string);
        assertNotSame(ordered, field(context(), "fileDescs"));
        cleanup();
        initialize(false, integer, string);
        assertSame(ordered, field(context(), "fileDescs"));
    }

    @Test
    void incrementalLoadsDetachWithoutModifyingOtherTasksOrTheSharedSchema() throws Exception {
        Path input = write("shared.pb", protocol(Type.TYPE_INT32).build());
        Path extra = write("extra.pb", DescriptorProtos.FileDescriptorProto.newBuilder().setName("extra.proto")
                .setPackage("other").setSyntax("proto2").build());
        initialize(false, input);
        cleanup();
        initialize(false, input);
        var shared = (Map<?, ?>) field(context(), "files");
        initialize(false, input, extra);
        var extended = (Map<?, ?>) field(context(), "files");
        assertNotSame(shared, extended);
        assertTrue(extended.containsKey("extra.proto"));
        assertFalse(shared.containsKey("extra.proto"));
        cleanup();
        initialize(false, input);
        assertSame(shared, field(context(), "files"));
        assertFalse(((Map<?, ?>) field(context(), "files")).containsKey("extra.proto"));
    }

    @Test
    void incompleteSchemasAreNotPublishedAndChangedInputsAreReloaded() throws Exception {
        var missing = protocol(Type.TYPE_INT32).addDependency("missing.proto").build();
        Path input = write("retry.pb", missing);
        var first = message(initialize(true, input));
        cleanup();
        assertNotSame(first, message(initialize(true, input)), "Incomplete linking must be retried per task");
        cleanup();
        ProgramOptions.getInstance().protocolFile = new String[] {input.toString()};
        SchemeConf.getInstance().setProtoName("fixture.Record");
        assertFalse(new DataDstPb().init(), "Strict tasks must not reuse a permissive result");
        cleanup();
        write("retry.pb", missing, DescriptorProtos.FileDescriptorProto.newBuilder().setName("missing.proto")
                .setSyntax("proto2").build());
        var complete = message(initialize(false, input));
        cleanup();
        assertSame(complete, message(initialize(false, input)));
        Object shared = field(context(), "fileDescs");
        cleanup();
        initialize(false, input);
        assertSame(shared, field(context(), "fileDescs"));
        assertThrows(UnsupportedOperationException.class, () -> ((Map<?, ?>) shared).clear());
    }
}
