package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PasswordUtil;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ProfilePasswordRequestHandler implements HttpHandler {
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
                        new PropertyField("password", true, RegexUtil.PASSWORD_REGEX));
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields);
                List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    System.err.println(errorMessage);
                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                String cookieHeader = req.getHeader("Cookie");
                String requestPassword = jsonBody.getString("password");

                // Check if token exists and is valid for password reset
                String token = req.getQueryParameter("token", null);
                String tokenUserId = null;
                if (token != null) {
                    try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                            propertiesManager.getDbUsername(), propertiesManager.getDbPassword())) {

                        String tokenQuery = "SELECT * FROM tokens WHERE token = ?";
                        try (PreparedStatement tokenStmt = con.prepareStatement(tokenQuery)) {
                            tokenStmt.setString(1, token);
                            ResultSet rs = tokenStmt.executeQuery();

                            String userId = null;
                            String type = "";
                            Timestamp expiryDate = null;
                            boolean used = false;
                            while (rs.next()) {
                                userId = rs.getString("user_id");
                                type = rs.getString("type");
                                expiryDate = rs.getTimestamp("expiry_date");
                                used = rs.getBoolean("used");
                            }

                            boolean isExpired = expiryDate != null && expiryDate.before(new Timestamp(System.currentTimeMillis()));

                            if (!used && type.equals("password_reset") && !isExpired) {
                                tokenUserId = userId;
                                String invalidateTokenQuery = "UPDATE tokens SET used = true WHERE token = ?";
                                try (PreparedStatement invalidateStmt = con.prepareStatement(invalidateTokenQuery)) {
                                    invalidateStmt.setString(1, token);
                                    int updateCount = invalidateStmt.executeUpdate();
                                    if (updateCount == 0) {
                                        String errorMessage = "Token not found";
                                        System.err.println(errorMessage);
                                        res.sendJsonResponse(404, createErrorResponse(errorMessage));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                // Hash the password
                String hashedPassword = PasswordUtil.hashPassword(requestPassword);

                // If a valid token was provided, use tokenUserId, otherwise verify JWT
                String sub = tokenUserId;
                if (sub == null) {
                    String jwt = CookieUtil.getCookie(cookieHeader, "token");
                    JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
                    jwtManager.verifySignature(jwt);
                    sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");
                }

                // Update the password in the database
                String updatePasswordQuery = "UPDATE users SET password = ? WHERE user_id = ?";
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        PreparedStatement updateStmt = con.prepareStatement(updatePasswordQuery)) {

                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, sub);

                    int updateCount = updateStmt.executeUpdate();
                    if (updateCount > 0) {
                        res.sendJsonResponse(200,
                                new JSONObject().put("message", "Password updated successfully").toString());
                    } else {
                        String errorMessage = "User not found";
                        System.err.println(errorMessage);
                        res.sendJsonResponse(404, createErrorResponse(errorMessage));
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
