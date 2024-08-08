package com.camagru;

public class RegexUtil {
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,30}$";
    public static final String EMAIL_REGEX = "^(.+)@(.+)$";
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$";
}
