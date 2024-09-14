package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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

                String query = "update users set username='" + requestUsername + "' where user_id='" + sub + "'";

                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {

                    // Check if username already exists
                    {
                        String username = "";
                        String userId = "";
                        ResultSet rs = stmt
                                .executeQuery("select * from users where username='" + requestUsername + "'");
                        while (rs.next()) {
                            if (rs.getString("username").equals(requestUsername)) {
                                username = rs.getString("username");
                                userId = rs.getString("user_id");
                            }
                        }
                        if (!userId.equals(sub) && !username.isEmpty()) {
                            String errorMessage = "Username already exists";
                            System.err.println(errorMessage);
                            res.sendJsonResponse(409, createErrorResponse(errorMessage));
                            return;
                        }
                    }

                    int rs = stmt.executeUpdate(query);
                    if (rs != 0) {
                        res.sendJsonResponse(200,
                                new JSONObject().put("message", "Username updated successfully").toString());
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
