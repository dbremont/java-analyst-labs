package com.mycompany.app;

import java.io.File;
import java.util.jar.JarFile;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {

        System.out.println("Premain started!");

        //TODO: Make it cleaner and more robust.
        // File jarFile = new File("/home/dvictoriano/Code/cs-analyst-labs/java-agent/agent/target/java-agent-1.0-SNAPSHOT.jar");
    
        File jarFile = null;
        CodeSource codeSource = Agent.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                jarFile = new File(codeSource.getLocation().toURI());
                System.out.println( "✅ " + jarFile.getAbsolutePath());
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
        } catch (Exception e) {
            System.out.println("❌ Failed to add to bootstrap: " + e.getMessage());
        }

        inst.addTransformer(new NullPointerTransformer(), true);
        inst.addTransformer(new TimingTransformer(), true);

        // Initialize the timing helper
        try {
            com.mycompany.app.TimerHelper.init();
            System.out.println("✅ TimerHelper initialized");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize TimerHelper: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // Force transformation for the Java base system classes loaded from 'jrt:/' path
            for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
                // Why I Cant' Modify Lambdas?
                if (loadedClass.getName().startsWith("java.") && inst.isModifiableClass(loadedClass)) {
                    System.out.println("Retransforming system class: " + loadedClass.getName());
                    inst.retransformClasses(loadedClass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}