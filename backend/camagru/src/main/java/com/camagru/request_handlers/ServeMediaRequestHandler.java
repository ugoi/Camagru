package com.camagru.request_handlers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.services.MediaService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ServeMediaRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Content upload request received");

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
        res.sendOptionsResponse(res);

    }

    private void handleGetRequest(Request req, Response res) {
        try {
            // Extract jwt from cookie
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());

            String jwt = "";
            try {
                jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
                jwtManager.verifySignature(jwt);
            } catch (Exception e) {
                res.sendJsonResponse(401, createErrorResponse("Unauthorized"));
                return;
            }
            String sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");

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
            MediaService.hasPermission(sub, contentId);
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
