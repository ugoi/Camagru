package com.camagru.request_handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

public class Request {
    private final HttpExchange exchange;

    public Request(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public String getMethod() {
        return exchange.getRequestMethod();
    }

    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public String getHeader(String headerKey) {
        return exchange.getRequestHeaders().getFirst(headerKey);
    }

    public String getHeader(String headerKey, String valueKey) {
        String header = getHeader(headerKey);
        return HttpUtil.getHeader(header, valueKey);

    }

    public String getQueryParameter(String key) {
        return exchange.getRequestURI().getQuery().split(key + "=")[1].split("&")[0];
    }

    public JSONObject getBodyAsJson() throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        String requestBody = reader.lines().collect(Collectors.joining());
        return new JSONObject(requestBody);
    }

    public byte[] getBody() throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return inputStream.readAllBytes();
        }
    }

    public HashMap<String, byte[]> files() throws IOException {
        HashMap<String, byte[]> jsonParts = new HashMap<String, byte[]>();
        try (InputStream inputStream = exchange.getRequestBody()) {
            // Get boundary from headers
            String boundaryString = getHeader("Content-Type", "boundary");
            jsonParts = HttpUtil.files(inputStream, boundaryString.getBytes());
        }
        return jsonParts;
    }
}
