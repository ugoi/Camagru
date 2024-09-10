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

import org.json.JSONObject;

import com.camagru.PropertiesManager;
import com.camagru.services.SendVerificationEmailService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SendVerificationEmailRequestHandler implements HttpHandler {
  @Override
  public void handle(HttpExchange exchange) throws IOException {

    System.out.println("Media request received");

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
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type");
    res.sendJsonResponse(204, ""); // No content
  }

  private String createErrorResponse(String errorMessage) {
    return new JSONObject().put("error", errorMessage).toString();
  }

  private void handlePostRequest(Request req, Response res) {
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
      myStmt.setString(3, "email_validation");
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
      SendVerificationEmailService.sendForVerifyingEmail(email);
    } catch (Exception e) {
      String errorMessage = "Failed to send email: " + e.getMessage();
      res.sendJsonResponse(500, createErrorResponse(errorMessage));
      e.printStackTrace();
      return;
    }

    res.sendResponse(200, "Successfully sent email with password reset link");
  }
}
