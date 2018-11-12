package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.dao.WhydahUAWAServiceClient;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.sso.whydah.DEFCON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

@RequestMapping("/")
@Controller
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private WhydahUAWAServiceClient tokenServiceClient = new WhydahUAWAServiceClient();
    protected static Properties properties;

    private static String applicationInstanceName;


    public HealthController() throws IOException {
        tokenServiceClient = new WhydahUAWAServiceClient();
        try {
            properties = AppConfig.readProperties();
            this.applicationInstanceName = properties.getProperty("applicationname");
        } catch (Exception e) {
            log.warn("Unable to create WhydahUAWAServiceClient in constructor", e);
        }

    }

    @RequestMapping("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String isHealthy(HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            if (tokenServiceClient.getWAS() == null) {
                model.addAttribute("health", "Initializing");
                return "health";

            }
            boolean ok = tokenServiceClient.getWAS().getDefcon().equals(DEFCON.DEFCON5);

            if (ok && tokenServiceClient.getWAS().checkActiveSession()) {
                log.trace("isHealthy={}, status: {}", ok, WhydahUtil.getPrintableStatus(tokenServiceClient.getWAS()));
                model.addAttribute("health", getHealthTextJson());
                return "health";
            } else {
                log.trace("isHealthy={}, status: {}", ok, WhydahUtil.getPrintableStatus(tokenServiceClient.getWAS()));
                model.addAttribute("health", "isHealthy={false}");
                return "health";
            }
        } catch (Exception e) {
            log.warn("Initializing WhydahUAWAServiceClient", e);
            model.addAttribute("health", "Initializing");
            return "health";

        }

    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + tokenServiceClient.getWAS().getDefcon() + "\",\n" +
                "  \"STS\": \"" + tokenServiceClient.getWAS().getSTS() + "\",\n" +
                "  \"UAS\": \"" + tokenServiceClient.getWAS().getUAS() + "\",\n" +
                "  \"hasApplicationToken\": \"" + Boolean.toString((tokenServiceClient.getWAS().getActiveApplicationTokenId() != null)) + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + Boolean.toString(tokenServiceClient.getWAS().checkActiveSession()) + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + Boolean.toString(tokenServiceClient.getWAS().hasApplicationMetaData()) + "\",\n" +
                "  \"now\": \"" + Instant.now() + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\"" +


                "}\n";
    }

    private static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.identity/UserAdminWebApp/pom.properties";
        URL mavenVersionResource = HealthController.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }

}