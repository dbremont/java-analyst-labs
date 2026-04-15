package com.mycompany.app;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;

public class TimingTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, 
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        
        // Skip system classes to avoid issues
        if (className == null || 
            className.startsWith("java/") || 
            className.startsWith("javax/") || 
            className.startsWith("sun/") ||
            className.startsWith("jdk/") ||
            className.startsWith("com/sun/")) {
            return null;
        }

        // Skip our own agent classes to avoid infinite recursion
        if (className.startsWith("com/mycompany/app/")) {
            return null;
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor visitor = new TimingClassVisitor(writer, className);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();

        } catch (Exception e) {
            System.err.println("Error transforming class " + className + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}