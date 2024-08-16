package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONObject;

import com.camagru.CookieUtil;
import com.camagru.JwtManager;
import com.camagru.PropertiesManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ProfileRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Request req = new Request(exchange);
        Response res = new Response(exchange);

        switch (req.getMethod()) {
            case "GET":
                handleGetRequest(req, res);
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
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, *");
        res.sendResponse(204, ""); // No content
    }

    private void handleGetRequest(Request req, Response res) {
        try {
            // Properties
            PropertiesManager propertiesManager = new PropertiesManager();
            String cookieHeader = req.getHeader("Cookie");

            if (cookieHeader == null || cookieHeader.isEmpty()) {
                res.sendResponse(400, createErrorResponse("Request headers not supported"));
                return;
            }

            // GET COOKIE BY NAME
            String jwt = CookieUtil.getCookie(cookieHeader, "token");
            JwtManager jwtManager = new JwtManager(propertiesManager.getJwtSecret());
            jwtManager.verifySignature(jwt);
            JSONObject decoded = jwtManager.decodeToken(jwt);

            String userId = decoded.getJSONObject("payload").getString("sub");

            // Get user details from database
            String query = "select * from users where user_id='" + userId + "'";

            String userName = null;
            String userEmail = null;
            try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                    propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                    Statement stmt = con.createStatement()) {

                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    userName = rs.getString("username");
                    userEmail = rs.getString("email");
                } else {
                    String errorMessage = "User not found";
                    System.err.println(errorMessage);
                    res.sendResponse(404, createErrorResponse(errorMessage));
                    return;
                }

                System.out.println("Successfully connected to database and retrieved user data");
            }

            JSONObject jsonResponse = new JSONObject()
                    .put("username", userName)
                    .put("email", userEmail);
            res.sendResponse(200, jsonResponse.toString());
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