package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.usertoken.TokenServiceClient;
import net.whydah.identity.admin.usertoken.UserTokenXpathHelper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
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
import java.util.MissingResourceException;
import java.util.Properties;

@Controller
public class UserAdminController {
    private static final Logger logger = LoggerFactory.getLogger(UserAdminController.class);
    public static final String USERTICKET_KEY = "userticket";
    private static final String REDIRECT_URI_KEY = "redirectURI";
    private static final int MIN_USERTICKET_LENGTH = 7;
    private static final int MIN_USER_TOKEN_LENGTH = 11;
    private static final int MIN_USERTOKEN_ID_LENGTH = 4;
    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
    private static String userTokenId = null;

    private TokenServiceClient tokenServiceClient = new TokenServiceClient();

    private String MY_APP_TYPE = "myapp";
    private final String MY_APP_URI;
    private final String LOGIN_SERVICE_REDIRECT;
    private final String LOGOUT_SERVICE;
    private final String LOGOUT_SERVICE_REDIRECT;
    private final HttpClient httpClient;
    private final boolean STANDALONE;
    Properties properties = AppConfig.readProperties();

    public UserAdminController() throws IOException {
        STANDALONE = Boolean.valueOf(properties.getProperty("standalone"));
        MY_APP_URI = properties.getProperty("myuri");
        MY_APP_TYPE = properties.getProperty("myapp");
        if (MY_APP_TYPE == null || MY_APP_TYPE.isEmpty()) {
            MY_APP_TYPE = "useradmin"; //TODO To be fixed in https://github.com/altran/Whydah-UserAdminWebApp/issues/44
        }

        LOGIN_SERVICE_REDIRECT = "redirect:" + properties.getProperty("logonservice") + "login?" + REDIRECT_URI_KEY + "=" + MY_APP_URI;
        LOGOUT_SERVICE = properties.getProperty("logonservice") + "logout?" + REDIRECT_URI_KEY + "=" + MY_APP_URI;
        LOGOUT_SERVICE_REDIRECT = "redirect:" + LOGOUT_SERVICE;

        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

        StringBuilder strb = new StringBuilder("Initialized UserAdminController \n");
        strb.append("\n- Standalone=").append(STANDALONE);
        strb.append("\n- MY_APP_URI=").append(MY_APP_URI);
        strb.append("\n- LOGIN_SERVICE_REDIRECT=").append(LOGIN_SERVICE_REDIRECT);
        strb.append("\n- LOGOUT_SERVICE_REDIRECT=").append(LOGOUT_SERVICE_REDIRECT);
        logger.debug(strb.toString());
    }

    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    @RequestMapping("/")
    public String myapp(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setContentType(HTML_CONTENT_TYPE);
        if (STANDALONE) {
            logger.info("Log on OK. - Standalone mode selected, so no authentication.");
            addModelParams(model, "Unauthorized", "Unknown User");
            return MY_APP_TYPE;
        }

        String userTicket = request.getParameter(USERTICKET_KEY);
        if (userTokenId == null && userTicket != null && userTicket.length() > MIN_USERTICKET_LENGTH) {
            String userTokenXml;
            try {
                userTokenXml = tokenServiceClient.getUserTokenByUserTicket(userTicket);
                logger.debug("Logon with userticket: userTokenXml={}", userTokenXml);

                if (userTokenXml == null || userTokenXml.length() < MIN_USER_TOKEN_LENGTH) {
                    logger.trace("UserTokenXML null or too short to be useful. Redirecting to login.");
                    CookieManager.clearUserTokenCookie(request, response);
                    return LOGIN_SERVICE_REDIRECT;
                }

                userTokenId = UserTokenXpathHelper.getUserTokenIdFromUserTokenXML(userTokenXml);

                if (!UserTokenXpathHelper.hasUserAdminRight(userTokenXml)) {
                    logger.trace("Got user from userTokenXml, but wrong access rights. Redirecting to logout.");
                    userTokenId = null;
                    return LOGOUT_SERVICE_REDIRECT;
                }

                logger.info("Logon OK. UserTokenXML obtained with user ticket contained a valid admin user. userTokenId={}", userTokenId);
                addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml));
                Integer tokenRemainingLifetimeSeconds = TokenServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, response);
                return MY_APP_TYPE;
            } catch (MissingResourceException mre) {
                logger.trace("getUserTokenByUserTicket failed. The ticked might have already been used. Checking cookie. MissingResourceException=", mre.getMessage());
            }
        }



        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        if (userTokenIdFromCookie == null) {
            CookieManager.clearUserTokenCookie(request, response);
            userTokenId = null;
            return LOGIN_SERVICE_REDIRECT;
        }

        String userTokenXml;
        try {
            userTokenXml = tokenServiceClient.getUserTokenFromUserTokenId(userTokenIdFromCookie);
            if (userTokenXml.length() < MIN_USER_TOKEN_LENGTH) {
                CookieManager.clearUserTokenCookie(request, response);
                logger.trace("UserTokenXML null or too short to be useful. Redirecting to login.");
                userTokenId = null;
                return LOGIN_SERVICE_REDIRECT;
            }
        } catch (RuntimeException mre) {
            CookieManager.clearUserTokenCookie(request, response);
            logger.trace("{}. Redirecting to login.", userTokenIdFromCookie, mre.getMessage());
            userTokenId = null;
            return LOGIN_SERVICE_REDIRECT;
        }

        if (!UserTokenXpathHelper.hasUserAdminRight(userTokenXml)) {
            logger.trace("Got user from userTokenXml, but wrong access rights. Redirecting to logout.");
            CookieManager.clearUserTokenCookie(request, response);
            userTokenId = null;
            return LOGOUT_SERVICE_REDIRECT;
        }

        userTokenId = UserTokenXpathHelper.getUserTokenIdFromUserTokenXML(userTokenXml);
        addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml));
        Integer tokenRemainingLifetimeSeconds = TokenServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
        CookieManager.updateUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);

        logger.info("Logon OK. userTokenIdFromUserTokenXml={}", userTokenId);
        return MY_APP_TYPE;
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Model model) {
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        //model.addAttribute("redirectURI", MY_APP_URI);
        userTokenId = null;
        logger.trace("Logout was called with userTokenIdFromCookie={}. Redirecting to {}.", userTokenIdFromCookie, LOGOUT_SERVICE_REDIRECT);
        CookieManager.clearUserTokenCookie(request, response);
        return LOGOUT_SERVICE_REDIRECT;
    }


    private void addModelParams(Model model, String userTokenXml, String realName) {
        model.addAttribute("token", userTokenXml);
        model.addAttribute("realName", realName);
        //model.addAttribute("logOutUrl", LOGOUT_SERVICE);
        model.addAttribute("logOutUrl", MY_APP_URI + "logout");

        String baseUrl = "/useradmin/" + tokenServiceClient.getMyAppTokenId() + "/" + tokenServiceClient.getMyUserTokenId() + "/";
        model.addAttribute("baseUrl", baseUrl);
    }


}
