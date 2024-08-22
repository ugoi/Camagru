package com.camagru.request_handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
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
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.sendJsonResponse(204, ""); // No content
    }

    private void handleGetRequest(Request req, Response res) {
        try {

            // Extract jwt from cookie
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
            jwtManager.verifySignature(jwt);
            JSONObject decodedJwt = jwtManager.decodeToken(jwt);
            String sub = decodedJwt.getJSONObject("payload").getString("sub");

            String contentId = req.getQueryParameter("contentId");

            // Find out all log files
            String targetDirectory = "uploads/media/";
            File dir = new File(targetDirectory);
            FilenameFilter uploadIdFileFilter = (d, s) -> {
                String[] parts = s.split("_|\\.");

                return parts[1].equals(contentId);
            };
            String[] fileNames = dir.list(uploadIdFileFilter);

            if (fileNames == null || fileNames.length == 0) {
                String errorMessage = "Media not found";
                System.err.println(errorMessage);
                res.sendJsonResponse(404, createErrorResponse(errorMessage));
                return;
            }

            String fileName = fileNames[0];

            // Extract sub from fileName
            String[] parts = fileName.split("_|\\.");
            String fileSub = parts[0];
            String fileType = parts[2];

            if (fileType.equals("container") && !sub.equals(fileSub)) {
                String errorMessage = "Unauthorized";
                System.err.println(errorMessage);
                res.sendJsonResponse(401, createErrorResponse(errorMessage));
                return;
            }

            // Get video file from disk
            byte[] videoFile = Files.readAllBytes(Paths.get(targetDirectory + fileName));

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
