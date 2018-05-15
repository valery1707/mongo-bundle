Bundle of MongoDB binaries packaged as maven library.

Packages deployed into [JitPack](https://jitpack.io/). Based on [Flapdoodle's Embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo)

For add bundle into project you need:

1. Add the JitPack repository to your build file
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```
1. Add the dependency
```xml
    <dependency>
        <groupId>com.github.valery1707</groupId>
        <artifactId>mongo-bundle</artifactId>
        <version>Tag</version>
    </dependency>
```

Source of supported versions: [Version.java](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/blob/de.flapdoodle.embed.mongo-2.0.3/src/main/java/de/flapdoodle/embed/mongo/distribution/Version.java).

Deployed versions can be found in [branches list](https://github.com/valery1707/mongo-bundle/branches).
