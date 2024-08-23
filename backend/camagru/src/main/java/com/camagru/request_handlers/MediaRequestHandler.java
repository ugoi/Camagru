package com.camagru.request_handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.tika.Tika;
import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MediaRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Content upload request received");

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
            JSONObject decodedJwt = jwtManager.decodeToken(jwt);
            String sub = decodedJwt.getJSONObject("payload").getString("sub");

            String containerDescription = "";
            try {
                containerDescription = req.getQueryParameter("containerDescription");
            } catch (Exception e) {
                containerDescription = "";
            }

            HashMap<String, byte[]> files = req.files();
            byte[] videoFile = files.get("media");

            String extension;
            String mimeType;
            Tika tika = new Tika();
            try (ByteArrayInputStream input = new ByteArrayInputStream(videoFile)) {
                mimeType = tika.detect(input);
                if (mimeType.equals("video/mp4")) {
                    extension = ".mp4";
                } else if (mimeType.equals("video/quicktime")) {
                    extension = ".mov";
                } else if (mimeType.equals("image/png")) {
                    extension = ".png";
                } else if (mimeType.equals("image/jpeg")) {
                    extension = ".jpeg";
                } else {
                    throw new Exception("Invalid mime type");
                }
            }

            String mediaFileName;
            long containerId;
            // Add media to database
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                int affectedColumns = stmt.executeUpdate(
                        "INSERT INTO containers(user_id, mime_type, container_description, container_date)"
                                + " VALUES('" + sub + "', '" + mimeType + "', '" + containerDescription + "', '"
                                + java.time.LocalDateTime.now() + "')",
                        Statement.RETURN_GENERATED_KEYS);
                if (affectedColumns == 0) {
                    String errorMessage = "Failed to add media to database";
                    res.sendJsonResponse(500, createErrorResponse(errorMessage));
                    return;
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    containerId = keys.getLong(1);
                }

                mediaFileName = sub + "_" + containerId + "_container" + extension;

                System.out.println("Successfully connected to database and added user");
            }

            // Save video file to disk
            new File("uploads/media/").mkdirs();
            OutputStream out = new FileOutputStream(new File("uploads/media/" + mediaFileName));
            out.write(videoFile);
            out.close();

            // Save video file to disk
            JSONObject jsonResponse = new JSONObject()
                    .put("containerId", containerId)
                    .put("downloadUrl",
                            "http://localhost:8000/api/media?containerId=" + containerId)
                    .put("publishUrl", "http://localhost:8000/api/media_publish?containerId=" + containerId);
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
