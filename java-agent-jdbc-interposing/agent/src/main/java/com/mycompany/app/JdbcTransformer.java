package com.mycompany.app;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

public class JdbcTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (className == null) return null;

        try {
            ClassReader cr = new ClassReader(classfileBuffer);

            ClassInfo info = ClassInfo.extract(cr);

            if (info.implementsInterface("java/sql/Driver")) {
                System.out.println("✅ Driver impl: " + className);
                return AsmUtils.transformDriver(classfileBuffer);
            }

            if (info.implementsInterface("javax/sql/DataSource")) {
                System.out.println("✅ DataSource impl: " + className);
                return AsmUtils.transformDataSource(classfileBuffer);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }
}