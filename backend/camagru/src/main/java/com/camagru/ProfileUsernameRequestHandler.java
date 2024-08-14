package com.camagru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

public class ProfileUsernameRequestHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SimpleHttpResponse response;

        switch (exchange.getRequestMethod()) {
            case "PUT":
                response = handlePutRequest(exchange);
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
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
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

    private SimpleHttpResponse handlePutRequest(HttpExchange exchange) {
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
            {
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("username", true, RegexUtil.USERNAME_REGEX));
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields);
                List<String> wrongFields = propertyFieldsManager.getWrongFields(jsonBody);
                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 400);
                }
            }

            String cookieHeder = requestHeaders.getFirst("Cookie");

            System.out.println("Request Body");
            System.out.println(jsonBody.toString());

            // Extract fileds from JSON body
            String reqestUsername = jsonBody.getString("username");

            // GET COOKIE BY NAME
            String jwt = CookieUtil.getCookie(cookieHeder, "token");
            JwtManager jwtManager = new JwtManager(jwtSecret);
            jwtManager.verifySignature(jwt);
            JSONObject decoded = jwtManager.decodeToken(jwt);

            String sub = decoded.getJSONObject("payload").getString("sub");

            // Get user password from database
            // String query = "select * from users where username='" + sub + "'";
            String query = "update users set username='" + reqestUsername + "' where user_id='" + sub + "'";

            try (Connection con = DriverManager.getConnection(dbUrl,
                    dbUsername, dbPassword);
                    Statement stmt = con.createStatement();) {
                ;

                int rs = stmt.executeUpdate(query);

                if (rs != 0) {
                    System.out.println("Successfully connected to database and updated user");
                } else {
                    String errorMessage = "User not found";
                    System.err.println(errorMessage);
                    return new SimpleHttpResponse(createErrorResponse(errorMessage), 404);
                }

                System.out.println("Successfully connected to database and updated user");
            }

            JSONObject jsonResponse = new JSONObject()
                    .put("message", "Username updated successfully");

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
