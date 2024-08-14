package com.camagru;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

public class ProfileRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpResponse response;

        switch (exchange.getRequestMethod()) {
            case "GET":
                response = handleGetRequest(exchange);
                break;
            case "OPTIONS":
                response = handleOptionsRequest(exchange);
                break;
            default:
                response = new SimpleHttpResponse(createErrorResponse("Unsupported method"), 405);
                break;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://127.0.0.1:5500");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                    "Content-Type, *");
        }
        if (response.responseHeaders != null) {
            response.responseHeaders.keySet()
                    .forEach(key -> exchange.getResponseHeaders().set(key, response.responseHeaders.getString(key)));
        }

        exchange.sendResponseHeaders(response.statusCode, response.responseBody.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.responseBody.getBytes());
        os.close();
    }

    private SimpleHttpResponse handleOptionsRequest(HttpExchange exchange) {
        return new SimpleHttpResponse("", 204);
    }

    private SimpleHttpResponse handleGetRequest(HttpExchange exchange) {
        try {
            // Properties
            Properties appProps = ConfigUtil.getProperties();
            String dbUrl = appProps.getProperty("db.url");
            String dbUsername = appProps.getProperty("db.username");
            String dbPassword = appProps.getProperty("db.password");
            String jwtSecret = appProps.getProperty("jwt.secret");
            Headers requestHeaders = exchange.getRequestHeaders();

            if (dbUrl == null || dbUsername == null || dbPassword == null || jwtSecret == null) {
                String errorMessage = "Internal server error: Properties file 'app.properties' must contain 'db.url', 'db.username', and 'db.password'.";
                System.err.println(errorMessage);
                return new SimpleHttpResponse(createErrorResponse(errorMessage), 500);

            }

            if (requestHeaders == null || requestHeaders.isEmpty()) {
                return new SimpleHttpResponse(createErrorResponse("Request headers not supported"), 400);
            }

            requestHeaders.forEach((key, values) -> {
                System.out.println(key + ": " + values);
            });

            String cookieHeder = requestHeaders.getFirst("Cookie");

            // GET COOKIE BY NAME

            String jwt = CookieUtil.getCookie(cookieHeder, "token");

            JwtManager jwtManager = new JwtManager(jwtSecret);
            jwtManager.verifySignature(jwt);
            JSONObject decoded = jwtManager.decodeToken(jwt);

            String userid = decoded.getJSONObject("payload").getString("sub");

            // Get user password from database
            String query = "select * from users where user_id='" + userid + "'";

            String userName = null;
            String userEmail = null;
            String userPassword = null;
            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

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

                System.out.println("Successfully connected to database and added user");
            }

            JSONObject jsonResponse = new JSONObject()
                    .put("username", userName)
                    .put("email", userEmail);
            return new SimpleHttpResponse(jsonResponse.toString(), 200);
        } catch (Exception e) {
            String errorMessage = "Internal server error: " + e.getMessage();
            return new SimpleHttpResponse(createErrorResponse(errorMessage), 500);
        }
    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
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
