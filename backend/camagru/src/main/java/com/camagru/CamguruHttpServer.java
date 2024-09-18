package com.camagru;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.camagru.exceptions.InvalidProperiesException;
import com.camagru.request_handlers.CommentRequestHandler;
import com.camagru.request_handlers.FeedRequestHandler;
import com.camagru.request_handlers.ForgotPasswordRequestHandler;
import com.camagru.request_handlers.LikesRequestHandler;
import com.camagru.request_handlers.LoginRequestHandler;
import com.camagru.request_handlers.MediaPublishRequestHandler;
import com.camagru.request_handlers.MediaRequestHandler;
import com.camagru.request_handlers.ProfileEmailRequestHandler;
import com.camagru.request_handlers.ProfilePasswordRequestHandler;
import com.camagru.request_handlers.ProfileRequestHandler;
import com.camagru.request_handlers.ProfileUsernameRequestHandler;
import com.camagru.request_handlers.RegisterRequestHandler;
import com.camagru.request_handlers.SendVerificationEmailRequestHandler;
import com.camagru.request_handlers.ServeMediaRequestHandler;
import com.camagru.request_handlers.SettingsRequestHandler;
import com.camagru.request_handlers.VerifyEmailRequestHandler;
import com.camagru.request_handlers.VideoCompleteRequestHandler;
import com.camagru.request_handlers.VideoDownloadPartRequestHandler;
import com.camagru.request_handlers.VideoInitiateUploadRequestHandler;
import com.camagru.request_handlers.VideoUploadPartRequestHandler;
import com.sun.net.httpserver.HttpServer;

public class CamguruHttpServer {
        public static void main(String[] args) throws IOException, InvalidProperiesException {
                // Properties
                PropertiesManager propertiesManager = new PropertiesManager();

                // Database: Create table if not exists
                try (Connection con = DriverManager.getConnection(propertiesManager.getDbUrl(),
                                propertiesManager.getDbUsername(), propertiesManager.getDbPassword());
                                Statement stmt = con.createStatement();) {

                        // Create users table
                        stmt.execute("CREATE TABLE IF NOT EXISTS users"
                                        + "(user_id int PRIMARY KEY AUTO_INCREMENT, username varchar(30), email varchar(30),"
                                        + "password varchar(255), isEmailVerified double, enabledNotifications boolean DEFAULT true)");

                        // Create media table
                        stmt.execute("CREATE TABLE IF NOT EXISTS media"
                                        + "(media_id int PRIMARY KEY AUTO_INCREMENT, user_id int, mime_type varchar(30),"
                                        + "media_description varchar(255), media_type varchar(30), media_uri varchar(36) UNIQUE, container_uri varchar(36) UNIQUE,"
                                        + "media_date datetime, FOREIGN KEY (user_id) REFERENCES users(user_id))");

                        // Create tokens
                        stmt.execute("CREATE TABLE IF NOT EXISTS tokens"
                                        + "(token_id int PRIMARY KEY AUTO_INCREMENT, user_id int, token varchar(36),"
                                        + "type varchar(30), expiry_date datetime, used boolean not null default 0, FOREIGN KEY (user_id) REFERENCES users(user_id))");

                        // Create comments
                        stmt.execute("CREATE TABLE IF NOT EXISTS comments"
                                        + "(comment_id int PRIMARY KEY AUTO_INCREMENT, media_uri varchar(36), user_id int,"
                                        + "comment_title varchar(30), comment_body varchar(255),"
                                        + "comment_date datetime, FOREIGN KEY (media_uri) REFERENCES media(media_uri), FOREIGN KEY (user_id) REFERENCES users(user_id))");

                        // Create likes
                        stmt.execute("CREATE TABLE IF NOT EXISTS likes"
                                        + "(like_id int PRIMARY KEY AUTO_INCREMENT, media_uri varchar(36), user_id int,"
                                        + "reaction varchar(30),"
                                        + " FOREIGN KEY (media_uri) REFERENCES media(media_uri), FOREIGN KEY (user_id) REFERENCES users(user_id))");

                } catch (SQLException e) {
                        System.err.println("Error connecting to database in CamguruHttpServer");
                        e.printStackTrace();
                }

                // Http Server
                HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);

                // Setting up a fixed thread pool
                ExecutorService executor = Executors.newFixedThreadPool(1); // Adjust thread
                // count as needed
                server.setExecutor(executor);

                server.createContext("/api/register", new RegisterRequestHandler());
                server.createContext("/api/login", new LoginRequestHandler());
                server.createContext("/api/user/profile", new ProfileRequestHandler());
                server.createContext("/api/user/profile/username", new ProfileUsernameRequestHandler());
                server.createContext("/api/user/profile/email", new ProfileEmailRequestHandler());
                server.createContext("/api/user/profile/password", new ProfilePasswordRequestHandler());
                server.createContext("/api/videos/initiate-upload", new VideoInitiateUploadRequestHandler());
                server.createContext("/api/videos/upload", new VideoUploadPartRequestHandler());
                server.createContext("/api/videos/complete", new VideoCompleteRequestHandler());
                server.createContext("/api/videos/download-part", new VideoDownloadPartRequestHandler());
                server.createContext("/api/media", new MediaRequestHandler());
                server.createContext("/api/media_publish", new MediaPublishRequestHandler());
                server.createContext("/api/serve/media", new ServeMediaRequestHandler());
                server.createContext("/api/feed", new FeedRequestHandler());
                server.createContext("/api/forgot-password", new ForgotPasswordRequestHandler());
                server.createContext("/api/send-verification-email", new SendVerificationEmailRequestHandler());
                server.createContext("/api/verify-email", new VerifyEmailRequestHandler());
                server.createContext("/api/comments", new CommentRequestHandler());
                server.createContext("/api/likes", new LikesRequestHandler());
                server.createContext("/api/settings", new SettingsRequestHandler());

                // server.setExecutor(null); // creates a default executor
                server.start();
                System.out.println("Server started on port 8000");
        }
}
