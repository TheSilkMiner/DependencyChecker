package openmods.depcheck;

import openmods.depcheck.ModInfo.ModRegistrationContext;
import openmods.depcheck.utils.ElementType;

import org.objectweb.asm.*;

import com.google.common.collect.ImmutableSet;

public class SourceClassBytecodeVisitor extends ClassVisitor {
    private final ModRegistrationContext context;

    private String className;

    public SourceClassBytecodeVisitor(ModRegistrationContext context) {
        super(Opcodes.ASM5);
        this.context = context;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        final String dotName = name.replace('/', '.');
        final String dotSuperName = superName.replace('/', '.');

        ImmutableSet.Builder<String> dotInterfaces = ImmutableSet.builder();
        for (String intf : interfaces)
            dotInterfaces.add(intf.replace('/', '.'));

        context.registerClass(dotName, dotSuperName, dotInterfaces.build());
        className = dotName;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        context.registerElement(className, ElementType.FIELD, name, desc);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        context.registerElement(className, ElementType.METHOD, name, desc);
        return null;
    }

}
