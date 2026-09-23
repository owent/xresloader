package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xresloader.core.data.dst.DataDstPbHelper;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.UninterpretedOption;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;

public class DataDstPbHelperTest {
    @Test
    public void fieldOptionsPreserveEnumsRepeatedEnumsAndNestedMessages() throws Exception {
        FieldOptions options = FieldOptions.newBuilder()
                .setRetention(FieldOptions.OptionRetention.RETENTION_RUNTIME)
                .addTargets(FieldOptions.OptionTargetType.TARGET_TYPE_FIELD)
                .addTargets(FieldOptions.OptionTargetType.TARGET_TYPE_MESSAGE)
                .addUninterpretedOption(UninterpretedOption.newBuilder().setIdentifierValue("enabled")
                        .addName(UninterpretedOption.NamePart.newBuilder().setNamePart("custom").setIsExtension(false)))
                .build();
        FileDescriptor file = FileDescriptor.buildFrom(FileDescriptorProto.newBuilder()
                .setName("test_options.proto")
                .addMessageType(DescriptorProto.newBuilder().setName("Config")
                        .addField(FieldDescriptorProto.newBuilder().setName("value").setNumber(1)
                                .setType(FieldDescriptorProto.Type.TYPE_STRING).setOptions(options)))
                .build(), new FileDescriptor[0]);

        var dump = DataDstPbHelper.dumpOptionsIntoHashMap(ProgramOptions.ProtoDumpType.OPTIONS,
                file.getMessageTypes().get(0).getFields().get(0), ExtensionRegistry.newInstance());
        Map<?, ?> actualOptions = assertInstanceOf(Map.class, dump.get("options"));
        assertEquals(Map.of("name", "RETENTION_RUNTIME", "number", 1), actualOptions.get("retention"));
        assertEquals(List.of(Map.of("name", "TARGET_TYPE_FIELD", "number", 4),
                Map.of("name", "TARGET_TYPE_MESSAGE", "number", 3)), actualOptions.get("targets"));
        assertEquals(List.of(Map.of("identifier_value", "enabled",
                "name", List.of(Map.of("name_part", "custom", "is_extension", false)))),
                actualOptions.get("uninterpreted_option"));
    }
}
