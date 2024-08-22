package com.camagru.request_handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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

            String userId;
            // Add user to database
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                String queryMedia = "select * from media where media_id='" + contentId + "'";
                String queryContainers = "select * from containers where container_id='" + contentId + "'";

                ResultSet rsMedia = stmt
                        .executeQuery(queryMedia);

                if (rsMedia.next()) {
                    userId = rsMedia.getString("user_id");
                } else {
                    ResultSet rsContainers = stmt
                            .executeQuery(queryContainers);
                    if (rsContainers.next()) {
                        userId = rsContainers.getString("user_id");
                    } else {
                        String errorMessage = "Media not found";
                        System.err.println(errorMessage);
                        res.sendJsonResponse(404, createErrorResponse(errorMessage));
                        return;
                    }
                }
                System.out.println("Successfully connected to database and added user");
            }

            if (!sub.equals(userId)) {
                String errorMessage = "Unauthorized";
                System.err.println(errorMessage);
                res.sendJsonResponse(401, createErrorResponse(errorMessage));
                return;
            }

            // Find out all log files
            String targetDirectory = "uploads/media/";
            File dir = new File(targetDirectory);
            FilenameFilter uploadIdFileFilter = (d, s) -> {
                return s.startsWith(sub + "_" + contentId);
            };
            String[] fileNames = dir.list(uploadIdFileFilter);

            if (fileNames == null || fileNames.length == 0) {
                String errorMessage = "Media not found";
                System.err.println(errorMessage);
                res.sendJsonResponse(404, createErrorResponse(errorMessage));
                return;
            }

            String fileName = fileNames[0];

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
