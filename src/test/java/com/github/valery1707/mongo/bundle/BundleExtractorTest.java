package com.github.valery1707.mongo.bundle;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static com.github.valery1707.mongo.bundle.BundleExtractor.extractFromXml;
import static org.assertj.core.api.Assertions.assertThat;

public class BundleExtractorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @SuppressWarnings("UnnecessarySemicolon")
    private Path extract(Supplier<InputStream> streamSupplier) throws IOException {
        Path target = folder.newFile().toPath();
        try (
                InputStream input = streamSupplier.get();
                OutputStream output = Files.newOutputStream(target);
        ) {
            IOUtils.copy(input, output);
        }
        return target;
    }

    private Path extract(String resource) throws IOException {
        return extract(() -> BundleExtractorTest.class.getResourceAsStream(resource));
    }

    @Test
    public void testExtractFromXml_Absent() throws IOException {
        assertThat(extractFromXml(extract("/xml/xml-1.xml"), "/settings/localRepository")).isEmpty();
    }

    @Test
    public void testExtractFromXml_Exists() throws IOException {
        assertThat(extractFromXml(extract("/xml/xml-1.xml"), "/settings/servers/server/id")).isNotEmpty().contains("siteServer");
        assertThat(extractFromXml(extract("/xml/xml-2.xml"), "/settings/localRepository")).isNotEmpty().contains("/path/to/local/repo");
    }
}
