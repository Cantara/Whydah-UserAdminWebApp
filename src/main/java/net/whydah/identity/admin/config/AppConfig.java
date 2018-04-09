package net.whydah.identity.admin.config;

import net.whydah.sso.config.ApplicationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

/**
 * Helper methods for reading configurration.
 */
public class AppConfig {
    public static final String IAM_CONFIG_KEY = "IAM_CONFIG";
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static java.util.Random rand = new SecureRandom();
    private static Properties properties=null;
    
    public static Properties readProperties() throws IOException {
        String appMode = ApplicationMode.getApplicationMode();
        Properties props = loadFromClasspath(appMode);
        String configfilename = System.getProperty(IAM_CONFIG_KEY);
        if(configfilename != null) {
            loadFromFile(props, configfilename);
        }   
        return props;
    }
    
    public AppConfig() {
        if (rand.nextInt(100)>95 || properties==null) {  // reload properties on 5% of the calls or if not loaded
            try {
                properties = readProperties();
              
            } catch (IOException e) {
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }
    }
    
    static {
    	if (properties==null) {  // reload properties on 5% of the calls or if not loaded
            try {
                properties = readProperties();
                
            } catch (IOException e) {
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }
        }
    }
    
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private static Properties loadFromClasspath(String appMode) throws IOException {
        Properties props = new Properties();
        String propertyfile = String.format("useradminwebapp.%s.properties", appMode);
        log.info("Loading properties from classpath: {}", propertyfile);
        InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(propertyfile);
        if(is == null) {
            log.error("Error reading {} from classpath.", propertyfile);
            System.exit(3);
        }
        props.load(is);
        logProperties(props);

        return props;
    }

    private static void logProperties(Properties properties) {
        Set keys = properties.keySet();
        for (Object key : keys) {
            log.info("Property: {}, value {}", key, properties.getProperty((String) key));
        }
    }
    private static void loadFromFile(Properties props, String configfilename) throws IOException {
        File file = new File(configfilename);
        log.info("Overriding defaults from property file {}", file.getAbsolutePath());
        if(file.exists()) {
        	props.load(new FileInputStream(file));
        } else {
            log.error("Config file {} specified by System property {} not found.", configfilename, IAM_CONFIG_KEY);
            System.exit(3);
        }
        logProperties(props);

    }
  
}
