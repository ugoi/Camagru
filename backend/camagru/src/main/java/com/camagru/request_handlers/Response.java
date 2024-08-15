package com.camagru.request_handlers;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

public class Response {
    private final HttpExchange exchange;

    public Response(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void setHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }

    public void sendResponse(int statusCode, String responseBody) {
        try {

            // Set Default Headers
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
