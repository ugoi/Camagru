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
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, *");
        res.sendJsonResponse(204, ""); // No content
    }

    private void handlePutRequest(Request req, Response res) {
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

            // Check if token exists and is valid for password and then set tokenUserId.
            String token = req.getQueryParameter("token", null);
            String tokenUserId = null;
            if (token != null) {
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {

                    // If email exists, generate a reset password token and store it in the database
                    String preparedStmt = "SELECT * FROM tokens WHERE token = ?";
                    PreparedStatement myStmt;
                    myStmt = con.prepareStatement(preparedStmt);
                    myStmt.setString(1, token);
                    ResultSet rs = myStmt.executeQuery();

                    String userId = null;
                    String type = "";
                    Timestamp expiryDate = null;
                    Boolean used = false;
                    while (rs.next()) {
                        userId = rs.getString("user_id");
                        type = rs.getString("type");
                        expiryDate = rs.getTimestamp("expiry_date");
                        used = rs.getBoolean("used");
                    }
                    Boolean isExpired = false;
                    if (expiryDate != null) {
                        Timestamp currentDate = new Timestamp(System.currentTimeMillis());
                        isExpired = expiryDate.before(currentDate);
                    }

                    if (!used && type.equals("password_reset") && !isExpired) {
                        tokenUserId = userId;
                        String invalidateTokenQuery = "update tokens set used=true where token='" + token + "'";
                        int rs2 = stmt.executeUpdate(invalidateTokenQuery);
                        if (rs2 != 0) {
                            System.out.println("Successfully connected to database and invalidated token");
                        } else {
                            String errorMessage = "Token not found";
                            System.err.println(errorMessage);
                            res.sendJsonResponse(404, createErrorResponse(errorMessage));
                        }
                    }

                }
            }

            // Hashing password
            String hashedPassword = PasswordUtil.hashPassword(requestPassword);

            // If valid token was provided, skip jwt verification and use tokenUserId
            String sub = null;
            if (tokenUserId == null) {
                String jwt = CookieUtil.getCookie(cookieHeader, "token");
                JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
                jwtManager.verifySignature(jwt);
                sub = jwtManager.decodeToken(jwt).getJSONObject("payload").getString("sub");
            } else {
                sub = tokenUserId;
            }

            String query = "update users set password='" + hashedPassword + "' where user_id='" + sub + "'";

            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                int rs = stmt.executeUpdate(query);
                if (rs != 0) {
                    System.out.println("Successfully connected to database and updated user");
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
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }
}
