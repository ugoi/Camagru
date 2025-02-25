package com.camagru;

import com.camagru.request_handlers.HttpUtil;

public class CookieUtil {
    public static String getCookie(String headerKey, String valueKey) {
        return HttpUtil.getHeader(headerKey, valueKey);
    }
}
