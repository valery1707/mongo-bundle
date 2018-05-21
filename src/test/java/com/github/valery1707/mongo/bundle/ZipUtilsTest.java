package com.github.valery1707.mongo.bundle;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipUtilsTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @SuppressWarnings("UnnecessarySemicolon")
    private File writeZip(Map<String, String> content) throws IOException {
        File file = folder.newFile("temp.zip");
        try (
                OutputStream stream = new FileOutputStream(file);
                ZipOutputStream zip = new ZipOutputStream(stream);
        ) {
            for (Map.Entry<String, String> entry : content.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zip.putNextEntry(zipEntry);
                IOUtils.write(entry.getValue(), zip, UTF_8);
                zip.closeEntry();
            }
        }
        return file;
    }

    @Test
    public void testExtract() throws IOException {
        Map<String, String> content = new LinkedHashMap<>();
        content.put("file1.txt", "file1");
        content.put("file2.txt", "file2");
        File zip = writeZip(content);
        File target = folder.newFile();
        ZipUtils.extract(zip.toPath(), entry -> entry
                .map(ZipEntry::getName)
                .filter("file2.txt"::equals)
                .map(__ -> target.toPath())
        );
        assertThat(target).exists().hasContent("file2");
    }
}
