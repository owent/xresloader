package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Test;
import org.xresloader.core.scheme.SchemeDataSourceImpl;

class LegacyApiCompatibilityTest {
    @Test
    @SuppressWarnings("deprecation")
    void oldLoggerEntryPointRemainsAvailableToJavaAndJavascript() throws Exception {
        assertSame(ProgramOptions.getLogger(), ProgramOptions.getLoger());
        var engine = new ScriptEngineManager().getEngineByName("javascript");
        assertEquals(true, engine.eval("var Options = Java.type('org.xresloader.core.ProgramOptions');"
                + "Options.getLoger() === Options.getLogger();"));
        assertEquals("INT", engine.eval("Java.type('org.xresloader.core.data.dst.DataDstWriterNode$JAVA_TYPE').INT.name();"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void camelCaseEntryPointInvokesExistingSchemeImplementations() throws Exception {
        SchemeDataSourceImpl source = new SchemeDataSourceImpl() {
            public int load() { return 0; }
            public boolean load_scheme(String name) { return name.equals("legacy"); }
            public boolean isSupportMultipleScheme() { return false; }
        };
        assertTrue(source.loadScheme("legacy"));
    }
}
