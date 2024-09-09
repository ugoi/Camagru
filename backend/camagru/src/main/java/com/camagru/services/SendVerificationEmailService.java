package com.camagru.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

import com.camagru.PropertiesManager;

public class SendVerificationEmailService {
    public static void sendForVerifyingEmail(String email) throws Exception {
        // Send verification email

        PropertiesManager propertiesManager = new PropertiesManager();

        String username2 = "";

        String token = UUID.randomUUID().toString();
        Timestamp expiryDate = new Timestamp(System.currentTimeMillis() + 36000000);
        try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                Statement stmt = con.createStatement()) {

            // Check if email exists in the database
            String preparedStmt = "SELECT * FROM users WHERE email = ?";
            PreparedStatement myStmt;
            myStmt = con.prepareStatement(preparedStmt);
            myStmt.setString(1, email);
            ResultSet rs = myStmt.executeQuery();

            String userId = null;
            while (rs.next()) {
                userId = rs.getString("user_id");
                username2 = rs.getString("username");
            }
            if (userId == null) {
                throw new Exception("Email does not exist in the database");
            }
            // If email exists, generate a reset password token and store it in the database
            preparedStmt = "INSERT INTO tokens (user_id, token, type, expiry_date, used) VALUES (?, ?, ?, ?, ?)";
            myStmt = con.prepareStatement(preparedStmt);
            myStmt.setString(1, userId);
            myStmt.setString(2, token);
            myStmt.setString(3, "email_validation");
            myStmt.setTimestamp(4, expiryDate);
            myStmt.setBoolean(5, false);
            myStmt.executeUpdate();
        }

        EmailService service = EmailService.create();
        String emailTemplate = """
                <html>
                <head>
                  <style>
                    body {
                      font-family: Arial, sans-serif;
                      color: #333;
                      line-height: 1.6;
                    }
                    .container {
                      padding: 20px;
                      background-color: #f4f4f4;
                      border-radius: 10px;
                      max-width: 600px;
                      margin: 0 auto;
                    }
                    .btn {
                      display: inline-block;
                      padding: 10px 20px;
                      font-size: 16px;
                      color: #fff;
                      background-color: #007bff;
                      text-decoration: none;
                      border-radius: 5px;
                    }
                    .footer {
                      margin-top: 20px;
                      font-size: 12px;
                      color: #777;
                    }
                  </style>
                </head>
                <body>
                  <div class='container'>
                    <p>Hello,</p>
                    <p>Thank you for registering with Camagru! Please click the button below to verify your email address:</p>
                    <p>
                      <a href='%s' class='btn'>Verify Email</a>
                    </p>
                    <p>If you did not sign up for this account, you can safely ignore this email.</p>
                    <p>Thank you, <br> The Camagru Team</p>
                    <div class='footer'>
                      <p>If you have any issues, contact our support team at support@camagru.xyz.</p>
                    </div>
                  </div>
                </body>
                </html>
                """;

        // Send email with reset password link
        String resetLink = "http://127.0.0.1:5500/email-validation/?token=" + token;
        String formattedEmail = String.format(emailTemplate, resetLink);
        service.send(username2, email, "Verify Email", formattedEmail);
    }
}
