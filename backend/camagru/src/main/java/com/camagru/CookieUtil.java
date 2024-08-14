package com.camagru;

public class CookieUtil {
    public static String getCookie(String cookieHeader, String name) {
        String cookieString = "; " + cookieHeader;
        String[] parts = cookieString.split("; " + name + "=");
        if (parts.length == 2) {
            return parts[1].split(";")[0];
        }
        return null;
    }
}
