package net.whydah.identity.admin;

import net.whydah.sso.util.WhydahUtil;
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
import java.util.Properties;

@RequestMapping("/")
@Controller
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private WhydahServiceClient tokenServiceClient = new WhydahServiceClient();

    public HealthController() throws IOException {
        tokenServiceClient = new WhydahServiceClient();

    }

    @RequestMapping("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String isHealthy(HttpServletRequest request, HttpServletResponse response, Model model) {
        boolean ok = true;
        String statusText = WhydahUtil.getPrintableStatus(tokenServiceClient.getWAS());
        log.trace("isHealthy={}, status: {}", ok, statusText);
        if (ok) {
            model.addAttribute("health", getHealthTextJson());
        } else {
        }
        return "health";
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + "DEFCON5" + "\"\n" +
                "  \"hasApplicationToken\": \"" + Boolean.toString((tokenServiceClient.getWAS().getActiveApplicationTokenId() != null)) + "\"\n" +
                "  \"hasValidApplicationToken\": \"" + Boolean.toString(tokenServiceClient.getWAS().checkActiveSession()) + "\"\n" +
                "  \"hasApplicationsMetadata\": \"" + Boolean.toString(tokenServiceClient.getWAS().getApplicationList().size() > 2) + "\"\n" +


                "}\n";
    }

    private static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.identity/UserAdminWebApp/pom.properties";
        URL mavenVersionResource = HealthController.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)";
    }

}