import de.flapdoodle.embed.mongo.Command
import de.flapdoodle.embed.mongo.Paths
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.store.FileSet
import de.flapdoodle.embed.process.config.store.IPackageResolver
import de.flapdoodle.embed.process.distribution.*
import de.flapdoodle.embed.process.store.Downloader
import org.apache.commons.io.FileUtils

def versionRaw = properties['mongo.version'] as String
def rootRaw = properties['bundle.root'] as String
println "Requested MongoDb distribution for version $versionRaw into $rootRaw"

def versionInt = "V" + versionRaw.replace('.', '_')
IVersion version
try {
    version = Version.Main.valueOf(versionInt)
} catch (IllegalArgumentException ignored) {
    version = Version.valueOf(versionInt)
}

def root = new File(rootRaw, "mongo")

def baseResolver = new Paths(Command.MongoD)
def subResolvers = Command.values().collect {new Paths(it)}
def downloadConfig = new DownloadConfigBuilder()
        .defaults()
        .packageResolver(new IPackageResolver() {
    @Override
    FileSet getFileSet(Distribution distribution) {
        def builder = FileSet.builder()
        for (sub in subResolvers) {
            for (entry in sub.getFileSet(distribution).entries()) {
                builder.addEntry(entry.type(), entry.destination(), entry.matchingPattern())
            }
        }
        return builder.build()
    }

    @Override
    ArchiveType getArchiveType(Distribution distribution) {
        return baseResolver.getArchiveType(distribution)
    }

    @Override
    String getPath(Distribution distribution) {
        return baseResolver.getPath(distribution)
    }
})
        .build()

def downloader = new Downloader()
for (platform in Platform.values()) {
    for (bitSize in BitSize.values()) {
        def distribution = new Distribution(version, platform, bitSize)
        def archiveType = downloadConfig.getPackageResolver().getArchiveType(distribution)
        def name = "${platform}-${version}-${bitSize}.${archiveType}"
        def target = new File(root, name)
        if (target.exists() && target.isFile()) {
            println "Download $distribution: already exists"
            continue
        }
        try {
            def url = downloader.getDownloadUrl(downloadConfig, distribution)
            println "Download $distribution: start download from '$url' into '/mongo/$name'"
            def file = downloader.download(downloadConfig, distribution)
            FileUtils.moveFile(file, target)
        } catch (IllegalArgumentException | IOException e) {
            println "Download $distribution: ${e.getMessage()}"
        }
    }
}
