package openmods.depcheck;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetParser {

    private static final Logger logger = LoggerFactory.getLogger(TargetParser.class);

    public interface TargetClassVisitor {
        public void visitRequiredClass(String cls);

        public void visitRequiredField(String cls, String name, String desc);

        public void visitRequiredMethod(String cls, String name, String desc);
    }

    public interface TargetModContentsVisitor {
        public TargetClassVisitor visitClass(String cls);
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
