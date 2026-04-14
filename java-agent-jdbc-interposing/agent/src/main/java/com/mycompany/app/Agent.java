package com.mycompany.app;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.jar.JarFile;

public class Agent {
    public static void premain(String args, Instrumentation inst) throws Exception {
        System.out.println("Premain started!");

        // TODO: Make it cleaner and more robust.
        // File jarFile = new
        // File("/home/dvictoriano/Code/cs-analyst-labs/java-agent/agent/target/java-agent-1.0-SNAPSHOT.jar");

        File jarFile = null;
        CodeSource codeSource = Agent.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                jarFile = new File(codeSource.getLocation().toURI());
                System.out.println("✅ " + jarFile.getAbsolutePath());
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Agent JAR Path: " + jarFile.getAbsolutePath());
        } else {
            System.out.println("Could not determine the JAR file path.");
        }

        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(jarFile));
            System.out.println("✅ Successfully added to bootstrap classpath: " + jarFile.getAbsolutePath());

            // inst.appendToBootstrapClassLoaderSearch(new  JarFile("/home/dvictoriano/Code/hacienda/toolbox/libs/ojdbc8.jar"));

        } catch (Exception e) {
            System.out.println("❌ Failed to add to bootstrap: " + e.getMessage());
        }

        System.out.println("Modifiable: " + inst.isModifiableClass(DriverManager.class));
        // inst.addTransformer(new NullPointerTransformer(), true);

        install(inst);

        try {
            // Force transformation for the Java base system classes loaded from 'jrt:/'
            // path
            for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
                // Why I Cant' Modify Lambdas?
                if (loadedClass.getName().startsWith("java.") && inst.isModifiableClass(loadedClass)) {
                    // System.out.println("Retransforming system class: " + loadedClass.getName());
                    inst.retransformClasses(loadedClass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void agentmain(String args, Instrumentation inst) {
        install(inst);
    }

    private static void install(Instrumentation inst) {
        System.out.println("Installing JDBC interposing agent...");

        inst.addTransformer(new JdbcTransformer(), true);

        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (cls.getName().contains("Driver")) {
                try {
                    if (inst.isModifiableClass(cls)) {
                        // System.out.println("♻️ Retransforming: " + cls.getName());
                        inst.retransformClasses(cls);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        // try {
        //     Class<?> driverClass = Class.forName("oracle.jdbc.OracleDriver");
        //     Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        //     DriverManager.registerDriver(new DriverShim(driver));
        // } catch (Exception e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }

        // Start debug server in background thread
        new Thread(() -> {
            try {
                DebugServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "DebugServer").start();
    }
}