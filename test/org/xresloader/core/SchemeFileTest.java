package org.xresloader.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xresloader.core.data.err.InitializeException;
import org.xresloader.core.scheme.SchemeConf;
import org.xresloader.core.scheme.SchemeDataSourceConf;
import org.xresloader.core.scheme.SchemeDataSourceJson;

class SchemeFileTest {
    @TempDir
    Path directory;

    @AfterEach
    void resetConfiguration() {
        ProgramOptions.getInstance().reset();
        SchemeConf.getInstance().reset();
    }

    @Test
    void iniFilesPreserveSettingsWithAndWithoutUtf8Bom() throws Exception {
        for (String prefix : new String[] { "", "\ufeff" }) {
            Path file = directory.resolve("scheme.ini");
            Files.writeString(file, prefix + "[example]\nProtoName=role_cfg\nOutputFile=example.bin\nKeyRow=2\n",
                    StandardCharsets.UTF_8);
            ProgramOptions.getInstance().dataSourceFile = file.toString();
            var source = new SchemeDataSourceConf();
            assertEquals(0, source.load());
            assertTrue(source.loadScheme("example"));
            assertEquals("role_cfg", SchemeConf.getInstance().getProtoName());
            assertEquals("example.bin", SchemeConf.getInstance().getOutputFile());
            assertEquals(2, SchemeConf.getInstance().getKey().getRow());
            // Windows rejects this if the reader still owns a file handle.
            Files.delete(file);
        }
    }

    @Test
    void jsonFilesCloseAfterLoadingWithAndWithoutUtf8Bom() throws Exception {
        for (String prefix : new String[] { "", "\ufeff" }) {
            Path file = directory.resolve("scheme.json");
            Files.writeString(file, prefix + "{\"example\":{\"OutputFile\":\"数据.bin\"}}", StandardCharsets.UTF_8);
            ProgramOptions.getInstance().dataSourceFile = file.toString();
            assertEquals(0, new SchemeDataSourceJson().load());
            Files.delete(file);
        }
    }

    @Test
    void invalidJsonStillFailsAndReleasesItsInput() throws Exception {
        Path file = directory.resolve("invalid.json");
        Files.writeString(file, "{\"invalid\":", StandardCharsets.UTF_8);
        ProgramOptions.getInstance().dataSourceFile = file.toString();
        assertThrows(InitializeException.class, () -> new SchemeDataSourceJson().load());
        Files.delete(file);
    }
}
