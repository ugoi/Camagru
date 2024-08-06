package com.camagru;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response;
        int statusCode = 200;

        switch (exchange.getRequestMethod()) {
            case "GET":
                response = handleGetRequest(exchange);
                break;
            case "POST":
                response = handlePostRequest(exchange);
                break;
            case "PUT":
                response = handlePutRequest(exchange);
                break;
            case "DELETE":
                response = handleDeleteRequest(exchange);
                break;
            default:
                response = "Unsupported method";
                statusCode = 405;
                break;
        }

        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String handleGetRequest(HttpExchange exchange) {
        // Example of processing query parameters
        String query = exchange.getRequestURI().getQuery();
        return "Handled GET request with query: " + (query == null ? "none" : query);
    }

    private String handlePostRequest(HttpExchange exchange) throws IOException {
        // Example of reading request body
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return "Handled POST request with body: " + sb.toString();
    }

    private String handlePutRequest(HttpExchange exchange) throws IOException {
        // Example of reading request body
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return "Handled PUT request with body: " + sb.toString();
    }

    private String handleDeleteRequest(HttpExchange exchange) {
        return "Handled DELETE request";
    }
}
