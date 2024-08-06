package com.camagru;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
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

        String response;
        int statusCode = 200;

        switch (exchange.getRequestMethod()) {
            case "POST":
                response = handlePostRequest(exchange);
                break;
            default:
                response = "Unsupported method";
                statusCode = 405;
                break;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String handlePostRequest(HttpExchange exchange) throws IOException {
        // Properties
        Properties appProps = ConfigUtil.getProperties();
        String dbUrl = appProps.getProperty("db.url");
        String dbUsername = appProps.getProperty("db.username");
        String dbPassword = appProps.getProperty("db.password");

        if (dbUrl == null || dbUsername == null || dbPassword == null) {
            throw new IllegalArgumentException(
                    "Properties file 'app.properties' must contain 'db.url', 'db.username', and 'db.password'.");
        }

        // Example of reading request body
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
        String email = jsonBody.getString("email");
        String password = jsonBody.getString("password");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(password.getBytes());
            String hashPasswordText = Base64.getEncoder().encodeToString(messageDigest);

            // Add user to database
            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

                ResultSet rs = stmt.executeQuery("select * from users where email='" + email + "'");
                Boolean emailExists = rs.next();

                if (emailExists) {
                    throw new EmailAlreadyExistsException("The email address " + email + " is already registered.");
                }

                stmt.executeUpdate("INSERT INTO users(email, password, isEmailVerified)"
                        + " VALUES('" + email + "', '" + hashPasswordText + "', false" + ")");
                System.out.println("Successfully connected to database and added user");
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("message", "User registered successfully");
            jsonResponse.put("email", email);

            return jsonResponse.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }
}
