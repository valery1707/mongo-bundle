package com.github.valery1707.mongo.bundle;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipUtils {
    private ZipUtils() {
    }

    @SuppressWarnings("UnnecessarySemicolon")
    public static void extract(Path path, Function<Optional<ZipEntry>, Optional<Path>> targetMapper) throws IOException {
        try (
                InputStream input = Files.newInputStream(path);
                ZipInputStream zip = new ZipInputStream(input);
        ) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Optional<Path> target = targetMapper.apply(Optional.of(entry));
                if (target.isPresent()) {
                    try (
                            OutputStream output = Files.newOutputStream(target.get());
                    ) {
                        IOUtils.copy(zip, output);
                    }
                }
            }
        }
    }
}
