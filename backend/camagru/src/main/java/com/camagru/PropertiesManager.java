package com.camagru;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.camagru.exceptions.InvalidProperiesException;

/**
 * The PropertiesManager class is responsible for loading and validating the
 * properties from the
 * app.properties file.
 */
public class PropertiesManager {
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String jwtSecret;
    private String brevoApiKey;
    private String brevoSenderEmail;
    private String brevoSenderName;

    /**
     * Constructor for the PropertiesManager class.
     * Implicitly loads the properties from the app.properties file.
     * 
     * @throws InvalidProperiesException
     *                                   If the properties file has invalid
     *                                   properties. For example, if the propeties
     *                                   are missing or have invalid values.
     * @throws IOException
     *                                   If an I/O error occurs while reading the
     *                                   properties file.
     * @throws FileNotFoundException
     *                                   If the properties file is not found.
     */
    public PropertiesManager() throws InvalidProperiesException, IOException, FileNotFoundException {
        loadProperties();
    }

    /**
     * Load the properties from the app.properties file.
     * 
     * @throws InvalidProperiesException
     *                                   If the properties file has invalid
     *                                   properties. For example, if the propeties
     *                                   are missing or have invalid values.
     * @throws IOException
     *                                   If an I/O error occurs while reading the
     *                                   properties file.
     * @throws FileNotFoundException
     *                                   If the properties file is not found.
     */
    public void loadProperties() throws InvalidProperiesException, IOException, FileNotFoundException {
        // Properties
        Properties appProps = getProperties();
        dbUrl = appProps.getProperty("db.url");
        dbUsername = appProps.getProperty("db.username");
        dbPassword = appProps.getProperty("db.password");
        jwtSecret = appProps.getProperty("jwt.secret");
        brevoApiKey = appProps.getProperty("brevo.api.key");
        brevoSenderEmail = appProps.getProperty("brevo.sender.email");
        brevoSenderName = appProps.getProperty("brevo.sender.name");

        if (dbUrl == null || dbUsername == null || dbPassword == null || jwtSecret == null) {
            throw new InvalidProperiesException("Properties file 'app.properties' is missing required properties",
                    null);
        }

    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public String getBrevoApiKey() {
        return brevoApiKey;
    }

    public String getBrevoSenderEmail() {
        return brevoSenderEmail;
    }

    public String getBrevoSenderName() {
        return brevoSenderName;
    }

    /**
     * Load the properties from the app.properties file.
     *
     * @return A Properties object.
     */
    public static Properties getProperties() throws FileNotFoundException, IOException {
        Properties appProps = new Properties();
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("app.properties");

        if (stream == null) {
            throw new FileNotFoundException("Properties file 'app.properties' not found in the resources folder.");
        }

        appProps.load(stream);

        return appProps;
    }

}
