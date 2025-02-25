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
        try {
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT, PATCH");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type, *");
            res.sendResponse(204, null);
        } catch (Exception e) {
            System.err.println("Error while sending OPTIONS response: " + e.getMessage());
            e.printStackTrace();
        } finally {
            exchange.close();
        }
    }

    public void setHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }

    public void sendJsonResponse(int statusCode, String responseBody) {
        try {
            // Set default headers for JSON
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://camagru.com");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);

            // Writing the response body
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IOException e) {
            // Handle client disconnection during the response
            System.err.println("Client disconnected while sending JSON response: " + e.getMessage());
        } catch (Exception e) {
            // Handle other unexpected errors
            System.err.println("Error while sending JSON response: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure the exchange is closed, freeing resources
            exchange.close();
        }
    }

    public void sendResponse(int statusCode, Object responseBody) {
        try {
            byte[] responseBytes = null;
            String contentType = null;

            if (responseBody instanceof String) {
                // Handle string response
                contentType = "text/plain";
                responseBytes = ((String) responseBody).getBytes(StandardCharsets.UTF_8);
            } else if (responseBody instanceof byte[]) {
                // Handle binary data response
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
                // Handle no content response
                contentType = null;
            } else {
                // Fallback for other types
                contentType = "text/plain";
                responseBytes = responseBody.toString().getBytes(StandardCharsets.UTF_8);
            }

            // Set response headers
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://camagru.com");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");

            if (responseBytes == null) {
                // No content to send
                exchange.sendResponseHeaders(statusCode, -1);
            } else {
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                // Writing the response body
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        } catch (IOException e) {
            // Handle client disconnection or I/O issues
            System.err.println("Client disconnected or I/O error: " + e.getMessage());
        } catch (Exception e) {
            // Handle other unexpected errors
            System.err.println("Error while sending response: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure the exchange is closed, freeing resources
            exchange.close();
        }
    }
}
