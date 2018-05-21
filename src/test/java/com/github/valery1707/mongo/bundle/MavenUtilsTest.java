package com.github.valery1707.mongo.bundle;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.github.valery1707.mongo.bundle.MavenUtils.ENV;
import static com.github.valery1707.mongo.bundle.MavenUtils.FS;
import static org.assertj.core.api.Assertions.assertThat;

public class MavenUtilsTest {
    private static final String ENV_MAVEN_HOME = "MAVEN_HOME";
    private static final String ENV_USER_HOME = "USER" + "PROFILE";
    private static final String[] ENV_NAMES = new String[]{ENV_MAVEN_HOME, ENV_USER_HOME};

    private final Map<String, String> env = new HashMap<>();

    private Path maven;
    private Path user;

    @Before
    public void setUp() throws IOException {
        MavenUtils.repository.set(null);
        FS = MemoryFileSystemBuilder.newLinux().build();

        maven = Files.createDirectories(FS.getPath("/opt/.m2"));
        user = Files.createDirectories(FS.getPath("/home/test-user"));

        for (String name : ENV_NAMES) {
            env.put(name, System.getenv(name));
        }
        env.put(ENV_MAVEN_HOME, maven.toString());
        env.put(ENV_USER_HOME, user.toString());
        ENV = env::get;
    }

    @After
    public void tearDown() throws Exception {
        FS.close();
    }

    @Test
    public void testRepositoryRoots_notFound() throws IOException {
        env.clear();
        assertThat(MavenUtils.repositoryRoots()).isEmpty();
    }

    @Test
    public void testRepositoryRoots_mavenWithoutConf() throws IOException {
        assertThat(MavenUtils.repositoryRoots()).isEmpty();
    }

    @SuppressWarnings("UnnecessarySemicolon")
    private static void copy(Supplier<InputStream> inputSupplier, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        target = Files.createFile(target);
        try (
                InputStream input = inputSupplier.get();
                OutputStream output = Files.newOutputStream(target);
        ) {
            IOUtils.copy(input, output);
        }
    }

    @Test
    public void testRepositoryRoots_fromConfig_notExists() throws IOException {
        copy(() -> getClass().getResourceAsStream("/xml/xml-2.xml"), maven.resolve("conf").resolve("settings.xml"));
        assertThat(MavenUtils.repositoryRoots()).isEmpty();
    }

    @Test
    public void testRepositoryRoots_fromConfig_exists() throws IOException {
        copy(() -> getClass().getResourceAsStream("/xml/xml-2.xml"), maven.resolve("conf").resolve("settings.xml"));
        Path repo = Files.createDirectories(FS.getPath("/path/to/local/repo"));
        assertThat(MavenUtils.repositoryRoots()).hasSize(1).containsOnly(repo);
        //cache
        assertThat(MavenUtils.repositoryRoots()).isSameAs(MavenUtils.repositoryRoots());
    }

    @Test
    public void testRepositoryRoots_userHome() throws IOException {
        String oldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", user.toString());
            Path repo = Files.createDirectories(user.resolve(".m2").resolve("repository"));
            assertThat(MavenUtils.repositoryRoots()).hasSize(1).containsOnly(repo);
            //cache
            assertThat(MavenUtils.repositoryRoots()).isSameAs(MavenUtils.repositoryRoots());
        } finally {
            System.setProperty("user.home", oldHome);
        }
    }
}
