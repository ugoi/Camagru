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

import org.json.JSONArray;
import org.json.JSONObject;

import com.camagru.PropertiesManager;
import com.camagru.PropertyField;
import com.camagru.PropertyFieldsManager;
import com.camagru.RegexUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FeedRequestHandler implements HttpHandler {
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
        res.sendJsonResponse(405, createErrorResponse("Unsupported method"));
    }

    private void handleOptionsRequest(Request req, Response res) {
        res.sendOptionsResponse(res);

    }

    private String createErrorResponse(String errorMessage) {
        return new JSONObject().put("error", errorMessage).toString();
    }

    // Main methods
    private void handleGetRequest(Request req, Response res) {
        CompletableFuture.runAsync(() -> {
            try {
                // Validate input
                List<PropertyField> propertyFields = Arrays.asList(
                        new PropertyField("after", false),
                        new PropertyField("limit", false, RegexUtil.NUMBER_REGEX))

                ;
                PropertyFieldsManager propertyFieldsManager = new PropertyFieldsManager(propertyFields, null);
                List<String> wrongFields = propertyFieldsManager.validationResult(req);

                if (!wrongFields.isEmpty()) {
                    String errorMessage = "The following fields are invalid: " + String.join(", ", wrongFields);
                    res.sendJsonResponse(400, createErrorResponse(errorMessage));
                    return;
                }

                // Extract input
                String lastPictureId = req.getQueryParameter("after", null);
                String limit = req.getQueryParameter("limit", "30");

                // Properties
                PropertiesManager propertiesManager = new PropertiesManager();

                List<String> ids = new ArrayList<>();
                List<String> mimeTypes = new ArrayList<>();

                // Add media to database
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                        propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                        Statement stmt = con.createStatement()) {

                    String query;
                    if (lastPictureId != null && !lastPictureId.isEmpty() && !lastPictureId.equals("null")
                            && !lastPictureId.equals("undefined")) {
                        query = String.format(
                                "SELECT * FROM media " +
                                        "WHERE media_date < (SELECT media_date FROM media WHERE media_uri='%s') " +
                                        "AND media_type='media' " +
                                        "ORDER BY media_date DESC " +
                                        "LIMIT %s",
                                lastPictureId, limit);
                    } else {
                        query = String.format(
                                "SELECT * FROM media " +
                                        "WHERE media_type='media' " +
                                        "ORDER BY media_date DESC " +
                                        "LIMIT %s",
                                limit);
                    }

                    ResultSet rs = stmt.executeQuery(query);

                    while (rs.next()) {
                        String id = rs.getString("media_uri");
                        String mimeType = rs.getString("mime_type");
                        ids.add(id);
                        mimeTypes.add(mimeType);

                    }
                }

                // find first element
                String first;
                String last;
                String next;
                String previous;
                try {
                    first = ids.get(0);
                    previous = "http://camagru.com:8000/api/feed?after=" + first;
                } catch (Exception e) {
                    first = null;
                    previous = null;
                }
                try {
                    last = ids.get(ids.size() - 1);
                    next = "http://camagru.com:8000/api/feed?after=" + last;

                } catch (Exception e) {
                    last = null;
                    next = null;
                }

                JSONObject responseBody = new JSONObject();
                JSONArray responseBodyData = new JSONArray();
                for (String id : ids) {
                    JSONObject contnet = new JSONObject();
                    contnet.put("id", id);
                    contnet.put("downloadUrl", "http://camagru.com:8000/api/serve/media?id=" + id);
                    contnet.put("mime_type", mimeTypes.get(ids.indexOf(id)));
                    responseBodyData.put(contnet);
                }

                responseBody.put("paging", new JSONObject()
                        .put("after", last != null ? last : JSONObject.NULL)
                        .put("before", first != null ? first : JSONObject.NULL)
                        .put("next", next != null ? next : JSONObject.NULL)
                        .put("previous", previous != null ? previous : JSONObject.NULL))
                        .put("data", responseBodyData);

                res.sendJsonResponse(200, responseBody.toString());

            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                res.sendJsonResponse(500, createErrorResponse(errorMessage));
            }
        });

    }

}
