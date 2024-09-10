package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class CommentRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

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
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.sendJsonResponse(204, ""); // No content
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    // Main methods
    private void handleGetRequest(Request req, Response res) {
        try {
            // Validate input
            List<PropertyField> propertyFields = Arrays.asList(
                    new PropertyField("media_id", true));

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
            String mediaID = req.getQueryParameter("media_id");

            // Properties
            PropertiesManager propertiesManager = new PropertiesManager();

            JSONArray jsonArray = new JSONArray();

            // Add media to database
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                String sql = """
                        SELECT * FROM comments
                        WHERE media_id=?
                        ORDER BY comment_date DESC
                        """;
                PreparedStatement myStmt;
                myStmt = con.prepareStatement(sql);
                myStmt.setString(1, mediaID);
                ResultSet rs = myStmt.executeQuery();

                while (rs.next()) {
                    String commentTitle = "";
                    String commentBody = "";
                    String mediaId = "";
                    String userId = "";
                    commentTitle = rs.getString("comment_title");
                    commentBody = rs.getString("comment_body");
                    mediaId = rs.getString("media_id");
                    userId = rs.getString("user_id");

                    JSONObject comment = new JSONObject();

                    comment.put("comment_title", commentTitle);
                    comment.put("comment_body", commentBody);
                    comment.put("media_id", mediaId);
                    comment.put("user_id", userId);

                    jsonArray.put(comment);
                }
                System.out.println("Successfully connected to database and added user");
            }

            JSONObject responseBody = new JSONObject();

            responseBody.put("data", jsonArray);

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
                    new PropertyField("comment_title", true),
                    new PropertyField("comment_body", true),
                    new PropertyField("media_id", true));
            PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(null, propertyFields);
            List<String> wrongFields = propertyFieldsManager.validationResult(req);

            if (!wrongFields.isEmpty()) {
                String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                System.err.println(errorMessage);
                res.sendJsonResponse(400, createErrorResponse(errorMessage));
                return;
            }
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

            // Get body params
            JSONObject json = req.getBodyAsJson();
            String mediaId = json.getString("media_id");
            String commentTitle = json.getString("comment_title");
            String commentBody = json.getString("comment_body");
            Timestamp commentDate = new Timestamp(System.currentTimeMillis());

            // Add media to database
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                String sql = """
                            INSERT INTO comments(media_id, user_id, comment_title, comment_body, comment_date)
                            VALUES(?, ?, ?, ?, ?)
                        """;

                PreparedStatement myStmt;

                myStmt = con.prepareStatement(sql);
                myStmt.setString(1, mediaId);
                myStmt.setString(2, sub);
                myStmt.setString(3, commentTitle);
                myStmt.setString(4, commentBody);
                myStmt.setTimestamp(5, commentDate);

                int affectedColumns = myStmt.executeUpdate();

                if (affectedColumns == 0) {
                    String errorMessage = "Failed to add media to database";
                    res.sendJsonResponse(500, createErrorResponse(errorMessage));
                    return;
                }

                System.out.println("Successfully connected to database and added user");
            }

            // Save video file to disk

            res.sendResponse(200, "Successfully posted comment!");
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            res.sendJsonResponse(500, createErrorResponse(errorMessage));
        }
    }
}
