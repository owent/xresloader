package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
public class ProgramOptionsHelpTest {
    private void assertHelp(int expectedResult, String... arguments) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            System.setErr(output);
            assertEquals(expectedResult, ProgramOptions.getInstance().init(arguments));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            ProgramOptions.getInstance().reset();
        }

        String help = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(help.contains("java -client -jar"), help);
        assertTrue(help.contains("--help"), help);
        assertTrue(help.contains("--output-type"), help);
        assertTrue(help.contains("--data-source-mapping-mode"), help);
        assertTrue(help.contains("-Dlog4j.configurationFile=log4j2.xml"), help);
    }

    @Test
    public void helpOptionPrintsUsageAndSucceeds() {
        assertHelp(1, "--help");
    }

    @Test
    public void unknownOptionPrintsUsageAndFails() {
        assertHelp(-1, "--unknown-option-for-test");
    }
}
