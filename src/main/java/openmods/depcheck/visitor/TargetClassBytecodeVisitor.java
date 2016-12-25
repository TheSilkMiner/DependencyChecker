package openmods.depcheck.visitor;

import java.util.function.Consumer;

import openmods.depcheck.parser.TargetParser.TargetClassVisitor;
import openmods.depcheck.utils.ElementType;

import org.objectweb.asm.*;

public class TargetClassBytecodeVisitor extends ClassVisitor {

    private class MethodDependencyVisitor extends MethodVisitor {

        public MethodDependencyVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Type)
                visitType((Type)cst);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            String clsName = type.replace('/', '.');
            visitor.visitRequiredClass(clsName);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            visitType(Type.getType(desc));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            visitor.visitRequiredElement(internalToJava(owner), ElementType.FIELD, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            visitor.visitRequiredElement(internalToJava(owner), ElementType.METHOD, name, desc);
        }

    }

    private final TargetClassVisitor visitor;

    public TargetClassBytecodeVisitor(TargetClassVisitor visitor) {
        super(Opcodes.ASM5);
        this.visitor = visitor;
    }

    private static String internalToJava(String cls) {
        return cls.replace('/', '.');
    }

    private static void extractReferenceType(Type type, Consumer<String> typeConsumer) {
        if (type.getSort() == Type.OBJECT) {
            typeConsumer.accept(type.getClassName());
        } else if (type.getSort() == Type.ARRAY)
            extractReferenceType(type.getElementType(), typeConsumer);
        else if (type.getSort() == Type.METHOD) {
            extractReferenceType(type.getReturnType(), typeConsumer);

            for (Type argType : type.getArgumentTypes())
                extractReferenceType(argType, typeConsumer);
        }
    }

    private void visitType(Type type) {
        extractReferenceType(type, visitor::visitRequiredClass);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (String intf : interfaces)
            visitor.visitRequiredClass(internalToJava(intf));

        visitor.visitRequiredClass(internalToJava(superName));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        visitType(Type.getType(desc));
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        visitType(Type.getMethodType(desc));
        return new MethodDependencyVisitor();
    }

}
