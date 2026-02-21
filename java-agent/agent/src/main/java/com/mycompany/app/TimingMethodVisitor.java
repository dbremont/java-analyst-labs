package com.mycompany.app;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class TimingMethodVisitor extends AdviceAdapter {
    private final String className;
    private final String methodName;
    private final String descriptor;
    private int startTimeVar = -1;
    
    protected TimingMethodVisitor(MethodVisitor mv, int access, String name, String descriptor, String className) {
        super(Opcodes.ASM9, mv, access, name, descriptor);
        this.className = className;
        this.methodName = name;
        this.descriptor = descriptor;
    }
    
    @Override
    protected void onMethodEnter() {
        // Call TimerHelper.startTimer() and store result in a local variable
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/mycompany/app/TimerHelper", "startTimer", "()J", false);
        startTimeVar = newLocal(Type.LONG_TYPE);
        mv.visitVarInsn(Opcodes.LSTORE, startTimeVar);
    }
    
    @Override
    protected void onMethodExit(int opcode) {
        // Only record if not an ATHROW (exceptions are handled separately)
        if (opcode != ATHROW) {
            recordMeasurement();
        }
    }
    
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // Record measurement before exception handlers
        recordMeasurement();
        super.visitMaxs(maxStack, maxLocals);
    }
    
    private void recordMeasurement() {
        if (startTimeVar == -1) {
            return; // Timing not initialized (e.g., abstract methods)
        }
        
        // Load start time
        mv.visitVarInsn(Opcodes.LLOAD, startTimeVar);
        
        // Load class name (convert to String)
        mv.visitLdcInsn(className);
        
        // Load method name
        mv.visitLdcInsn(methodName);
        
        // Load descriptor
        mv.visitLdcInsn(descriptor);
        
        // Call TimerHelper.endTimerAndRecord(long, String, String, String)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/mycompany/app/TimerHelper", "endTimerAndRecord", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
    }
}