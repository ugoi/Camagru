package com.camagru.request_handlers;

import java.io.IOException;
import java.util.UUID;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class VideoInitiateUploadRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Request req = new Request(exchange);
        Response res = new Response(exchange);

        switch (req.getMethod()) {
            case "POST":
                handlePostRequest(req, res);
                break;
            case "OPTIONS":
                handleOptionsRequest(req, res);
                break;
            default:
                handleDefaultRequest(req, res);
                break;
        }
    }

    private void handleDefaultRequest(Request req, Response res) {
        res.sendJsonResponse(405, createErrorResponse("Unsupported method"));
    }

    private void handleOptionsRequest(Request req, Response res) {
        res.sendOptionsResponse(res);
    }

    private void handlePostRequest(Request req, Response res) {
        try {
            String uploadId = UUID.randomUUID().toString();

            JSONObject jsonResponse = new JSONObject()
                    .put("message", "Request handled successfully")
                    .put("uploadId", uploadId);
            res.sendJsonResponse(200, jsonResponse.toString());
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }
}
