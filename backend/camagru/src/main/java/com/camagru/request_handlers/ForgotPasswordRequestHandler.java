package com.camagru.request_handlers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.camagru.PropertiesManager;
import com.camagru.services.EmailService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ForgotPasswordRequestHandler implements HttpHandler {
  @Override
  public void handle(HttpExchange exchange) throws IOException {

    Request req = new Request(exchange);
    Response res = new Response(exchange);

    switch (req.getMethod()) {
      case "POST":
        handlePostRequest(req, res);
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

  private void handlePostRequest(Request req, Response res) {
    CompletableFuture.runAsync(() -> {
      String email;
      try {
        email = req.getQueryParameter("email");
      } catch (Exception e) {
        res.sendJsonResponse(400, createErrorResponse("Invalid request: email is required"));
        return;
      }

      PropertiesManager propertiesManager;
      try {
        propertiesManager = new PropertiesManager();
      } catch (Exception e) {
        String errorMessage = "Failed to load properties file";
        res.sendJsonResponse(500, createErrorResponse(errorMessage));
        e.printStackTrace();
        return;
      }

      String username = "";

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
          username = rs.getString("username");
        }
        if (userId == null) {
          String errorMessage = "User not found";
          res.sendJsonResponse(404, createErrorResponse(errorMessage));
          return;
        }
        // If email exists, generate a reset password token and store it in the database
        preparedStmt = "INSERT INTO tokens (user_id, token, type, expiry_date, used) VALUES (?, ?, ?, ?, ?)";
        myStmt = con.prepareStatement(preparedStmt);
        myStmt.setString(1, userId);
        myStmt.setString(2, token);
        myStmt.setString(3, "password_reset");
        myStmt.setTimestamp(4, expiryDate);
        myStmt.setBoolean(5, false);
        myStmt.executeUpdate();
      } catch (SQLException e) {
        String errorMessage = "Database error: " + e.getMessage();
        res.sendJsonResponse(500, createErrorResponse(errorMessage));
        e.printStackTrace();
        return;
      }

      try {
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
                <p>We received a request to reset your password. Please click the button below to reset it:</p>
                <p>
                  <a href='%s' class='btn'>Reset Password</a>
                </p>
                <p>If you did not request this change, you can safely ignore this email.</p>
                <p>Thank you, <br> The Camagru Team</p>
                <div class='footer'>
                  <p>If you have any issues, contact our support team at support@camagru.xyz.</p>
                </div>
              </div>
            </body>
            </html>
            """;

        // Send email with reset password link
        String resetLink = "http://127.0.0.1:5500/password-reset/?token=" + token;
        String formattedEmail = String.format(emailTemplate, resetLink);
        service.send(username, email, "Reset Password", formattedEmail);
      } catch (Exception e) {
        String errorMessage = "Failed to send email: " + e.getMessage();
        res.sendJsonResponse(500, createErrorResponse(errorMessage));
        e.printStackTrace();
        return;
      }

      res.sendResponse(200, "Successfully sent email with password reset link");
    });
  }
}
