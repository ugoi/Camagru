package com.camagru;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

public class RegisterRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Database
        SimpleHttpResponse response;

        switch (exchange.getRequestMethod()) {
            case "POST":
                response = handlePostRequest(exchange);
                break;
            default:
                response = new SimpleHttpResponse(createErrorResponse("Unsupported method"), 405); // Method not allowed
                break;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.statusCode, response.responseBody.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.responseBody.getBytes());
        os.close();
    }

    private SimpleHttpResponse handlePostRequest(HttpExchange exchange) {
        try {
            // Properties
            Properties appProps = ConfigUtil.getProperties();
            String dbUrl = appProps.getProperty("db.url");
            String dbUsername = appProps.getProperty("db.username");
            String dbPassword = appProps.getProperty("db.password");

            if (dbUrl == null || dbUsername == null || dbPassword == null) {
                String errorMessage = "Internal server error: Properties file 'app.properties' must contain 'db.url', 'db.username', and 'db.password'.";
                System.err.println(errorMessage);
                return new SimpleHttpResponse(createErrorResponse(errorMessage), 500);

            }

            // Reading request body
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            isr.close();

            JSONObject jsonBody = new JSONObject(sb.toString());

            List<String> wrongFields = new ArrayList<>();
            List<String> propertyFields = Arrays.asList("username", "email", "password");
            // Check for invalid fields
            for (String requiredFieldString : propertyFields) {
                try {
                    String field = jsonBody.getString(requiredFieldString);
                    if (field.isEmpty()) {
                        wrongFields.add(requiredFieldString);
                    }
                } catch (Exception e) {
                }
            }
            // Check missing fields
            for (String requiredFieldString : propertyFields) {
                try {
                    jsonBody.getString(requiredFieldString);
                } catch (Exception e) {
                    wrongFields.add(requiredFieldString);
                }
            }

            if (!wrongFields.isEmpty()) {
                String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                System.err.println(errorMessage);
                return new SimpleHttpResponse(createErrorResponse(errorMessage), 400);
            }

            // Extract fields
            String username = null;
            String email = null;
            String password = null;

            username = jsonBody.getString("username");
            email = jsonBody.getString("email");
            password = jsonBody.getString("password");

            // Hashing password
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(password.getBytes());
            String hashPasswordText = Base64.getEncoder().encodeToString(messageDigest);

            // Add user to database
            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

                List<String> existingFields = new ArrayList<>();

                // Check if username, email already exists
                ResultSet rs = stmt
                        .executeQuery("select * from users where username='" + username + "' OR email='" + email + "'");
                while (rs.next()) {
                    if (rs.getString("username").equals(username)) {
                        existingFields.add("username");
                    }
                    if (rs.getString("email").equals(email)) {
                        existingFields.add("email");
                    }
                }

                if (!existingFields.isEmpty()) {
                    String errorMessage = "The following fields already exist: " + String.join(", ", existingFields);
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 409);
                }

                stmt.executeUpdate("INSERT INTO users(username, email, password, isEmailVerified)"
                        + " VALUES('" + username + "', '" + email + "', '" + hashPasswordText + "', false" + ")");
                System.out.println("Successfully connected to database and added user");
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", "User successfully registered");
            jsonResponse.put("username", username);
            jsonResponse.put("email", email);

            return new SimpleHttpResponse(jsonResponse.toString(), 201);
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            return new SimpleHttpResponse(createErrorResponse(errorMessage), 500);
        }
    }

    private String createErrorResponse(String errorMessage) {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("error", errorMessage);
        return jsonResponse.toString();
    }

    private static class SimpleHttpResponse {
        public final String responseBody;
        public final int statusCode;

        public SimpleHttpResponse(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

    }
}
