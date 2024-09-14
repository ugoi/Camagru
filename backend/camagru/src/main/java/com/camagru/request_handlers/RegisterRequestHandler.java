package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.camagru.PasswordUtil;
import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.camagru.services.SendVerificationEmailService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterRequestHandler implements HttpHandler {
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
                handleDefaultRequest(req, res); // Method not allowed
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
                JSONObject jsonBody = req.getBodyAsJson();
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("email", true, RegexUtil.EMAIL_REGEX),
                        new PropertyField("username", true, RegexUtil.USERNAME_REGEX),
                        new PropertyField("password", true, RegexUtil.PASSWORD_REGEX));
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields);
                List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    System.err.println(errorMessage);
                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                // Extract fields
                String username = jsonBody.getString("username");
                String email = jsonBody.getString("email");
                String password = jsonBody.getString("password");

                // Hashing password
                String hashedPassword = PasswordUtil.hashPassword(password);

                // Add user to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {

                    List<String> existingFields = new ArrayList<>();

                    // Check if username, email already exists
                    ResultSet rs = stmt
                            .executeQuery(
                                    "select * from users where username='" + username + "' OR email='" + email + "'");
                    while (rs.next()) {
                        if (rs.getString("username").equals(username)) {
                            existingFields.add("username");
                        }
                        if (rs.getString("email").equals(email)) {
                            existingFields.add("email");
                        }
                    }

                    if (!existingFields.isEmpty()) {
                        String errorMessage = "The following fields already exist: "
                                + String.join(", ", existingFields);
                        System.err.println(errorMessage);
                        res.sendJsonResponse(409, createErrorResponse(errorMessage));
                        return;
                    }

                    stmt.executeUpdate("INSERT INTO users(username, email, password, isEmailVerified)"
                            + " VALUES('" + username + "', '" + email + "', '" + hashedPassword + "', false)");
                }

                // Send verification email
                try {
                    SendVerificationEmailService.sendForVerifyingEmail(email);
                } catch (Exception e) {
                    String errorMessage = "Failed to send email: " + e.getMessage();
                    res.sendJsonResponse(500, createErrorResponse(errorMessage));
                    e.printStackTrace();
                    return;
                }

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("message", "User successfully registered");
                jsonResponse.put("username", username);
                jsonResponse.put("email", email);

                res.sendJsonResponse(201, jsonResponse.toString());
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
