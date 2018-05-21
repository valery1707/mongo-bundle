package com.github.valery1707.mongo.bundle;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

import static java.nio.file.attribute.PosixFilePermissions.asFileAttribute;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsTest {
    @Test
    public void testIsReadableFile_notExists() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path file = root.resolve("file.ext");
            assertThat(FileUtils.isReadableFile(file)).isFalse();
        }
    }

    @Test
    public void testIsReadableFile_notFile() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            assertThat(FileUtils.isReadableFile(root)).isFalse();
        }
    }

    @Test
    public void testIsReadableFile_notReadable() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path file = Files.createFile(root.resolve("file.ext"), asFileAttribute(singleton(PosixFilePermission.GROUP_WRITE)));
            assertThat(FileUtils.isReadableFile(file)).isFalse();
        }
    }

    @Test
    public void testIsReadableFile_success() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path file = Files.createFile(root.resolve("file.ext"));
            assertThat(FileUtils.isReadableFile(file)).isTrue();
        }
    }

    @Test
    public void testIsReadableDir_notExists() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path test = root.resolve("test");
            assertThat(FileUtils.isReadableDirectory(test)).isFalse();
        }
    }

    @Test
    public void testIsReadableDir_notDirectory() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path test = Files.createFile(root.resolve("test"));
            assertThat(FileUtils.isReadableDirectory(test)).isFalse();
        }
    }

    @Test
    public void testIsReadableDir_notReadable() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path test = Files.createDirectory(root.resolve("test"), asFileAttribute(singleton(PosixFilePermission.GROUP_WRITE)));
            assertThat(FileUtils.isReadableDirectory(test)).isFalse();
        }
    }

    @Test
    public void testIsReadableDir_success() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path test = Files.createDirectory(root.resolve("test"));
            assertThat(FileUtils.isReadableDirectory(test)).isTrue();
        }
    }

    @Test
    public void testList_notDirectory() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Path file = Files.createFile(root.resolve("file.ext"));
            try {
                assertThat(FileUtils.list(file)).isNull();
            } catch (IllegalStateException e) {
                assertThat(e).isNotNull().hasCauseInstanceOf(IOException.class);
            }
        }
    }

    @Test
    public void testList_empty() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            assertThat(FileUtils.list(root)).isEmpty();
        }
    }

    @Test
    public void testList_NotMatched() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            Files.createFile(root.resolve("file.ext"));
            assertThat(FileUtils.list(root, file -> file.endsWith(".txt"))).isEmpty();
        }
    }

    @Test
    public void testList_oneLevel() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            String[] names = new String[]{"dir1", "dir2"};
            for (String name : names) {
                Path dir = Files.createDirectories(root.resolve(name));
                assertThat(dir).isDirectory();
                assertThat(Files.createFile(dir.resolve(name + ".txt"))).isRegularFile();
            }
            assertThat(FileUtils.list(root)).hasSize(names.length);
        }
    }

    @Test
    public void testList_filterAnd() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            String[] names = new String[]{"dir1", "dir2"};
            for (String name : names) {
                Path dir = Files.createDirectories(root.resolve(name));
                assertThat(dir).isDirectory();
                assertThat(Files.createFile(dir.resolve(name + ".txt"))).isRegularFile();
            }
            assertThat(FileUtils.list(root
                    , t -> t.getName(t.getNameCount() - 1).toString().endsWith("1")
                    , Files::isDirectory
            )).hasSize(1);
        }
    }

    @Test
    public void testLastModifiedTime_notExist() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            try {
                assertThat(FileUtils.getLastModifiedTime(root.resolve("unknown.txt"))).isNull();
            } catch (IllegalStateException e) {
                assertThat(e).isNotNull().hasCauseInstanceOf(IOException.class);
            }
        }
    }

    @Test
    public void testLastModifiedTime_success() throws IOException {
        try (
                FileSystem fileSystem = MemoryFileSystemBuilder.newLinux().build()
        ) {
            Path root = Files.createDirectories(fileSystem.getPath("tmp", "root"));
            assertThat(FileUtils.getLastModifiedTime(root)).isNotNull();
        }
    }
}
