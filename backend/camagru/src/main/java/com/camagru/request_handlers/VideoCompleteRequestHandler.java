package com.camagru.request_handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class VideoCompleteRequestHandler implements HttpHandler {
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
                    Arrays.asList(new PropertyField("uploadId", true)), null)
                    .validationResult(req);

            if (!wrongFields.isEmpty()) {
                res.sendJsonResponse(400,
                        createErrorResponse("The following fields are invalid: " + String.join(", ", wrongFields)));
                return;
            }
            // Generate uuid for video
            String uploadId = req.getQueryParameter("uploadId");
            String targetDirectory = "uploads/temp/";
            String combinedFilesDirectory = "uploads/";
            // Find out all log files
            File dir = new File(targetDirectory);
            FilenameFilter uploadIdFileFilter = (d, s) -> {
                return s.startsWith(sub + "_" + uploadId + "_");
            };
            String[] partFiles = dir.list(uploadIdFileFilter);
            // Sort log files by part number
            Arrays.sort(partFiles);
            // Combine all parts into one file
            String combinedFileName = combinedFilesDirectory + sub + "_" + uploadId + "_combined.mp4";
            // Combine all parts into one file
            // Step 1: Create a text file that lists all the MP4 files to combine
            String fileListPath = "file_list.txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileListPath))) {
                for (String partFile : partFiles) {
                    writer.write("file '" + targetDirectory + partFile + "'\n");
                }
            }

            // Step 2: Build the ffmpeg command to concatenate the files
            String outputFile = combinedFileName;
            String ffmpegCommand = String.format("ffmpeg -f concat -safe 0 -i %s -c copy %s", fileListPath, outputFile);

            // Step 3: Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", ffmpegCommand);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("MP4 files combined successfully into " + outputFile);
            } else {
                System.out.println("Failed to combine MP4 files. Exit code: " + exitCode);
                JSONObject jsonResponse = new JSONObject().put("message", "Failed to combine MP4 files");
                res.sendJsonResponse(500, jsonResponse.toString());
            }

            JSONObject jsonResponse = new JSONObject().put("message", "Request handled successfully");
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
