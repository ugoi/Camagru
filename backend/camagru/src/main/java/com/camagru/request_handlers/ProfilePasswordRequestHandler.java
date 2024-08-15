package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
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
        res.sendResponse(405, createErrorResponse("Unsupported method"));
    }

    private void handleOptionsRequest(Request req, Response res) {
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, *");
        res.sendResponse(204, ""); // No content
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
                res.sendResponse(400, createErrorResponse(errorMessage));
                return;
            }

            String cookieHeader = req.getHeader("Cookie");
            String requestPassword = jsonBody.getString("password");

            // Hashing password
            String hashedPassword = PasswordUtil.hashPassword(requestPassword);

            String jwt = CookieUtil.getCookie(cookieHeader, "token");
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            jwtManager.verifySignature(jwt);
            JSONObject decoded = jwtManager.decodeToken(jwt);
            String sub = decoded.getJSONObject("payload").getString("sub");

            String query = "update users set password='" + hashedPassword + "' where user_id='" + sub + "'";

            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                int rs = stmt.executeUpdate(query);
                if (rs != 0) {
                    System.out.println("Successfully connected to database and updated user");
                    res.sendResponse(200, new JSONObject().put("message", "Password updated successfully").toString());
                } else {
                    String errorMessage = "User not found";
                    System.err.println(errorMessage);
                    res.sendResponse(404, createErrorResponse(errorMessage));
                }

            }
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            res.sendResponse(500, createErrorResponse(errorMessage));
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }
}
