package com.camagru.request_handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
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

public class MediaPublishRequestHandler implements HttpHandler {
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

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    private void handlePostRequest(Request req, Response res) {
        try {

            // TODO: Refactor to AuthUtils
            // Extract jwt from cookie
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
            jwtManager.verifySignature(jwt);
            JSONObject decodedJwt = jwtManager.decodeToken(jwt);
            String sub = decodedJwt.getJSONObject("payload").getString("sub");

            String creationId;
            try {
                creationId = req.getQueryParameter("creation_id");
            } catch (Exception e) {
                String errorMessage = "Missing creation_id";
                System.err.println(errorMessage);
                res.sendJsonResponse(400, createErrorResponse(errorMessage));
                return;
            }

            // TODO: Refactor out to get media in MediaService
            // Find out all log files
            String targetDirectory = "uploads/media/";
            File dir = new File(targetDirectory);
            FilenameFilter uploadIdFileFilter = (d, s) -> {
                String[] parts = s.split("_|\\.");

                return parts[1].equals(creationId);
            };
            String[] fileNames = dir.list(uploadIdFileFilter);

            if (fileNames == null || fileNames.length == 0) {
                String errorMessage = "Media not found";
                System.err.println(errorMessage);
                res.sendJsonResponse(404, createErrorResponse(errorMessage));
                return;
            }
            String fileName = fileNames[0];

            // TODO: Move to auth service in check permissions for file method
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

            // TODO: Refactor out Get the media
            // Get video file from disk
            byte[] mediaFile = Files.readAllBytes(Paths.get(targetDirectory + fileName));

            // Publish the file

            // Add media to database
            String mediaFileName;
            long mediaId;
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                // Get media from database
                // Check if username, email already exists
                ResultSet rs = stmt
                        .executeQuery(
                                "select * from containers where container_id='" + creationId + "'");
                String userId = null, mimeType = null, containerDescription = null;
                while (rs.next()) {
                    userId = rs.getString("user_id");
                    mimeType = rs.getString("mime_type");
                    containerDescription = rs.getString("container_description");

                }

                int affectedColumns = stmt.executeUpdate(
                        "INSERT INTO media(user_id, mime_type, media_description, media_date)"
                                + " VALUES('" + userId + "', '" + mimeType + "', '" + containerDescription + "', '"
                                + java.time.LocalDateTime.now() + "')",
                        Statement.RETURN_GENERATED_KEYS);
                if (affectedColumns == 0) {
                    String errorMessage = "Failed to add media to database";
                    res.sendJsonResponse(500, createErrorResponse(errorMessage));
                    return;
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    mediaId = keys.getLong(1);
                }

                String extension = HttpUtil.getMimeTypeExtension(mimeType);

                mediaFileName = sub + "_" + mediaId + "_media" + extension;

                System.out.println("Successfully connected to database and added user");
            }

            String mediaPath = "uploads/media/";
            new File(mediaPath).mkdirs();
            OutputStream outMedia = new FileOutputStream(new File(mediaPath + mediaFileName));
            outMedia.write(mediaFile);
            outMedia.close();

            res.sendJsonResponse(200,
                    new JSONObject()
                            .put("media_id", mediaId)
                            .put("downloadUrl",
                                    "http://localhost:8000/api/media?id=" + mediaId)
                            .toString());

        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }

    }
}
