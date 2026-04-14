package com.mycompany.app;

import java.util.*;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionRegistry {

    private static final Map<Integer, Connection> connections = new ConcurrentHashMap<>();

    // import java.lang.StackWalker;

    // StackWalker walker = StackWalker.getInstance();

    // walker.forEach(frame ->
    //     System.out.println(
    //         frame.getClassName() + "." +
    //         frame.getMethodName() + ":" +
    //         frame.getLineNumber()
    //     )
    // );

    public static void register(Connection conn) {
        if (conn == null) return;

        int id = System.identityHashCode(conn);
        connections.put(id, conn);

        System.out.println("📡 Registered connection: " + conn);
    }

    public static Object get(int id) {
        // This is a bit hacky, but we don't have the actual Connection objects here,
        // only their string representations. In a real implementation, we'd want to
        // store the actual Connection objects, but that would require more complex
        // bytecode manipulation to ensure we can clean them up properly.
        
        Object conn = connections.get(id);
        if (conn == null) return null;

        // We can't reconstruct the Connection object from its string representation,
        // so we'll just return null here. In a real implementation, we'd want to
        // store the actual Connection objects in the registry.
        return conn;
    }

    public static Map<Integer, Connection> list() {
        return connections;
    }
}