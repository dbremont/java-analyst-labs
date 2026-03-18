package com.mycompany.app;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class DebugServer {

    public static void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(9090)) {

                System.out.println("✅ HTTP debug server on http://localhost:9090");

                while (true) {
                    try (Socket socket = server.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));
                            OutputStream out = socket.getOutputStream()) {

                        // Read first HTTP line (e.g. GET /connections HTTP/1.1)
                        String requestLine = in.readLine();
                        if (requestLine == null)
                            continue;

                        String path = requestLine.split(" ")[1];

                        String body;
                        int status = 200;
                        String contentType = "application/json";

                        if (path.startsWith("/connections")) {
                            body = toJson(ConnectionRegistry.list());

                        } else if (path.startsWith("/sqlclient")) {
                            body = sqlClientHtml();
                            contentType = "text/html";

                        } else if ("/health".equals(path)) {
                            body = "{\"status\":\"ok\"}";

                        } else if (path.startsWith("/query")) {
                            body = handleQuery(path);

                        } else {
                            status = 404;
                            body = "{\"error\":\"not found\"}";
                        }

                        writeResponse(out, status, body, contentType);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void writeResponse(OutputStream out, int status, String body, String contentType)
            throws IOException {

        if (body == null)
            body = "";

        String statusText = (status == 200) ? "OK" : "Not Found";

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        String response = "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private static String toJson(Map<Integer, ?> map) {
        StringBuilder sb = new StringBuilder("{");

        boolean first = true;
        for (Map.Entry<Integer, ?> e : map.entrySet()) {
            if (!first)
                sb.append(",");
            first = false;

            sb.append("\"")
                    .append(e.getKey())
                    .append("\":\"")
                    .append(escape(e.getValue().toString()))
                    .append("\"");
        }

        return sb.append("}").toString();
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String handleQuery(String path) {

        return QueryService.handleQuery(path);        
    }


    private static void sqlClientHtml(OutputStream out, String html) {
        try {
            out.write(html.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sqlClientHtml() {
        try (
            InputStream is = ClassLoader.getSystemResourceAsStream("sqlclient.html")
        ) {

            if (is == null) {
                return "<h1>404 - sqlclient.html not found</h1>";
            }

            return readAll(is);

        } catch (Exception e) {
            return "<h1>Error loading sqlclient.html</h1><pre>" + e.getMessage() + "</pre>";
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[8192];
        int n;

        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }

        return buffer.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }

}