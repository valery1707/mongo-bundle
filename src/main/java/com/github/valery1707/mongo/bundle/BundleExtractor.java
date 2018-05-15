package com.github.valery1707.mongo.bundle;

import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.directories.PropertyOrPlatformTempDir;
import de.flapdoodle.embed.process.store.IDownloader;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.flapdoodle.embed.process.io.file.Files.createTempFile;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@SuppressWarnings("UnnecessarySemicolon")
public class BundleExtractor implements IDownloader {
    private Path mavenHome = null;

    private List<Path> findMavenHome() throws IOException {
        if (mavenHome != null) {
            return singletonList(mavenHome);
        }
        //region env: MAVEN_HOME
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null) {
            Path home = Paths.get(mavenHome);
            Path settings = home.resolve("conf").resolve("settings.xml");
            if (isReadableFile(settings)) {
                this.mavenHome = extractFromXml(settings, "/settings/localRepository")
                        .map(path -> path.replace("${user.home}", System.getenv("USERPROFILE")))
                        .map(Paths::get)
                        .filter(Files::exists)
                        .filter(Files::isDirectory)
                        .filter(Files::isReadable)
                        .orElse(null);
                if (this.mavenHome != null) {
                    return singletonList(this.mavenHome);
                }
            }
        }
        //endregion
        //region env: USERPROFILE
        this.mavenHome = Optional
                .ofNullable(System.getenv("USERPROFILE"))
                .map(Paths::get)
                .map(home -> home.resolve(".m2").resolve("repository"))
                .filter(Files::exists)
                .filter(Files::isDirectory)
                .filter(Files::isReadable)
                .orElse(null);
        //endregion

        return this.mavenHome != null ? singletonList(this.mavenHome) : emptyList();
    }

    static Optional<String> extractFromXml(Path xml, String searchTag) throws IOException {
        try (
                InputStream stream = Files.newInputStream(xml);
        ) {
            String name = "";
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader reader = factory.createFilteredReader(
                    factory.createXMLEventReader(stream, "UTF-8"),
                    event -> event.isStartElement() || event.isEndElement() || event.isCharacters()
            );
            while (reader.hasNext()) {
                XMLEvent event = (XMLEvent) reader.next();
                if (event.isCharacters() && !event.asCharacters().isWhiteSpace() && searchTag.equals(name)) {
                    return Optional.ofNullable(event.asCharacters().getData());
                } else if (event.isStartElement()) {
                    name += "/" + event.asStartElement().getName().getLocalPart();
                } else if (event.isEndElement()) {
                    name = name.substring(0, name.lastIndexOf('/'));
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new IOException("Fail to parse xml from " + xml.normalize().toAbsolutePath().toString(), e);
        }
        return Optional.empty();
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
        String snapshot = version + "-SNAPSHOT";
        Path jar = findMavenHome()
                .stream()
                .map(path -> path.resolve("com").resolve("github").resolve("valery1707"))
                .map(path -> path.resolve("mongo-bundle"))
                .flatMap(path -> Stream
                        .of(
                                path.resolve(version),
                                path.resolve(snapshot)
                        )
                        .filter(Files::isDirectory)
                )
                .flatMap(path -> Stream
                        .of(
                                path.resolve("mongo-bundle" + "-" + version + ".jar"),
                                path.resolve("mongo-bundle" + "-" + snapshot + ".jar")
                        )
                )
                .filter(BundleExtractor::isReadableFile)
                .findFirst()
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

    private static boolean isReadableFile(Path jar) {
        return Files.exists(jar) && Files.isRegularFile(jar) && Files.isReadable(jar);
    }
}
