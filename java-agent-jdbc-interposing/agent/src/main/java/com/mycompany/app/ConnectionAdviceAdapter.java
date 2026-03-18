package com.mycompany.app;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

public class ConnectionAdviceAdapter extends AdviceAdapter {

    protected ConnectionAdviceAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
        super(api, mv, access, name, desc);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode == ARETURN) {

            // Stack: [connection]

            dup(); // [connection, connection]

            // Call register (must tolerate null internally)
            visitMethodInsn(INVOKESTATIC,
                    "com/mycompany/app/ConnectionRegistry",
                    "register",
                    "(Ljava/sql/Connection;)V",
                    false);

            // Stack still: [connection]
        }
    }
}