package com.camagru;

import org.json.JSONObject;

public class SimpleHttpResponse {
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
