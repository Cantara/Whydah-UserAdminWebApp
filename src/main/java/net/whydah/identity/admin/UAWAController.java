package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.dao.ConstantValue;
import net.whydah.identity.admin.dao.SessionUserAdminDao;
import net.whydah.identity.admin.dao.WhydahUAWAServiceClient;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
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

import static net.whydah.identity.admin.dao.SessionUserAdminDao.hasUserAdminRight;

@Controller
public class UAWAController {
    private static final Logger log = LoggerFactory.getLogger(UAWAController.class);

    public UAWAController() throws IOException {
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
        log.info("Logon OK.  userTokenXML={}", userTokenXml);
        if(userTokenXml==null){
        	log.trace("UserTokenXML null or too short to be useful. Redirecting to login.");
        	return SessionUserAdminDao.instance.LOGIN_SERVICE_REDIRECT;
           
        } else {
            if (!hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
                log.trace("Got user from userTokenXml, but wrong access rights. Redirecting to logout.");
        		CookieManager.clearUserTokenCookie(request, response);
        		addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml)); 
        		return "login_error";
        	} else {
                String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
                Integer tokenRemainingLifetimeSeconds = WhydahUAWAServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                CookieManager.createAndSetUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, response);
                addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml));
                log.info("Logon OK. userTokenIdFromUserTokenXml={}, userTokenXML{}", userTokenId, userTokenXml);
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
            if (!hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
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
