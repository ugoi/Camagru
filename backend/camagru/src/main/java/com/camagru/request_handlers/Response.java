package com.camagru.request_handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.tika.Tika;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

public class Response {
    private final HttpExchange exchange;

    public Response(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void sendOptionsResponse(Response res) {
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT, PATCH");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, *");
        res.sendResponse(204, null);
    }

    public void setHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }

    public void sendJsonResponse(int statusCode, String responseBody) {
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
        } catch (IOException e) {
            // Handle case where client disconnects
            System.err.println("Client disconnected while sending JSON response: " + e.getMessage());
        } catch (Exception e) {
            // Log unexpected errors
            System.err.println("Error while sending JSON response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendResponse(int statusCode, Object responseBody) {
        try {
            byte[] responseBytes;
            String contentType;

            if (responseBody instanceof String) {
                // Handle String response
                contentType = "text/plain";
                responseBytes = ((String) responseBody).getBytes(StandardCharsets.UTF_8);
            } else if (responseBody instanceof byte[]) {
                // Handle binary data (e.g., images, files)
                Tika tika = new Tika();
                try (ByteArrayInputStream input = new ByteArrayInputStream((byte[]) responseBody)) {
                    contentType = tika.detect(input);
                }
                responseBytes = (byte[]) responseBody;
            } else if (responseBody instanceof JSONObject) {
                // Handle JSONObject response
                contentType = "application/json";
                responseBytes = ((JSONObject) responseBody).toString().getBytes(StandardCharsets.UTF_8);
            } else if (responseBody == null) {
                // Handle null response, don't send any body, Content type should be none
                contentType = null;
                responseBytes = null;
            } else {
                // Fallback for other types
                contentType = "text/plain";
                responseBytes = responseBody.toString().getBytes(StandardCharsets.UTF_8);
            }

            // Set Headers
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            if (responseBytes == null) {
                // No content to send
                exchange.sendResponseHeaders(statusCode, -1);
            } else {
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        } catch (IOException e) {
            // Handle specific IOException (client disconnection or I/O issues)
            System.err.println("Client disconnected or I/O error: " + e.getMessage());
        } catch (Exception e) {
            // Log other exceptions
            System.err.println("Error while sending response: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
