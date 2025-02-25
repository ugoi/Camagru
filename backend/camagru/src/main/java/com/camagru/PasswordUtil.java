package com.camagru;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtil {
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(password.getBytes());
        String hashPasswordText = Base64.getEncoder().encodeToString(messageDigest);
        return hashPasswordText;
    }

    public static void verifyPassword(String password, String hashedPassword) throws NoSuchAlgorithmException {
        String hashedPassword2 = hashPassword(password);

        if (!hashedPassword.equals(hashedPassword2)) {
            throw new RuntimeException("Passwords do not match");
        }
    }

}
