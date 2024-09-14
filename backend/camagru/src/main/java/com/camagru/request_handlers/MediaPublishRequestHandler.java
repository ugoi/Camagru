package com.camagru.request_handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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

public class MediaPublishRequestHandler implements HttpHandler {
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

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
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
                    Arrays.asList(new PropertyField("creation_id", true)), null)
                    .validationResult(req);

            if (!wrongFields.isEmpty()) {
                res.sendJsonResponse(400,
                        createErrorResponse("The following fields are invalid: " + String.join(", ", wrongFields)));
                return;
            }
            String creationId = req.getQueryParameter("creation_id");
            // Add media to database
            String mediaFileName;
            String mediaId = null;
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                ResultSet rs = stmt
                        .executeQuery(
                                "select * from media where container_uri='" + creationId + "'"
                                        + " AND media_type='container'");
                String userId = null, mimeType = null;
                while (rs.next()) {
                    mediaId = rs.getString("media_uri");
                    userId = rs.getString("user_id");
                    mimeType = rs.getString("mime_type");
                }

                if (mediaId == null) {
                    String errorMessage = "Media not found";
                    res.sendJsonResponse(404, createErrorResponse(errorMessage));
                    return;
                }

                if (!userId.equals(sub)) {
                    String errorMessage = "Unauthorized";
                    res.sendJsonResponse(401, createErrorResponse(errorMessage));
                    return;
                }

                // UPDATE THE MEDIAS MEDIA TYPE FROM CONTAINER TO MEDIA

                int affectedColumns = stmt.executeUpdate(
                        "UPDATE media SET media_type='media' WHERE container_uri='" + creationId
                                + "' AND media_type='container'");

                if (affectedColumns == 0) {
                    String errorMessage = "Failed to add media to database";
                    res.sendJsonResponse(500, createErrorResponse(errorMessage));
                    return;
                }

                String extension = HttpUtil.getMimeTypeExtension(mimeType);

                mediaFileName = sub + "_" + mediaId + "_media" + extension;

            }

            String mediaPath = "uploads/media/";
            MediaService.hasPermission(sub, creationId);
            byte[] mediaFile = MediaService.getMedia(creationId);
            OutputStream outMedia = new FileOutputStream(new File(mediaPath + mediaFileName));
            new File(mediaPath).mkdirs();
            outMedia.write(mediaFile);
            outMedia.close();

            res.sendJsonResponse(200,
                    new JSONObject()
                            .put("media_uri", mediaId)
                            .put("downloadUrl",
                                    "http://127.0.0.1:8000/api/serve/media?id=" + mediaId)
                            .toString());

        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }

    }
}
