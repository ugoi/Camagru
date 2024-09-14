package com.camagru.request_handlers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.services.MediaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ServeMediaRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Request req = new Request(exchange);
        Response res = new Response(exchange);

        switch (req.getMethod()) {
            case "GET":
                handleGetRequest(req, res);
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
        System.out.println("Getting options response");
        res.sendOptionsResponse(res);

    }

    private void handleGetRequest(Request req, Response res) {
        try {
            System.out.println("Serving media");
            // No authorization needed

            // Validate input
            List<String> wrongFields = new PropertyFieldsManager(
                    Arrays.asList(new PropertyField("id", true)), null)
                    .validationResult(req);

            if (!wrongFields.isEmpty()) {
                res.sendJsonResponse(400,
                        createErrorResponse("The following fields are invalid: " + String.join(", ", wrongFields)));
                return;
            }
            String contentId = req.getQueryParameter("id");
            byte[] videoFile = MediaService.getMedia(contentId);
            res.sendResponse(200, videoFile);
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }
}
