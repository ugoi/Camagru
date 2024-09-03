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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MediaRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Content upload request received");

        Request req = new Request(exchange);
        Response res = new Response(exchange);

        switch (req.getMethod()) {
            case "GET":
                handleGetRequest(req, res);
                break;
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

    private void handleGetRequest(Request req, Response res) {
        try {
            // Validate input
            List<PropertyField> propertyFields = Arrays.asList(
                    new PropertyField("after", false),
                    new PropertyField("limit", false, RegexUtil.NUMBER_REGEX))

            ;
            PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields, null);
            List<String> wrongFields = propertyFieldsManager.validationResult(req);

            if (!wrongFields.isEmpty()) {
                String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                System.err.println(errorMessage);
                res.sendJsonResponse(400, createErrorResponse(errorMessage));
                return;
            }

            // Extract input
            String lastPictureId = req.getQueryParameter("after", null);
            String limit = req.getQueryParameter("limit", "30");

            // Properties
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            String sub;
            try {
                String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
                jwtManager.verifySignature(jwt);
                sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");
            } catch (Exception e) {
                String errorMessage = "Authentication failed: Invalid JWT token. Please include a valid \"token\" in the request Cookie. Error details: "
                        + e.getMessage();
                res.sendJsonResponse(401, createErrorResponse(errorMessage));
                return;
            }

            List<String> ids = new ArrayList<>();
            // Add media to database
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                String query;
                if (lastPictureId != null) {
                    query = String.format(
                            "SELECT * FROM media " +
                                    "WHERE user_id='%s' " +
                                    "AND media_date < (SELECT media_date FROM media WHERE media_id='%s') " +
                                    "ORDER BY media_date DESC " +
                                    "LIMIT %s",
                            sub, lastPictureId, limit);
                } else {
                    query = String.format(
                            "SELECT * FROM media " +
                                    "WHERE user_id='%s' " +
                                    "ORDER BY media_date DESC " +
                                    "LIMIT %s",
                            sub, limit);
                }

                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    String id = rs.getString("media_id");
                    ids.add(id);
                    System.out.println(id);

                }
                System.out.println("Successfully connected to database and added user");
            }

            // find first element
            String first;
            String last;
            String next;
            String previous;
            try {
                first = ids.get(0);
                previous = "http://localhost:8000/api/media?after=" + first;
            } catch (Exception e) {
                first = null;
                previous = null;
            }
            try {
                last = ids.get(ids.size() - 1);
                next = "http://localhost:8000/api/media?after=" + last;

            } catch (Exception e) {
                last = null;
                next = null;
            }

            JSONObject responseBody = new JSONObject();
            JSONArray responseBodyData = new JSONArray();
            for (String id : ids) {
                responseBodyData.put(new JSONObject().put("id", id));
            }

            responseBody.put("paging", new JSONObject()
                    .put("after", last != null ? last : JSONObject.NULL)
                    .put("before", first != null ? first : JSONObject.NULL)
                    .put("next", next != null ? next : JSONObject.NULL)
                    .put("previous", previous != null ? previous : JSONObject.NULL))
                    .put("data", responseBodyData);

            res.sendJsonResponse(200, responseBody.toString());

        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }

    }

    private void handlePostRequest(Request req, Response res) {
        try {
            // Validate input
            List<PropertyField> propertyFields = Arrays.asList(
                    new PropertyField("scale_factor", false, RegexUtil.NUMBER_BETWEEN_0_AND_1_REGEX),
                    new PropertyField("x_position_factor", false, RegexUtil.NUMBER_BETWEEN_0_AND_1_REGEX),
                    new PropertyField("y_position_factor", false, RegexUtil.NUMBER_BETWEEN_0_AND_1_REGEX));
            PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields, null);
            List<String> wrongFields = propertyFieldsManager.validationResult(req);

            if (!wrongFields.isEmpty()) {
                String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                System.err.println(errorMessage);
                res.sendJsonResponse(400, createErrorResponse(errorMessage));
                return;
            }

            // Get query parameters
            double scaleFactor = Double.parseDouble(req.getQueryParameter("scale_factor", "0.5"));
            double xPositionFactor = Double.parseDouble(req.getQueryParameter("x_position_factor", "0.5"));
            double yPositionFactor = Double.parseDouble(req.getQueryParameter("y_position_factor", "0.5"));

            // Properties
            PropertiesManager propertiesManager = new PropertiesManager();
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            String sub;
            try {
                String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
                jwtManager.verifySignature(jwt);
                sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");
            } catch (Exception e) {
                String errorMessage = "Authentication failed: Invalid JWT token. Please include a valid \"token\" in the request Cookie. Error details: "
                        + e.getMessage();
                res.sendJsonResponse(401, createErrorResponse(errorMessage));
                return;
            }

            String containerDescription = "";
            try {
                containerDescription = req.getQueryParameter("containerDescription");
            } catch (Exception e) {
                containerDescription = "";
            }

            HashMap<String, byte[]> files = req.files();
            byte[] media = files.get("media");
            byte[] overlayMedia = files.get("overlayMedia");

            if (media == null || overlayMedia == null) {
                String errorMessage = "Media and overlayMedia must be provided";
                System.err.println(errorMessage);
                res.sendJsonResponse(400, createErrorResponse(errorMessage));
                return;
            }

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
                        "INSERT INTO media(user_id, mime_type, media_description, media_type, media_date)"
                                + " VALUES('" + sub + "', '" + mimeType + "', '" + containerDescription + "', '" + "container" + "', '"
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

            String uploadFilePath = "uploads/media/" + mediaFileName;
            // Save video file to disk
            combineMedia(media, overlayMedia, uploadFilePath, scaleFactor, xPositionFactor, yPositionFactor);

            // Save video file to disk
            JSONObject jsonResponse = new JSONObject()
                    .put("containerId", containerId)
                    .put("downloadUrl",
                            "http://localhost:8000/api/media?id=" + containerId)
                    .put("publishUrl", "http://localhost:8000/api/media_publish?id=" + containerId);
            res.sendJsonResponse(200, jsonResponse.toString());
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    private void combineMedia(byte[] media, byte[] overlayMedia, String outputFile, Double scaleFactor,
            Double xPositionFactor, Double yPositionFactor) throws Exception {

        if (scaleFactor == null) {
            scaleFactor = 0.5;
        }
        if (xPositionFactor == null) {
            xPositionFactor = 0.5;
        }
        if (yPositionFactor == null) {
            yPositionFactor = 0.5;
        }

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
        String mediaFilePath = mediaPath + mediaFileName;
        String overlayFilePath = mediaPath + overlayFileName;

        // String ffmpegCommand = String.format(
        // "ffmpeg -i %s -i %s -filter_complex \"[1]scale=iw*%f:-1[b];[0:v][b]
        // overlay=x=(W-w)*%f:y=(H-h)*%f\" %s -y",
        // mediaFilePath, overlayFilePath, scaleFactor, xPositionFactor,
        // yPositionFactor, outputFile);

        String ffmpegCommand = String.format(
                "my_width=$(ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0 %s | awk -F',' '{print $1}'); "
                        +
                        "ffmpeg -i %s -i %s -filter_complex \"[1]scale=${my_width}*%f:-1[b];[0:v][b]overlay=x=(W-w)*%f:y=(H-h)*%f\" %s -y",
                mediaFilePath, mediaFilePath, overlayFilePath, scaleFactor, xPositionFactor, yPositionFactor,
                outputFile);

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
