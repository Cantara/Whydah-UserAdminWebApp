package net.whydah.identity.admin;

import com.sun.jersey.api.client.Client;
import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.util.WhydahUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;

@Controller
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private WhydahServiceClient tokenServiceClient = new WhydahServiceClient();

    public HealthController() throws IOException {
        tokenServiceClient = new WhydahServiceClient();

    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
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