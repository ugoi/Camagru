package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.services.EmailService;
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
        res.sendOptionsResponse(res);
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    // Main methods
    private void handleGetRequest(Request req, Response res) {
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

                JSONArray jsonArray = new JSONArray();

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {

                    String sql = """
                            SELECT comments.*, users.username
                            FROM comments
                            INNER JOIN users ON comments.user_id = users.user_id
                            WHERE comments.media_uri=?
                            ORDER BY comment_date DESC
                            """;
                    PreparedStatement myStmt;
                    myStmt = con.prepareStatement(sql);
                    myStmt.setString(1, mediaUri);
                    ResultSet rs = myStmt.executeQuery();

                    while (rs.next()) {
                        String commentTitle = "";
                        String commentBody = "";
                        String userId = "";
                        String username = "";

                        commentTitle = rs.getString("comment_title");
                        commentBody = rs.getString("comment_body");
                        mediaUri = rs.getString("media_uri");
                        userId = rs.getString("user_id");
                        username = rs.getString("username");
                        JSONObject comment = new JSONObject();

                        comment.put("comment_title", commentTitle);
                        comment.put("comment_body", commentBody);
                        comment.put("media_uri", mediaUri);
                        comment.put("user_id", userId);
                        comment.put("username", username);
                        jsonArray.put(comment);
                    }
                }

                JSONObject responseBody = new JSONObject();

                responseBody.put("data", jsonArray);

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
                        new PropertyField("comment_title", true),
                        new PropertyField("comment_body", true),
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
                String commentTitle = json.getString("comment_title");
                String commentBody = json.getString("comment_body");
                Timestamp commentDate = new Timestamp(System.currentTimeMillis());

                // Db resuls
                String username = "";

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {
                    long comment_id;
                    {

                        String sql = """
                                    INSERT INTO comments(media_uri, user_id, comment_title, comment_body, comment_date)
                                    VALUES(?, ?, ?, ?, ?)
                                """;

                        PreparedStatement myStmt;

                        myStmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                        myStmt.setString(1, mediaUri);
                        myStmt.setString(2, sub);
                        myStmt.setString(3, commentTitle);
                        myStmt.setString(4, commentBody);
                        myStmt.setTimestamp(5, commentDate);

                        try {
                            int affectedColumns = myStmt.executeUpdate();

                            if (affectedColumns == 0) {
                                String errorMessage = "Failed to add media to database";
                                res.sendJsonResponse(500, createErrorResponse(errorMessage));
                                return;
                            }
                        } catch (SQLException e) {
                            String message = e.getMessage();
                            if (message.contains("Data truncation")) {
                                String errorMessage = "Data too long: " + message;
                                res.sendJsonResponse(413, createErrorResponse(errorMessage));
                                return;

                            }
                            String errorMessage = "Failed to add media to database: " + e.getMessage();
                            res.sendJsonResponse(500, createErrorResponse(errorMessage));
                            return;
                        }

                        try (ResultSet generatedKeys = myStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                comment_id = generatedKeys.getLong(1);
                            } else {
                                throw new SQLException("Creating user failed, no ID obtained.");
                            }
                        }
                    }

                    String sql = """
                            SELECT comments.*, users.username
                            FROM comments
                            INNER JOIN users ON comments.user_id = users.user_id
                            WHERE comment_id=?
                            """;
                    PreparedStatement myStmt;
                    myStmt = con.prepareStatement(sql);
                    myStmt.setString(1, String.valueOf(comment_id));
                    ResultSet rs = myStmt.executeQuery();

                    if (rs.next()) {
                        username = rs.getString("username");
                    }

                    // Send email to author of the media
                    String sql2 = """
                            SELECT users.email, users.username, users.enabledNotifications
                            FROM users
                            INNER JOIN media ON users.user_id = media.user_id
                            WHERE media_uri=?
                            """;

                    PreparedStatement myStmt2;
                    myStmt2 = con.prepareStatement(sql2);
                    myStmt2.setString(1, mediaUri);
                    ResultSet rs2 = myStmt2.executeQuery();

                    if (rs2.next()) {
                        String email = rs2.getString("email");
                        String authorName = rs2.getString("username");
                        boolean enabledNotifications = rs2.getBoolean("enabledNotifications");

                        if (enabledNotifications) {

                            EmailService service = EmailService.create();
                            String emailTemplate = """
                                    <!DOCTYPE html>
                                    <html lang="en">

                                    <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <meta http-equiv="X-UA-Compatible" content="ie=edge">
                                        <title>New Comment Notification</title>
                                        <style>
                                            body {
                                                font-family: Arial, sans-serif;
                                                background-color: #f4f4f4;
                                                margin: 0;
                                                padding: 20px;
                                            }
                                            .container {
                                                background-color: #ffffff;
                                                margin: 0 auto;
                                                padding: 20px;
                                                border-radius: 8px;
                                                max-width: 600px;
                                                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                                            }
                                            h1 {
                                                color: #333333;
                                            }
                                            p {
                                                font-size: 16px;
                                                color: #555555;
                                            }
                                            .btn {
                                                display: inline-block;
                                                padding: 10px 20px;
                                                background-color: #007bff;
                                                color: #ffffff;
                                                text-decoration: none;
                                                border-radius: 5px;
                                                font-size: 16px;
                                            }
                                            .btn:hover {
                                                background-color: #0056b3;
                                            }
                                            .footer {
                                                margin-top: 20px;
                                                font-size: 12px;
                                                color: #888888;
                                            }
                                        </style>
                                    </head>

                                    <body>
                                        <div class="container">
                                            <h1>New Comment on Your Image!</h1>
                                            <p>Hello <strong>%s</strong>,</p>
                                            <p>Your image has received a new comment:</p>
                                            <blockquote style="background-color: #f9f9f9; padding: 10px; border-left: 3px solid #007bff; margin: 10px 0;">
                                                "%s"
                                            </blockquote>
                                        </div>
                                    </body>
                                    </html>
                                                                              """;

                            // Send email with reset password link
                            String formattedEmail = String.format(emailTemplate, authorName, commentBody);
                            service.send(authorName, email, "New comment", formattedEmail);
                        }

                    }
                }

                // Save video file to disk

                JSONObject message = new JSONObject();
                message
                        .put("media_uri", mediaUri)
                        .put("user_id", sub)
                        .put("username", username)
                        .put("comment_title", commentTitle)
                        .put("comment_body", commentBody);

                res.sendResponse(200, message);
            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });
    }
}
