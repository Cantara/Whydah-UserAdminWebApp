package net.whydah.identity.admin;

import net.whydah.sso.util.WhydahUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@RequestMapping("/health")
@Controller
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private WhydahServiceClient tokenServiceClient = new WhydahServiceClient();

    public HealthController() throws IOException {
        tokenServiceClient = new WhydahServiceClient();

    }

    @Produces(MediaType.TEXT_PLAIN + ";charset=utf-8")
    @RequestMapping("/")
    public Response isHealthy() {
        boolean ok = true;
        String statusText = WhydahUtil.getPrintableStatus(tokenServiceClient.getWAS());
        log.trace("isHealthy={}, status: {}", ok, statusText);
        if (ok) {
            return Response.ok("Status OK!\n" + statusText).build();
        } else {
            //Intentionally not returning anything the client can use to determine what's the error for security reasons.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}