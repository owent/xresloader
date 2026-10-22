package org.xresloader.core.engine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;

/** Shared input bytes; callers receive independent cursors, never a mutable array. */
public final class InputFileCache {
    private static final SharedCache<FileKey, byte[]> contents = new SharedCache<>();

    private InputFileCache() {}

    public record FileKey(String path, long size, FileTime modified, Object fileKey) {}

    public static FileKey key(File file) throws IOException {
        File canonical = file.getCanonicalFile();
        BasicFileAttributes attributes = Files.readAttributes(canonical.toPath(), BasicFileAttributes.class);
        return new FileKey(canonical.getPath(), attributes.size(), attributes.lastModifiedTime(), attributes.fileKey());
    }

    private static byte[] read(File file) throws IOException {
        FileKey key = key(file);
        return contents.getOrLoad(key, () -> Files.readAllBytes(new File(key.path()).toPath()));
    }

    public static InputStream open(File file) throws IOException {
        return new ByteArrayInputStream(read(file));
    }

}
