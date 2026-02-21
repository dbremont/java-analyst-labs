package com.mycompany.app;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class NullPointerMethodVisitor extends MethodVisitor {
    public NullPointerMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {
            // mv.visitLdcInsn("com.mycompany.app.Logger");  // Load class name
            mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this'

            // mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com.mycompany.app.Logger", "logException", "(Ljava/lang/Throwable;)V", false); // This did not work; why; becase we in the bytecode the naming format is (/)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/mycompany/app/Logger", "logException", "(Ljava/lang/Throwable;)V", false);
        }
        super.visitInsn(opcode);
    }
}
