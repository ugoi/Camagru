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

public class LoginRequestHandler implements HttpHandler {
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
            List<String> propertyFields = Arrays.asList("password");
            // // Check for invalid fields
            // for (String requiredFieldString : propertyFields) {
            // try {
            // String field = jsonBody.getString(requiredFieldString);
            // if (field.isEmpty()) {
            // wrongFields.add(requiredFieldString);
            // }
            // } catch (Exception e) {
            // }
            // }
            // Check missing fields
            for (String requiredFieldString : propertyFields) {
                try {
                    jsonBody.getString(requiredFieldString);
                } catch (Exception e) {
                    wrongFields.add(requiredFieldString);
                }
            }
            // Special case: username or email
            try {
                jsonBody.getString("username");
            } catch (Exception e) {
                try {
                    jsonBody.getString("email");
                } catch (Exception e1) {
                    wrongFields.add("username or email");
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

            try {
                username = jsonBody.getString("username");
            } catch (Exception e) {
                username = null;
            }

            try {
                email = jsonBody.getString("email");
            } catch (Exception e) {
                email = null;
            }

            // Hashing password
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(jsonBody.getString("password").getBytes());
            String hashPasswordText = Base64.getEncoder().encodeToString(messageDigest);

            // Get user password from database
            String query = null;
            if (username != null) {
                query = "select * from users where username='" + username + "'";
            } else if (email != null) {
                query = "select * from users where email='" + email + "'";
            }

            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

                // Get user from database
                String userName = null;
                String userEmail = null;
                String userPassword = null;

                ResultSet rs = stmt
                        .executeQuery(query);
                if (rs.next()) {
                    userName = rs.getString("username");
                    userEmail = rs.getString("email");
                    userPassword = rs.getString("password");
                } else {
                    String errorMessage = "User not found";
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 404);
                }

                // Validate password
                if (!hashPasswordText.equals(userPassword)) {
                    System.out.println(hashPasswordText);
                    System.out.println(userPassword);

                    String errorMessage = "Invalid password";
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 401);
                }

                System.out.println("Successfully connected to database and added user");
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", "User successfully logged in");
            jsonResponse.put("token", "exampleToken123");

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
