package net.whydah.identity.admin.config;

import net.whydah.sso.config.ApplicationMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Helper methods for reading configurration.
 */
public class AppConfig {
    public static final String IAM_CONFIG_KEY = "IAM_CONFIG";
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    public static Properties readProperties() throws IOException {
        String appMode = ApplicationMode.getApplicationMode();
        Properties properties = loadFromClasspath(appMode);

        String configfilename = System.getProperty(IAM_CONFIG_KEY);
        if(configfilename != null) {
            loadFromFile(properties, configfilename);
        }
        return properties;
    }

    private static Properties loadFromClasspath(String appMode) throws IOException {
        Properties properties = new Properties();
        String propertyfile = String.format("useradminwebapp.%s.properties", appMode);
        log.info("Loading properties from classpath: {}", propertyfile);
        InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(propertyfile);
        if(is == null) {
            log.error("Error reading {} from classpath.", propertyfile);
            System.exit(3);
        }
        properties.load(is);
        return properties;
    }

    private static void loadFromFile(Properties properties, String configfilename) throws IOException {
        File file = new File(configfilename);
        log.info("Overriding defaults from property file {}", file.getAbsolutePath());
        if(file.exists()) {
            properties.load(new FileInputStream(file));
        } else {
            log.error("Config file {} specified by System property {} not found.", configfilename, IAM_CONFIG_KEY);
            System.exit(3);
        }
    }
  
}
