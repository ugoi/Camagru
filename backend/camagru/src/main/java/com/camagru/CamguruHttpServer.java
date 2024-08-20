package com.camagru;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.camagru.exceptions.InvalidProperiesException;
import com.camagru.request_handlers.LoginRequestHandler;
import com.camagru.request_handlers.ProfileEmailRequestHandler;
import com.camagru.request_handlers.ProfilePasswordRequestHandler;
import com.camagru.request_handlers.ProfileRequestHandler;
import com.camagru.request_handlers.ProfileUsernameRequestHandler;
import com.camagru.request_handlers.RegisterRequestHandler;
import com.camagru.request_handlers.VideoCompleteRequestHandler;
import com.camagru.request_handlers.VideoDownloadPartRequestHandler;
import com.camagru.request_handlers.VideoDownloadRequestHandler;
import com.camagru.request_handlers.VideoInitiateUploadRequestHandler;
import com.camagru.request_handlers.VideoUploadRequestHandler;
import com.sun.net.httpserver.HttpServer;

public class CamguruHttpServer {
    public static void main(String[] args) throws IOException, InvalidProperiesException {
        // Properties
        PropertiesManager propertiesManager = new PropertiesManager();

        // Database: Create table if not exists
        try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                Statement stmt = con.createStatement();) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users"
                    + "(user_id int PRIMARY KEY AUTO_INCREMENT, username varchar(30), email varchar(30),"
                    + "password varchar(255), isEmailVerified double)");
            System.out.println("Successfully connected to database and created table if it doesn't exist");
        } catch (SQLException e) {
            System.err.println("Error connecting to database in CamguruHttpServer");
            e.printStackTrace();
        }

        // Http Server
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);

        server.createContext("/api/register", new RegisterRequestHandler());
        server.createContext("/api/login", new LoginRequestHandler());
        server.createContext("/api/user/profile", new ProfileRequestHandler());
        server.createContext("/api/user/profile/username", new ProfileUsernameRequestHandler());
        server.createContext("/api/user/profile/email", new ProfileEmailRequestHandler());
        server.createContext("/api/user/profile/password", new ProfilePasswordRequestHandler());
        server.createContext("/api/videos/initiate-upload", new VideoInitiateUploadRequestHandler());
        server.createContext("/api/videos/upload", new VideoUploadRequestHandler());
        server.createContext("/api/videos/complete", new VideoCompleteRequestHandler());
        server.createContext("/api/videos/download", new VideoDownloadRequestHandler());
        server.createContext("/api/videos/download-part", new VideoDownloadPartRequestHandler());



        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8000");
    }
}
