package openmods.depcheck;

import java.util.Optional;

import openmods.depcheck.TargetParser.TargetClassVisitor;
import openmods.depcheck.utils.Field;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

public class TargetClassBytecodeVisitor extends ClassVisitor {

    private class MethodDependencyVisitor extends MethodVisitor {

        public MethodDependencyVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            String clsName = type.replace('/', '.');
            visitor.visitRequiredClass(clsName);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            addClsIfNeeded(Type.getType(desc));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            visitor.visitRequiredField(owner.replace('/', '.'), new Field(name, desc));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            visitor.visitRequiredMethod(owner.replace('/', '.'), new Method(name, desc));
        }

    }

    private final TargetClassVisitor visitor;

    public TargetClassBytecodeVisitor(TargetClassVisitor visitor) {
        super(Opcodes.ASM5);
        this.visitor = visitor;
    }

    private static Optional<String> extractReferenceType(Type type) {
        if (type.getSort() == Type.OBJECT) {
            return Optional.of(type.getClassName());
        } else if (type.getSort() == Type.ARRAY)
            return extractReferenceType(type.getElementType());
        else {
            return Optional.empty();
        }
    }

    private void addClsIfNeeded(final Type type) {
        final Optional<String> refType = extractReferenceType(type);
        refType.ifPresent(visitor::visitRequiredClass);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (String intf : interfaces)
            visitor.visitRequiredClass(intf.replace('/', '.'));

        visitor.visitRequiredClass(superName.replace('/', '.'));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        addClsIfNeeded(Type.getType(desc));
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final Type type = Type.getMethodType(desc);
        addClsIfNeeded(type.getReturnType());

        for (Type argType : type.getArgumentTypes())
            addClsIfNeeded(argType);

        return new MethodDependencyVisitor();
    }

}
