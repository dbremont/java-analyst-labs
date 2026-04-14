package com.mycompany.app;

import org.objectweb.asm.*;

public class AsmUtils {

    public static byte[] transformDriver(byte[] classBytes) {

        ClassReader cr = new ClassReader(classBytes);

        // ⚠️ DO NOT use COMPUTE_FRAMES in agents
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // Exact JDBC Driver.connect match
                if (name.equals("connect") &&
                        desc.equals("(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;")) {

                    System.out.println("✅ Instrumenting Driver.connect: " + desc);

                    return new ConnectionAdviceAdapter(
                            Opcodes.ASM9, mv, access, name, desc);
                }

                return mv;
            }
        };

        // ⚠️ DO NOT expand frames
        cr.accept(cv, ClassReader.SKIP_FRAMES);

        return cw.toByteArray();
    }

    public static byte[] transformDataSource(byte[] classBytes) {

        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(
                cr,
                ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // Match BOTH:
                // getConnection()
                // getConnection(String, String)
                if (name.equals("getConnection") &&
                        desc.endsWith("Ljava/sql/Connection;")) {

                    System.out.println("🎯 Instrumenting DataSource.getConnection: " + desc);

                    return new ConnectionAdviceAdapter(
                            Opcodes.ASM9, mv, access, name, desc);
                }

                return mv;
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}