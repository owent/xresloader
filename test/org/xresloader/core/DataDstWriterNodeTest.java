package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstFieldDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstOneofDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor;
import org.xresloader.core.data.dst.DataDstWriterNode.FIELD_LABEL_TYPE;
import org.xresloader.core.data.dst.DataDstWriterNode.JAVA_TYPE;
import org.xresloader.core.data.dst.DataDstWriterNode.SPECIAL_MESSAGE_TYPE;
import org.xresloader.core.data.vfy.DataVerifyImpl;

public class DataDstWriterNodeTest {
    @Test
    public void oneofLinksFieldsWithoutCallingOverridesDuringConstruction() {
        DataDstTypeDescriptor owner = new DataDstTypeDescriptor(JAVA_TYPE.MESSAGE, "test", "Event", null,
                SPECIAL_MESSAGE_TYPE.NONE, null, null);
        DataDstFieldDescriptor first = new DataDstFieldDescriptor(null, 1, "first", FIELD_LABEL_TYPE.OPTIONAL, null) {
            @Override
            public void setReferOneof(DataDstOneofDescriptor oneof) {
                throw new AssertionError("The oneof constructor must not invoke overridable field methods with this");
            }
        };
        DataDstFieldDescriptor second = new DataDstFieldDescriptor(null, 2, "second", FIELD_LABEL_TYPE.OPTIONAL, null);
        HashMap<String, DataDstFieldDescriptor> fields = new HashMap<>();
        fields.put(second.getName(), second);
        fields.put(first.getName(), first);
        DataVerifyImpl validator = new DataVerifyImpl("test") {
            @Override
            public boolean isValid() {
                return true;
            }
        };

        DataDstOneofDescriptor oneof = new DataDstOneofDescriptor(owner, fields, 0, "choice", null, validator);

        assertSame(oneof, first.getReferOneof());
        assertSame(oneof, second.getReferOneof());
        assertSame(first, oneof.getFieldById(1));
        assertSame(second, oneof.getFieldByName("second"));
        assertEquals(List.of(first, second), oneof.getSortedFields());
        assertEquals("test.Event.choice", oneof.getFullName());
        assertSame(owner, oneof.getOwnerDescriptor());
        assertSame(validator, oneof.getTypeValidator());
        assertTrue(oneof.hasTypeValidator());
    }
}
