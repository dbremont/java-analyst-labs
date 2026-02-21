package com.mycompany.app;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TimingClassVisitor extends ClassVisitor {
    private final String className;
    
    public TimingClassVisitor(ClassWriter writer, String className) {
        super(Opcodes.ASM9, writer);
        this.className = className;
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        String fullMethod = className.replace('/', '.') + "." + name;
        String lower = fullMethod.toLowerCase();

        // Skip native and abstract methods
        if ((access & Opcodes.ACC_NATIVE) != 0 || (access & Opcodes.ACC_ABSTRACT) != 0) {
            return mv;
        }
        
        // Skip class initialization methods to avoid recursion issues
        if (name.equals("<clinit>")) {
            return mv;
        }

        // Skip infrastructure / logging / utilities
        if (lower.contains("org.springframework.util") ||
            lower.contains("org.springframework.beans") ||
            lower.contains("org.springframework.context") ||
            lower.contains("org.springframework.boot.context.properties.source") ||
            lower.contains("org.springframework.boot.io") ||
            lower.contains("org.springframework.boot.devtools") ||
            lower.contains("org.springframework.aop") ||
            lower.contains("org.springframework.core") ||
            lower.contains("antlr") ||


            lower.contains("org.slf4j") ||
            lower.contains("log") ||
            lower.contains("debug") ||
            lower.contains("jdk") ||
            lower.contains("com.sun") ||
            // lower.startsWith("ch.qos.logback") ||
            // lower.startsWith("java.") ||
            // lower.startsWith("javax.") ||
            // lower.startsWith("jakarta.") ||
            name.contains("<clinit>")) {
            return mv; // skip instrumentation
        }

        // Create timing visitor for this method
        return new TimingMethodVisitor(mv, access, name, descriptor, className);
    }
}