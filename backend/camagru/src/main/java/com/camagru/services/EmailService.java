package com.camagru.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.camagru.PropertiesManager;
import com.camagru.exceptions.InvalidProperiesException;

public class EmailService {
    private String apiKey;
    private String senderEmail;
    private String senderName;

    public EmailService(String apiKey, String senderEmail, String senderName)
            throws FileNotFoundException, InvalidProperiesException, IOException {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    // Factory method to create an EmailService object
    public static EmailService create() throws FileNotFoundException, InvalidProperiesException, IOException {
        PropertiesManager propertiesManager = new PropertiesManager();
        String senderEmail = propertiesManager.getBrevoSenderEmail();
        String senderName = propertiesManager.getBrevoSenderName();
        String apiKey = propertiesManager.getBrevoApiKey();
        return new EmailService(apiKey, senderEmail, senderName);
    }

    public void send(String name, String email, String subject, String htmlContent) throws IOException {
        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("api-key",
                apiKey);
        httpConn.setRequestProperty("content-type", "application/json");

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());

        // Creating the JSON Object for "sender"
        JSONObject sender = new JSONObject();
        sender.put("name", senderName);
        sender.put("email", senderEmail);

        // Creating the JSON Object for the "to" array
        JSONObject recipient = new JSONObject();
        recipient.put("email", email);
        recipient.put("name", name);

        // Adding the recipient to the "to" array
        JSONArray toArray = new JSONArray();
        toArray.put(recipient);

        // Creating the main JSON object
        JSONObject emailObject = new JSONObject();
        emailObject.put("sender", sender);
        emailObject.put("to", toArray);
        emailObject.put("subject", subject);
        emailObject.put("htmlContent",
                htmlContent);

        writer.write(emailObject.toString());
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        try (Scanner s = new Scanner(responseStream).useDelimiter("\\A")) {
            String response = s.hasNext() ? s.next() : "";
            System.out.println(response);
        }
    }
}
