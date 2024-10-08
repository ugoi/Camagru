package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LikesRequestHandler implements HttpHandler {
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
            case "DELETE":
                handleDeleteRequest(req, res);
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

    // Main methods

    private void handleGetRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {
            try {
                PropertiesManager propertiesManager = new PropertiesManager();
                String sub = null;
                try {
                    JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
                    String jwt = CookieUtil.getCookie(req.getHeader("Cookie"), "token");
                    jwtManager.verifySignature(jwt);
                    sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");

                } catch (Exception e) {
                    sub = null;
                }

                // Validate input
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("media_id", true));

                ;
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields, null);
                List<String> wrongFields = propertyFieldsManager.validationResult(req);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                // Extract input
                String mediaUri = req.getQueryParameter("media_id");

                // Db variables
                int total = 0;
                boolean hasLiked = false;

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {
                    String sql = """
                            SELECT COUNT(*)
                            FROM likes
                            WHERE media_uri=?
                            AND reaction=?
                            """;
                    PreparedStatement myStmt;
                    myStmt = con.prepareStatement(sql);
                    myStmt.setString(1, mediaUri);
                    myStmt.setString(2, "like");
                    ResultSet rs = myStmt.executeQuery();

                    while (rs.next()) {
                        total = rs.getInt(1);
                    }

                    if (sub != null) {
                        hasLiked = hasUserLikedMedia(mediaUri, sub, con);
                    }
                }

                JSONObject responseBody = new JSONObject();

                responseBody.put("total_count", total);
                responseBody.put("has_liked", hasLiked);

                res.sendJsonResponse(200, responseBody.toString());

            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });
    }

    private void handlePostRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {

            try {
                // Validate input
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("reaction", false),
                        new PropertyField("media_id", true));
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(null, propertyFields);
                List<String> wrongFields = propertyFieldsManager.validationResult(req);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
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
                String mediaUri = json.getString("media_id");

                String reaction;
                try {
                    reaction = json.getString("reaction");
                } catch (Exception e) {
                    reaction = "like";
                }

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {
                    // Check if like already exists

                    if (hasUserLikedMedia(mediaUri, sub, con)) {
                        String resposne = "You already liked this media.";
                        res.sendResponse(400, resposne);
                        return;

                    }

                    String sql = """
                                INSERT INTO likes(media_uri, user_id, reaction)
                                VALUES(?, ?, ?)
                            """;

                    PreparedStatement myStmt;

                    myStmt = con.prepareStatement(sql);
                    myStmt.setString(1, mediaUri);
                    myStmt.setString(2, sub);
                    myStmt.setString(3, reaction);

                    int affectedColumns = myStmt.executeUpdate();

                    if (affectedColumns == 0) {
                        String errorMessage = "Failed to add media to database";
                        res.sendJsonResponse(500, createErrorResponse(errorMessage));
                        return;
                    }

                }

                // Save video file to disk

                res.sendResponse(200, "Successfully posted like!");
            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });
    }

    private void handleDeleteRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {

            try {
                // Validate input
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("media_id", true));

                ;
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields, null);
                List<String> wrongFields = propertyFieldsManager.validationResult(req);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);

                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                // Extract input
                String mediaUri = req.getQueryParameter("media_id");

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

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {
                    String sql = """
                            DELETE
                            FROM likes
                            WHERE media_uri=?
                            AND user_id=?
                            """;
                    PreparedStatement myStmt;
                    myStmt = con.prepareStatement(sql);
                    myStmt.setString(1, mediaUri);
                    myStmt.setString(2, sub);
                    int rs = myStmt.executeUpdate();

                    if (rs != 0) {
                        String response = "Successuflly deleted like";
                        res.sendResponse(200, response);
                    } else {
                        String response = "Like not found";
                        res.sendResponse(404, response);
                    }

                }
                return;

            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });

    }

    private boolean hasUserLikedMedia(String mediaUri, String userId, Connection con) throws Exception {
        String sql = """
                    SELECT 1
                    FROM likes
                    WHERE media_uri=?
                    AND user_id=?
                """;

        PreparedStatement myStmt;

        myStmt = con.prepareStatement(sql);
        myStmt.setString(1, mediaUri);
        myStmt.setString(2, userId);

        ResultSet rs = myStmt.executeQuery();

        if (rs.next()) {
            return true;
        } else {
            return false;
        }

    }
}
