package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.camagru.JwtManager;
import com.camagru.PasswordUtil;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginRequestHandler implements HttpHandler {
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
                handleDefaultRequest(req, res); // allowed
                break;
        }
    }

    private void handleDefaultRequest(Request req, Response res) {
        res.sendJsonResponse(405, createErrorResponse("Unsupported method"));
    }

    private void handleOptionsRequest(Request req, Response res) {
        res.sendOptionsResponse(res);
    }

    private void handlePostRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {

            try {
                // Properties
                PropertiesManager propertiesManager = new PropertiesManager();

                // Get JSON body
                JSONObject jsonBody = req.getBodyAsJson();

                // Validate fields

                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(Arrays.asList(
                        new PropertyField("username", true),
                        new PropertyField("password", true, RegexUtil.PASSWORD_REGEX)));
                List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    System.err.println(errorMessage);

                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;  // Add return to avoid further execution
                }

                // Extract fields
                String username = jsonBody.getString("username");

                // Query to get user password from database using prepared statement
                String query = "SELECT * FROM users WHERE username = ? OR email = ?";

                String userId = null;
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        PreparedStatement pstmt = con.prepareStatement(query)) {

                    // Set the parameters for the query
                    pstmt.setString(1, username);
                    pstmt.setString(2, username);

                    // Execute the query
                    ResultSet rs = pstmt.executeQuery();
                    String userPassword = null;

                    if (rs.next()) {
                        userId = rs.getString("user_id");
                        userPassword = rs.getString("password");
                    } else {
                        String errorMessage = "User not found";
                        System.err.println(errorMessage);
                        res.sendJsonResponse(404, createErrorResponse(errorMessage));
                        return;  // Exit if the user is not found
                    }

                    try {
                        PasswordUtil.verifyPassword(jsonBody.getString("password"), userPassword);
                    } catch (Exception e) {
                        String errorMessage = "Invalid password";
                        System.err.println(errorMessage);

                        res.sendJsonResponse(401, createErrorResponse(errorMessage));
                        return;  // Exit if the password is invalid
                    }
                }

                // Generate JWT token after successful login
                JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
                String token = jwtManager.createToken(userId);

                // Set the token in the response header
                res.setHeader("Set-Cookie", "token=" + token
                        + "; Max-Age=3600000; Path=/; Expires=Wed, 09 Jun 2025 10:18:14 GMT; SameSite=Lax");

                res.sendJsonResponse(201, new JSONObject()
                        .put("message", "User successfully logged in").toString());

            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                System.err.println(errorMessage);
                e.printStackTrace();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });
    }

    private String createErrorResponse(String errorMessage) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("error", errorMessage);
        return jsonResponse.toString();
    }
}
