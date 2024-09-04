package com.camagru.request_handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class VideoUploadPartRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Video upload request received");

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
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.sendJsonResponse(204, ""); // No content
    }

    private void handlePostRequest(Request req, Response res) {
        try {

            // Extract jwt from cookie
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
            jwtManager.verifySignature(jwt);
            String sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");
            // Validate input
            List<String> wrongFields = new PropertyFieldsManager(
                    Arrays.asList(new PropertyField("uploadId", true),
                            new PropertyField("partNumber", true)),
                    null)
                    .validationResult(req);

            if (!wrongFields.isEmpty()) {
                res.sendJsonResponse(400,
                        createErrorResponse("The following fields are invalid: " + String.join(", ", wrongFields)));
                return;
            }
            // Generate uuid for video
            String uploadId = req.getQueryParameter("uploadId");
            String partNumber = req.getQueryParameter("partNumber");

            String videoFileName = sub + "_" + uploadId.toString() + "_" + partNumber + ".mp4";

            HashMap<String, byte[]> files = req.files();
            byte[] videoFile = files.get("media");

            // Save video file to disk
            OutputStream out = new FileOutputStream(new File("uploads/temp/" + videoFileName));
            out.write(videoFile);
            out.close();

            // Save video file to disk

            JSONObject jsonResponse = new JSONObject()
                    .put("uploadId", uploadId)
                    .put("message", "Video uploaded successfully")
                    .put("status", "processing")
                    .put("downloadUrl",
                            "http://127.0.0.1:8000/api/videos/download-chunk?uploadId=" + uploadId + "&partNumber="
                                    + partNumber)
                    .put("completeUrl", "http://127.0.0.1:8000/api/videos/complete?uploadId=" + uploadId);
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
