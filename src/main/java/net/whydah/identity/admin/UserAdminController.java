package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.dao.ConstantValue;
import net.whydah.identity.admin.dao.SessionUserAdminDao;
import net.whydah.identity.admin.usertoken.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
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
import java.util.Properties;

@Controller
public class UserAdminController {
    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);
//    public static final String USERTICKET_KEY = "userticket";
//    private static final String REDIRECT_URI_KEY = "redirectURI";
//    private static final int MIN_USERTICKET_LENGTH = 7;
//    private static final int MIN_USER_TOKEN_LENGTH = 11;
//    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
//    private static String userTokenId = null;
//
//    private WhydahServiceClient tokenServiceClient = new WhydahServiceClient();
//
//    private String MY_APP_TYPE = "myapp";
//    private final String MY_APP_URI;
//    private final String LOGIN_SERVICE_REDIRECT;
//    private final String LOGOUT_SERVICE;
//    private final String LOGOUT_SERVICE_REDIRECT;
//    private final String UAWA_APPLICATION_ID;
//    private HttpClient httpClient;
//    private final boolean STANDALONE;
//    Properties properties = AppConfig.readProperties();

    public UserAdminController() throws IOException {
//        STANDALONE = Boolean.valueOf(properties.getProperty("standalone"));
//        MY_APP_URI = properties.getProperty("myuri");
//        MY_APP_TYPE = properties.getProperty("myapp");
//        if (MY_APP_TYPE == null || MY_APP_TYPE.isEmpty()) {
//            MY_APP_TYPE = "useradmin";
//        }
//
//        tokenServiceClient = new WhydahServiceClient();
//        LOGIN_SERVICE_REDIRECT = "redirect:" + properties.getProperty("logonservice") + "login?" + REDIRECT_URI_KEY + "=" + MY_APP_URI;
//        LOGOUT_SERVICE = properties.getProperty("logonservice") + "welcome?" + REDIRECT_URI_KEY + "=" + MY_APP_URI;
//        LOGOUT_SERVICE_REDIRECT = "redirect:" + LOGOUT_SERVICE;
//        UAWA_APPLICATION_ID = properties.getProperty("applicationid");
//        if (UAWA_APPLICATION_ID == null || UAWA_APPLICATION_ID.trim().isEmpty()) {
//            throw new RuntimeException("Missing configuration property: applicationid");
//        }
//
//        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
//
//        StringBuilder strb = new StringBuilder("Initialized UserAdminController \n");
//        strb.append("\n- Standalone=").append(STANDALONE);
//        strb.append("\n- MY_APP_URI=").append(MY_APP_URI);
//        strb.append("\n- LOGIN_SERVICE_REDIRECT=").append(LOGIN_SERVICE_REDIRECT);
//        strb.append("\n- LOGOUT_SERVICE_REDIRECT=").append(LOGOUT_SERVICE_REDIRECT);
//        log.debug(strb.toString());
    }

    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    @RequestMapping("/")
    public String myapp(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setContentType(ConstantValue.HTML_CONTENT_TYPE);
        if (SessionUserAdminDao.instance.STANDALONE) {
            log.info("Log on OK. - Standalone mode selected, so no authentication.");
            addModelParams(model, "Unauthorized", "Unknown User");
            return SessionUserAdminDao.instance.MY_APP_TYPE;
        }
        
        String userTokenXml = SessionUserAdminDao.instance.findUserTokenXMLFromSession(request, response, model);
        if(userTokenXml==null){
        	log.trace("UserTokenXML null or too short to be useful. Redirecting to login.");
        	return SessionUserAdminDao.instance.LOGIN_SERVICE_REDIRECT;
           
        } else {
        	if (!UserTokenXpathHelper.hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
        		log.trace("Got user from userTokenXml, but wrong access rights. Redirecting to logout.");
        		CookieManager.clearUserTokenCookie(request, response);
        		addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml)); 
        		return "login_error";
        	} else {
        		String userTokenId = UserTokenXpathHelper.getUserTokenIdFromUserTokenXML(userTokenXml);
                Integer tokenRemainingLifetimeSeconds = WhydahServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, response);
                addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml));
                log.info("Logon OK. userTokenIdFromUserTokenXml={}, userTokenXML", userTokenId, userTokenXml);
                return SessionUserAdminDao.instance.MY_APP_TYPE;
        	}
        }
    }
    
    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    @RequestMapping("/myapptokenid")
    public String getMyAppTokenIdForTesting(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setContentType(ConstantValue.HTML_CONTENT_TYPE);
        
        String userTokenXml = SessionUserAdminDao.instance.findUserTokenXMLFromSession(request, response, model);
        if(userTokenXml==null){
        	log.trace("UserTokenXML null or too short to be useful. Redirecting to login.");
        	return SessionUserAdminDao.instance.LOGIN_SERVICE_REDIRECT;
           
        } else {
        	if (!UserTokenXpathHelper.hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
        		log.trace("Got user from userTokenXml, but wrong access rights. Redirecting to logout.");
        		CookieManager.clearUserTokenCookie(request, response);
        		addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml)); 
        		return "login_error";
        	} else {
        		
        		model.addAttribute("jsondata", SessionUserAdminDao.instance.getServiceClient().getWAS().getActiveApplicationTokenId());
				
        		return "json";
        	}
        }
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Model model) {
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        //model.addAttribute("redirectURI", MY_APP_URI);
        //userTokenId = null;
        log.trace("Logout was called with userTokenIdFromCookie={}. Redirecting to {}.", userTokenIdFromCookie, SessionUserAdminDao.instance.LOGOUT_SERVICE_REDIRECT);
        CookieManager.clearUserTokenCookie(request, response);
        return SessionUserAdminDao.instance.LOGOUT_SERVICE_REDIRECT;
    }
    
//    
//    @RequestMapping("/relogin")
//    public String relogin(HttpServletRequest request, HttpServletResponse response, Model model) {
//        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
//        //model.addAttribute("redirectURI", MY_APP_URI);
//        //userTokenId = null;
//        log.trace("Logout was called with userTokenIdFromCookie={}. Redirecting to {}.", userTokenIdFromCookie, SessionUserAdminDao.instance.LOGOUT_SERVICE_REDIRECT);
//        CookieManager.clearUserTokenCookie(request, response);
//        return SessionUserAdminDao.instance.LOGOUT_SERVICE_REDIRECT;
//    }


    private void addModelParams(Model model, String userTokenXml, String realName) {
    	
        model.addAttribute("token", userTokenXml);
        model.addAttribute("realName", realName);
        //model.addAttribute("logOutUrl", LOGOUT_SERVICE);
        model.addAttribute("logOutUrl", SessionUserAdminDao.instance.MY_APP_URI + "logout");
        model.addAttribute("logOutRedirectUrl", SessionUserAdminDao.instance.LOGOUT_SERVICE);
        String baseUrl = "/useradmin/" + SessionUserAdminDao.instance.getServiceClient().getWAS().getActiveApplicationTokenId() + "/" + UserTokenMapper.fromUserTokenXml(userTokenXml).getUserTokenId() + "/";
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("statUrl", baseUrl);
        try {
            Properties properties = AppConfig.readProperties();
            model.addAttribute("statUrl", properties.getProperty("statisticsservice")==null||properties.getProperty("statisticsservice").equals("")?baseUrl:properties.getProperty("statisticsservice"));
        } catch (Exception e){
            log.warn("Unable to read properties and set statUrl value");
        }
    }
    
    

}
