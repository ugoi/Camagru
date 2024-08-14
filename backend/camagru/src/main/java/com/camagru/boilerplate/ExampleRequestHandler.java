package com.camagru.boilerplate;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

public class ExampleRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpResponse response;

        switch (exchange.getRequestMethod()) {
            case "POST":
                response = handlePostRequest(exchange);
                break;
            case "OPTIONS":
                response = handleOptionsRequest(exchange);
                break;
            default:
                response = new SimpleHttpResponse(createErrorResponse("Unsupported method"), 405);
                break;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(response.statusCode, response.responseBody.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.responseBody.getBytes());
        os.close();
    }

    private SimpleHttpResponse handleOptionsRequest(HttpExchange exchange) {
        return new SimpleHttpResponse("", 204);
    }

    private SimpleHttpResponse handlePostRequest(HttpExchange exchange) {
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JSONObject jsonBody = new JSONObject(requestBody);
            // Handle JSON body here
            
            JSONObject jsonResponse = new JSONObject().put("message", "Request handled successfully");
            return new SimpleHttpResponse(jsonResponse.toString(), 200);
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            return new SimpleHttpResponse(createErrorResponse(errorMessage), 500);
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    private static class SimpleHttpResponse {
        public final String responseBody;
        public final int statusCode;

        public SimpleHttpResponse(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }
    }
}
