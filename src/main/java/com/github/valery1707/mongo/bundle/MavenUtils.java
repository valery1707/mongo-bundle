package com.github.valery1707.mongo.bundle;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.valery1707.mongo.bundle.FileUtils.isReadableFile;
import static com.github.valery1707.mongo.bundle.XmlUtils.extractFromXml;
import static java.util.Collections.emptyList;

@SuppressWarnings("WeakerAccess")
public final class MavenUtils {
    static FileSystem FS = FileSystems.getDefault();
    static Function<String, String> ENV = System::getenv;

    private MavenUtils() {
    }

    final static AtomicReference<List<Path>> repository = new AtomicReference<>();

    public static List<Path> repositoryRoots() throws IOException {
        if (repository.get() != null) {
            return repository.get();
        }
        //region env: MAVEN_HOME
        String mavenHome = System.getProperty("env.maven.home", ENV.apply("MAVEN_HOME"));
        if (mavenHome != null) {
            Path home = FS.getPath(mavenHome);
            Path settings = home.resolve("conf").resolve("settings.xml");
            if (isReadableFile(settings)) {
                extractFromXml(settings, "/settings/localRepository")
                        .map(path -> path.replace("${user.home}", System.getProperty("user.home", ENV.apply("USERPROFILE"))))
                        .map(FS::getPath)
                        .filter(FileUtils::isReadableDirectory)
                        .map(Collections::singletonList)
                        .ifPresent(path -> repository.compareAndSet(null, path));
                if (repository.get() != null) {
                    return repository.get();
                }
            }
        }
        //endregion
        //region env: USERPROFILE
        Optional
                .ofNullable(System.getProperty("user.home", ENV.apply("USERPROFILE")))
                .map(FS::getPath)
                .map(home -> home.resolve(".m2").resolve("repository"))
                .filter(FileUtils::isReadableDirectory)
                .map(Collections::singletonList)
                .ifPresent(path -> repository.compareAndSet(null, path));
        //endregion

        return repository.get() != null ? repository.get() : emptyList();
    }

    public static Optional<Path> findLibrary(String group, String artifact, String version, String classifier, boolean snapshot) throws IOException {
        Stream<String> groupNames = Stream.of(group.split("\\."));
        return repositoryRoots()
                .stream()
                //group
                .map(root -> groupNames.reduce(root, Path::resolve, (p1, p2) -> p1))
                //artifact
                .map(path -> path.resolve(artifact))
                //version-dir
                .flatMap(path -> !snapshot ? Stream.of(
                        path.resolve(version)
                ) : FileUtils.list(path
                        , t -> t.getName(t.getNameCount() - 1).toString().startsWith(version + "-")
                ))
                .filter(FileUtils::isReadableDirectory)
                //version-file
                .flatMap(path -> !snapshot ? Stream.of(
                        path.resolve(artifact + "-" + version + classifier)
                ) : FileUtils.list(path
                        , t -> t.getName(t.getNameCount() - 1).toString().startsWith(artifact + "-" + version + "-")
                        , t -> t.getName(t.getNameCount() - 1).toString().endsWith(classifier)
                ))
                .filter(FileUtils::isReadableFile)
                //get last
                .max(Comparator.comparing(FileUtils::getLastModifiedTime))
                ;
    }
}
