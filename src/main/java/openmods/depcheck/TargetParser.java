package openmods.depcheck;

import java.io.*;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import openmods.depcheck.utils.ElementType;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetParser {

    private static final String REFLECTION_LOG_PREFIX = "###";
    private static final Logger logger = LoggerFactory.getLogger(TargetParser.class);

    public interface TargetClassVisitor {
        public void visitRequiredClass(String cls);

        public void visitRequiredElement(String cls, ElementType type, String name, String desc);
    }

    public interface TargetModContentsVisitor {
        public TargetClassVisitor visitClass(String cls);

        public Optional<TargetClassVisitor> visitClassIfExists(String cls);
    }

    public interface TargetModVisitor {
        public TargetModContentsVisitor visitFile(File file);
    }

    private final File targetsDir;

    public TargetParser(File topDir) {
        this.targetsDir = new File(topDir, "targets");
    }

    public void accept(TargetModVisitor visitor) {
        for (File f : targetsDir.listFiles((f, name) -> new File(f, name).isFile() && name.endsWith(".jar"))) {
            logger.info("Scanning target mod jar file {}", f.getAbsolutePath());
            final TargetModContentsVisitor fileVisitor = visitor.visitFile(f);
            try {
                acceptFile(f, fileVisitor);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to process target file %s", f.getAbsolutePath()), e);
            }

            final File dynamicDeps = new File(f.getParentFile(), f.getName() + ".dynamic");
            if (dynamicDeps.isFile()) {
                try {
                    acceptDynamicDeps(dynamicDeps, fileVisitor);
                } catch (IOException e) {
                    throw new RuntimeException(String.format("Failed to process dynamic dependencies file %s", dynamicDeps.getAbsolutePath()), e);
                }
            }
        }
    }

    private static void acceptDynamicDeps(File dynamicDeps, TargetModContentsVisitor fileVisitor) throws IOException {
        try (FileInputStream input = new FileInputStream(dynamicDeps);
                Reader reader = new InputStreamReader(input);
                BufferedReader lineReader = new BufferedReader(reader)) {
            lineReader.lines().forEach(line -> parseDynamicDependency(line, fileVisitor));
        }
    }

    private static void parseDynamicDependency(String line, TargetModContentsVisitor fileVisitor) {
        final int separatorIndex = line.indexOf(REFLECTION_LOG_PREFIX);
        if (separatorIndex >= 0) {
            final String[] fields = line.substring(separatorIndex + REFLECTION_LOG_PREFIX.length()).split("\\s+");
            if (fields.length < 2)
                logger.warn("Malformed line: {}", line);
            final String type = fields[0];
            final String caller = fields[1];

            fileVisitor.visitClassIfExists(caller).ifPresent(visitor -> {
                switch (type) {
                    case "C":
                        if (fields.length == 3) {
                            // TYPE CALLER CALLEE
                            visitor.visitRequiredClass(fields[2]);
                        } else {
                            logger.warn("Malformed class entry: {}", line);
                        }
                        break;
                    case "F":
                        if (fields.length == 5) {
                            // TYPE CALLER CALLEE NAME DESC
                            visitor.visitRequiredElement(fields[2], ElementType.FIELD, fields[3], fields[4]);
                        } else {
                            logger.warn("Malformed field entry: {}", line);
                        }
                        break;
                    case "M":
                        if (fields.length == 5) {
                            // TYPE CALLER CALLEE NAME DESC
                            visitor.visitRequiredElement(fields[2], ElementType.METHOD, fields[3], fields[4]);
                        } else {
                            logger.warn("Malformed method entry: {}", line);
                        }
                        break;
                    case "I":
                        if (fields.length == 4) {
                            // TYPE CALLER CALLEE DESC
                            visitor.visitRequiredElement(fields[2], ElementType.METHOD, "<init>", fields[3]);
                        } else {
                            logger.warn("Malformed constructor entry: {}", line);
                        }
                        break;
                    default:
                        logger.warn("Malformed line: {}", line);
                        break;
                }
            });
        }
    }

    private static void acceptFile(File jarFile, TargetModContentsVisitor fileVisitor) throws IOException {
        try (ZipFile zipFile = new ZipFile(jarFile)) {
            final Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                final ZipEntry entry = e.nextElement();
                if (entry.isDirectory())
                    continue;

                final String name = entry.getName();
                if (name.endsWith(".class")) {
                    logger.trace("Scanning class file {}", name);
                    final String clsName = name.replace('/', '.').substring(0, name.length() - ".class".length());
                    final TargetClassVisitor classVisitor = fileVisitor.visitClass(clsName);
                    try (InputStream zipFileStream = zipFile.getInputStream(entry)) {
                        scanClassFile(classVisitor, zipFileStream);
                    }
                }
            }
        }
    }

    private static void scanClassFile(TargetClassVisitor classVisitor, InputStream is) throws IOException {
        final ClassReader reader = new ClassReader(is);

        final TargetClassBytecodeVisitor cv = new TargetClassBytecodeVisitor(classVisitor);
        reader.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
}
