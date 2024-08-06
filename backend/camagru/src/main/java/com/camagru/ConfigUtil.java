package com.camagru;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {
    public static Properties getProperties() throws IOException {
        Properties appProps = new Properties();
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("app.properties");

        if (stream == null) {
            throw new FileNotFoundException("Properties file 'app.properties' not found in the resources folder.");
        }

        appProps.load(stream);

        return appProps;
    }
}
