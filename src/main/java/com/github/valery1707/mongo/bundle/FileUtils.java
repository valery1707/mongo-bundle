package com.github.valery1707.mongo.bundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public final class FileUtils {
    private FileUtils() {
    }

    static boolean isReadableFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    static boolean isReadableDirectory(Path path) {
        return Files.exists(path) && Files.isDirectory(path) && Files.isReadable(path);
    }

    static Stream<Path> list(Path root, Predicate<? super Path> filter) {
        try {
            return Files
                    .list(root)
                    .filter(filter::test);
        } catch (IOException e) {
            throw new IllegalStateException("Fail to list files in " + root.normalize().toAbsolutePath().toString(), e);
        }
    }

    @SafeVarargs
    static Stream<Path> list(Path root, Predicate<? super Path>... filters) {
        return list(root, file ->
                Stream.of(filters).allMatch(filter -> filter.test(file))
        );
    }

    public static FileTime getLastModifiedTime(Path path, LinkOption... options) {
        try {
            return Files.getLastModifiedTime(path, options);
        } catch (IOException e) {
            throw new IllegalStateException("Fail to get last modified time for " + path.normalize().toAbsolutePath().toString(), e);
        }
    }

}
