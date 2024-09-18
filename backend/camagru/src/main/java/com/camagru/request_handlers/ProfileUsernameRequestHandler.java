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

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ProfileUsernameRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Request req = new Request(exchange);
        Response res = new Response(exchange);

        switch (req.getMethod()) {
            case "PUT":
                handlePutRequest(req, res);
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

    private void handlePutRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {

            try {
                // Properties
                PropertiesManager propertiesManager = new PropertiesManager();
                JSONObject jsonBody = req.getBodyAsJson();
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("username", true, RegexUtil.USERNAME_REGEX));
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields);
                List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    System.err.println(errorMessage);
                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                String cookieHeader = req.getHeader("Cookie");
                String requestUsername = jsonBody.getString("username");
                String jwt = CookieUtil.getCookie(cookieHeader, "token");
                JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
                jwtManager.verifySignature(jwt);
                String sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");

                // Query to update the username
                String updateQuery = "UPDATE users SET username = ? WHERE user_id = ?";

                // Check if the username already exists
                String checkUsernameQuery = "SELECT username, user_id FROM users WHERE username = ?";

                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword())) {

                    // Check if username already exists
                    try (PreparedStatement checkStmt = con.prepareStatement(checkUsernameQuery)) {
                        checkStmt.setString(1, requestUsername);
                        ResultSet rs = checkStmt.executeQuery();

                        String existingUsername = "";
                        String existingUserId = "";
                        while (rs.next()) {
                            existingUsername = rs.getString("username");
                            existingUserId = rs.getString("user_id");
                        }

                        // If the username exists and belongs to a different user, return error
                        if (!existingUserId.equals(sub) && !existingUsername.isEmpty()) {
                            String errorMessage = "Username already exists";
                            System.err.println(errorMessage);
                            res.sendJsonResponse(409, createErrorResponse(errorMessage));
                            return;
                        }
                    }

                    // Update the username if it doesn't already exist
                    try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, requestUsername);
                        updateStmt.setString(2, sub);

                        int rowsAffected = updateStmt.executeUpdate();
                        if (rowsAffected > 0) {
                            res.sendJsonResponse(200,
                                    new JSONObject().put("message", "Username updated successfully").toString());
                        } else {
                            String errorMessage = "User not found";
                            System.err.println(errorMessage);
                            res.sendJsonResponse(404, createErrorResponse(errorMessage));
                        }
                    }
                }

            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                System.err.println(errorMessage);
                e.printStackTrace();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }
}
