package openmods.depcheck;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import openmods.depcheck.ModInfo.ModRegistrationContext;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;

public class SourceParser {

    private static final Logger logger = LoggerFactory.getLogger(SourceParser.class);

    private static final Gson GSON = new GsonBuilder().create();

    public static class VersionPattern {
        public String pattern;

        public int versionGroup;

        public transient Pattern cache;

        public Optional<String> getVersion(String input) {
            if (cache == null)
                cache = Pattern.compile(pattern);

            final Matcher m = cache.matcher(input);

            if (m.matches()) {
                final String version = m.group(versionGroup);
                return Optional.of(version);
            }

            return Optional.empty();
        }
    }

    public static class ModInfoMeta {
        public String mod;

        @SerializedName("package")
        public String pkgPrefix;

        List<VersionPattern> patterns;

    }

    private final File topDir;

    public SourceParser(File topDir) {
        this.topDir = topDir;
    }

    private static SourceDependencies getOrCreateSourceDependencies(File cache) {
        if (cache.isFile()) {
            try {
                try (FileInputStream input = new FileInputStream(cache);
                        FSTObjectInput os = new FSTObjectInput(input)) {
                    return (SourceDependencies)os.readObject();
                }
            } catch (Throwable t) {
                logger.error("Failed to load source cache from " + cache.getAbsolutePath(), t);
            }
        }

        return new SourceDependencies();
    }

    private static void storeCache(File cache, SourceDependencies deps) {
        try {
            try (FileOutputStream output = new FileOutputStream(cache);
                    FSTObjectOutput os = new FSTObjectOutput(output)) {
                os.writeObject(deps);
            }
        } catch (Throwable t) {
            logger.error("Failed to store source cache to " + cache.getAbsolutePath(), t);
        }
    }

    public SourceDependencies collectAvailableDependencies() {
        final File modsDir = new File(topDir, "mods");
        Preconditions.checkState(modsDir.isDirectory(), "%s is not directory", modsDir.getAbsolutePath());

        final File cache = new File(topDir, "cache.ser");

        final SourceDependencies result = getOrCreateSourceDependencies(cache);

        for (File f : modsDir.listFiles()) {
            if (f.isDirectory())
                try {
                    scanModDir(result, f);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Failed to process directory %s", f.getAbsolutePath()), e);
                }
        }

        storeCache(cache, result);
        return result;
    }

    private static void scanModDir(SourceDependencies result, File modDir) throws IOException {
        logger.info("Scanning source mod directory {}", modDir.getAbsolutePath());
        final File metaFile = new File(modDir, "meta.json");
        try (InputStream input = new FileInputStream(metaFile);
                Reader reader = new InputStreamReader(input)) {
            final ModInfoMeta meta = GSON.fromJson(reader, ModInfoMeta.class);
            final ModInfo mod = result.addMod(meta.pkgPrefix, meta.mod);

            for (File f : modDir.listFiles((f, name) -> new File(f, name).isFile() && name.endsWith(".jar")))
                scanJarFile(meta, mod, f);

        }
    }

    private static void scanJarFile(ModInfoMeta meta, ModInfo mod, File jarFile) throws IOException {
        logger.info("Scanning source mod jar file {}", jarFile.getAbsolutePath());
        final String jarFileName = jarFile.getName();
        final Optional<String> maybeVersion = meta.patterns.stream()
                .map(pattern -> pattern.getVersion(jarFileName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        final String version = maybeVersion.orElseThrow(() -> new IllegalStateException("File " + jarFile.getAbsolutePath() + " can't be matched"));

        if (mod.hasVersion(version)) {
            logger.info("Version {} already found in cache", version);
        } else {
            final ModRegistrationContext modVersion = mod.registerVersion(version);

            try (ZipFile zipFile = new ZipFile(jarFile)) {
                final Enumeration<? extends ZipEntry> e = zipFile.entries();
                while (e.hasMoreElements()) {
                    final ZipEntry entry = e.nextElement();
                    if (entry.isDirectory())
                        continue;

                    final String name = entry.getName();
                    if (name.endsWith(".class") && mod.matchPackage(name.replace('/', '.'))) {
                        logger.trace("Scanning class file {}", name);
                        try (InputStream zipFileStream = zipFile.getInputStream(entry)) {
                            scanClassFile(modVersion, zipFileStream);
                        }
                    }
                }
            }
        }

    }

    private static void scanClassFile(ModRegistrationContext mod, InputStream is) throws IOException {
        final ClassReader reader = new ClassReader(is);

        final SourceClassBytecodeVisitor cv = new SourceClassBytecodeVisitor(mod);
        reader.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

}
