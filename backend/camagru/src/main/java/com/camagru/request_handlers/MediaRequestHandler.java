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
            byte[] media = files.get("media");
            byte[] overlayMedia = files.get("overlayMedia");

            String extension;
            String mimeType;
            Tika tika = new Tika();
            try (ByteArrayInputStream input = new ByteArrayInputStream(media)) {
                mimeType = tika.detect(input);
                extension = HttpUtil.getMimeTypeExtension(mimeType);
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
            combineMedia(media, overlayMedia, "uploads/media/" + mediaFileName);

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

    private void combineMedia(byte[] media, byte[] overlayMedia, String outputFile) throws Exception {
        // Get mime type and extension
        String mediaMimeType;
        String mediaExtension;
        String overlayMimeType;
        String overlayExtension;
        {
            Tika tika = new Tika();
            try (ByteArrayInputStream input = new ByteArrayInputStream(media)) {
                mediaMimeType = tika.detect(input);
                mediaExtension = HttpUtil.getMimeTypeExtension(mediaMimeType);
            }
            try (ByteArrayInputStream input = new ByteArrayInputStream(overlayMedia)) {
                overlayMimeType = tika.detect(input);
                overlayExtension = HttpUtil.getMimeTypeExtension(overlayMimeType);
            }
        }

        // Create file names and paths
        String mediaUuid = java.util.UUID.randomUUID().toString();
        String overlayUuid = java.util.UUID.randomUUID().toString();
        String mediaFileName = mediaUuid + mediaExtension;
        String overlayFileName = overlayUuid + overlayExtension;
        String mediaPath = "uploads/temp/";

        // Step 1: Save media and overlayMedia to disk
        {
            new File(mediaPath).mkdirs();
            OutputStream outMedia = new FileOutputStream(new File(mediaPath + mediaFileName));
            outMedia.write(media);
            outMedia.close();
            OutputStream outOverlayMedia = new FileOutputStream(new File(mediaPath + overlayFileName));
            outOverlayMedia.write(overlayMedia);
            outOverlayMedia.close();
        }

        // Step 2: Build the ffmpeg command to concatenate the files
        String ffmpegCommand = String.format(
                "ffmpeg -i %s -i %s -filter_complex \"[1]scale=100:-1[b];[0:v][b] overlay=x=(W-w)*0.5:y=(H-h)*0.5\" %s -y",
                mediaPath + mediaFileName, mediaPath + overlayFileName, outputFile);

        // Step 3: Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", ffmpegCommand);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        // Step 4: Clean up in separate thread
        new File(mediaPath + mediaFileName).delete();
        new File(mediaPath + overlayFileName).delete();

        if (exitCode == 0) {
            System.out.println("MP4 files combined successfully into " + outputFile);
        } else {
            throw new Exception("Failed to combine MP4 files");
        }
    }
}
