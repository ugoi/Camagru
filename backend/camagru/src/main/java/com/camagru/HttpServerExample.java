package com.camagru;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.sun.net.httpserver.HttpServer;

public class HttpServerExample {
    public static void main(String[] args) throws IOException {
        // Properties
        Properties appProps = ConfigUtil.getProperties();
        String url = appProps.getProperty("db.url");
        String username = appProps.getProperty("db.username");
        String password = appProps.getProperty("db.password");

        if (url == null || username == null || password == null) {
            throw new IllegalArgumentException(
                    "Properties file 'app.properties' must contain 'db.url', 'db.username', and 'db.password'.");
        }

        // Database: Create table if not exists
        try (Connection con = DriverManager.getConnection(url,
                username, password);
                Statement stmt = con.createStatement();) {

            stmt.execute("CREATE TABLE IF NOT EXISTS users"
                    + "(user_id int PRIMARY KEY AUTO_INCREMENT, username varchar(30), email varchar(30),"
                    + "password varchar(255), isEmailVerified double)");
            System.out.println("Successfully connected to database and created table if it doesn't exist");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Http Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/api/example", new ExampleRequestHandler());
        server.createContext("/api/register", new RegisterRequestHandler());
        server.createContext("/api/login", new LoginRequestHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8000");
    }
}
