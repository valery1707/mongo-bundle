package com.github.valery1707.mongo.bundle;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.store.IDownloader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.flapdoodle.embed.process.io.file.Files.createTempFile;

@SuppressWarnings("UnnecessarySemicolon")
public class BundleExtractor implements IDownloader {
    private final boolean snapshot;

    public BundleExtractor(boolean snapshot) {
        this.snapshot = snapshot;
    }

    public BundleExtractor() {
        this(true);
    }

    @Override
    public String getDownloadUrl(IDownloadConfig runtime, Distribution distribution) {
        return null;
    }

    @Override
    public File download(IDownloadConfig config, Distribution distribution) throws IOException {
        File ret = createTempFile(
                PropertyOrPlatformTempDir.defaultInstance(),
                config.getFileNaming().nameFor(
                        config.getDownloadPrefix(), "." + config.getPackageResolver().getArchiveType(distribution)
                )
        );
        String version = distribution
                .getVersion()
                .toString()
                .replaceFirst("^V", "")
                .replace('_', '.');
        Path jar = MavenUtils
                .findLibrary("com.github.valery1707", "mongo-bundle", version, ".jar", snapshot)
                .orElseThrow(() -> new IOException("Mongo bundle jar not found"));
        String name = String.format("mongo/%s-%s-%s.%s",
                distribution.getPlatform(), distribution.getVersion(), distribution.getBitsize(),
                config.getPackageResolver().getArchiveType(distribution)
        );
        try (
                InputStream input = Files.newInputStream(jar);
                ZipInputStream zip = new ZipInputStream(input);
                OutputStream output = new FileOutputStream(ret);
        ) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    IOUtils.copy(zip, output);
                    break;
                }
            }
        }
        return ret;
    }
}
