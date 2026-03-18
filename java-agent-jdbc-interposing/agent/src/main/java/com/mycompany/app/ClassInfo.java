package com.mycompany.app;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

public class ClassInfo {

    private final Set<String> interfaces = new HashSet<>();

    public static ClassInfo extract(ClassReader cr) {
        ClassInfo info = new ClassInfo();

        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name,
                              String signature, String superName, String[] interfaces) {

                if (interfaces != null) {
                    for (String i : interfaces) {
                        info.interfaces.add(i);
                    }
                }
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return info;
    }

    public boolean implementsInterface(String iface) {
        return interfaces.contains(iface);
    }
}