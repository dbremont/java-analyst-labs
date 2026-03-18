package com.sample.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class App {

    // JDBC connection info
    private static final String URL = "...";
    private static final String USER = "...";
    private static final String PASS = "...";

    public static void main(String[] args) throws Exception {

        // Create simple HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);

        Connection conn = DriverManager.getConnection(URL, USER, PASS);

        server.createContext("/finfo", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange)  {

                ClassLoader original = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(App.class.getClassLoader()); // application classloader

                
                try {

                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT COD_ESTADO, DES_ESTADO FROM EG_DOM_ESTADOS_FU");

                    JsonArray jsonArray = new JsonArray();

                    while (rs.next()) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("cod_estado", rs.getString("COD_ESTADO"));
                        obj.addProperty("des_estado", rs.getString("DES_ESTADO"));
                        jsonArray.add(obj);
                    }

                    String response = new Gson().toJson(jsonArray);

                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        String err = "Internal Server Error";
                        exchange.sendResponseHeaders(500, err.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(err.getBytes());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        System.out.println("Service running at http://localhost:8084/finfo ...");
        server.start();
    }
}