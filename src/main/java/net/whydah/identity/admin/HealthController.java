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
            model.addAttribute("health", "Status OK! \n" + statusText);
        } else {
        }
        return "health";
    }

}