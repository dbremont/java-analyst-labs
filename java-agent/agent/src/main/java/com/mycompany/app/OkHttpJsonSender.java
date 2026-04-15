package com.mycompany.app;

import okhttp3.*;

public class OkHttpJsonSender {
    public static void send(String json, String url) throws Exception {
        OkHttpClient client = new OkHttpClient();
        
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // System.out.println("Response: " + response.body().string());
        }
    }
}
