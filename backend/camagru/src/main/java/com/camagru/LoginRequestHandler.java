package com.camagru;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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
        SimpleHttpResponse response;

        switch (exchange.getRequestMethod()) {
            case "POST":
                response = handlePostRequest(exchange);
                break;
            default:
                response = new SimpleHttpResponse(createErrorResponse("Unsupported method"), 405); // Method not
                                                                                                   // allowed
                break;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (response.responseHeaders != null) {
            response.responseHeaders.keySet()
                    .forEach(key -> exchange.getResponseHeaders().set(key, response.responseHeaders.getString(key)));
        }
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

            // Get JSON body
            JSONObject jsonBody = new JSONObject(sb.toString());

            // Validate fields
            List<PropertyField> propertyFields = Arrays.asList(
                    new PropertyField("username", true),
                    new PropertyField("password", true, RegexUtil.PASSWORD_REGEX));
            PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields);
            List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);

            if (!wrongFields.isEmpty()) {
                String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                System.err.println(errorMessage);
                return new SimpleHttpResponse(createErrorResponse(errorMessage), 400);
            }

            // Extract fields
            String username = jsonBody.getString("username");

            // Hashing password
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(jsonBody.getString("password").getBytes());
            String hashPasswordText = Base64.getEncoder().encodeToString(messageDigest);

            // Get user password from database
            String query = "select * from users where username='" + username + "'" + " OR email='" + username + "'";

            String userName = null;
            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

                // Get user from database
                String userPassword = null;

                ResultSet rs = stmt
                        .executeQuery(query);
                if (rs.next()) {
                    userName = rs.getString("username");
                    userPassword = rs.getString("password");
                } else {
                    String errorMessage = "User not found";
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 404);
                }

                // Validate password
                if (!hashPasswordText.equals(userPassword)) {
                    String errorMessage = "Invalid password";
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 401);
                }

                System.out.println("Successfully connected to database and added user");
            }

            JwtManager jwtManager = new JwtManager("secret");
            String token = jwtManager.createToken(userName);

            JSONObject jsonResBody = new JSONObject()
                    .put("message", "User successfully logged in");

            JSONObject jsonResHeaders = new JSONObject()
                    .put("Set-Cookie", "token=" + token + "; HttpOnly");

            return new SimpleHttpResponse(jsonResBody.toString(), 201, jsonResHeaders);
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
        public JSONObject responseHeaders = null;

        public SimpleHttpResponse(String responseBody, int statusCode) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }

        public SimpleHttpResponse(String responseBody, int statusCode, JSONObject responseHeaders) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.responseHeaders = responseHeaders;
        }

    }
}
