package net.whydah.identity.admin.dao;

import net.whydah.identity.admin.CookieManager;
import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.user.helpers.UserXpathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Properties;


public enum SessionUserAdminDao {
    instance;

    private static final Logger log = LoggerFactory.getLogger(SessionUserAdminDao.class);
    protected Logger logger = LoggerFactory.getLogger(SessionUserAdminDao.class);
    protected Properties properties;

    private WhydahUAWAServiceClient serviceClient;
    public String MY_APP_TYPE = "myapp";
    public String MY_APP_URI;
    public String LOGIN_SERVICE;
    public String SSO_SERVICE;
    public String LOGIN_SERVICE_REDIRECT;
    public String LOGOUT_SERVICE;
    public String LOGOUT_SERVICE_REDIRECT;
    public String UAWA_APPLICATION_ID;
    //protected HttpClient httpClient;
    public boolean STANDALONE;
    protected URI tokenServiceUri;
//    private String defaultInternalAppIds = "2210,2211,2212,2219"; //UIB, STS, UAS, UASWA
//    private List<String> internalAppIds = new ArrayList<String>();
//    public List<String> getInternalAppIds() {
//        return internalAppIds;
//    }
    
    private SessionUserAdminDao() {

        try {

            properties = AppConfig.readProperties();
            this.tokenServiceUri = UriBuilder.fromUri(properties.getProperty("securitytokenservice")).build();
            serviceClient = new WhydahUAWAServiceClient(properties);
            STANDALONE = Boolean.valueOf(properties.getProperty("standalone"));
            MY_APP_URI = properties.getProperty("myuri");
            MY_APP_TYPE = properties.getProperty("myapp");
            if (MY_APP_TYPE == null || MY_APP_TYPE.isEmpty()) {
                MY_APP_TYPE = "useradmin";
            }
            SSO_SERVICE = properties.getProperty("logonservice");
            LOGIN_SERVICE = properties.getProperty("logonservice") + "login?" + ConstantValue.REDIRECT_URI + "=" + MY_APP_URI;
            LOGIN_SERVICE_REDIRECT = "redirect:" + properties.getProperty("logonservice") + "login?" + ConstantValue.REDIRECT_URI + "=" + MY_APP_URI;
            LOGOUT_SERVICE = properties.getProperty("logonservice") + "logout?" + ConstantValue.REDIRECT_URI + "=" + MY_APP_URI;
            LOGOUT_SERVICE_REDIRECT = "redirect:" + LOGOUT_SERVICE;
            UAWA_APPLICATION_ID = properties.getProperty("applicationid").trim();
            if (UAWA_APPLICATION_ID == null || UAWA_APPLICATION_ID.trim().isEmpty()) {
                throw new RuntimeException("Missing configuration property: applicationid");
            }
//            String internalAppIdsConfig = properties.getProperty("internalappids", defaultInternalAppIds);
//            if(internalAppIdsConfig!=null && !internalAppIdsConfig.equals("")){
//            	internalAppIds = Arrays.asList(internalAppIdsConfig.split("\\s*,\\s*"));
//            } else {
//            	internalAppIds = Arrays.asList(defaultInternalAppIds.split("\\s*,\\s*"));
//            }
            
            //httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

            StringBuilder strb = new StringBuilder("Initialized UAWAController \n");
            strb.append("\n- Standalone=").append(STANDALONE);
            strb.append("\n- MY_APP_URI=").append(MY_APP_URI);
            strb.append("\n- LOGIN_SERVICE_REDIRECT=").append(LOGIN_SERVICE_REDIRECT);
            strb.append("\n- LOGOUT_SERVICE_REDIRECT=").append(LOGOUT_SERVICE_REDIRECT);

            logger.debug(strb.toString());


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getFromRequest_UserTicket(HttpServletRequest request) {
        return request.getParameter(ConstantValue.USERTICKET);
    }

    public String findUserTokenXMLFromSession(HttpServletRequest request, HttpServletResponse response, Model model) {

		/* FROM totto's comment
		   we should probably look at shared function like "public static UserToken findUserTokenFromSession(httprequest,httpresponse)" 
		   which check and handle cookie(s) and ticket correct and smart...   
		   but it is not a good fit for SDK, as it need the servlet-api  (sdk is client-oriented)... 
		   and we probably have som "variants" in handling...    as I see that INNsolwa, ssolwa and uawa handle it differently...  
		   and inconsistently... 
		   and it is easy to do it wrong...    
		   I think ticket should win over cookie  (if ticket exist and is valid...  
		   but if invalid ticket, we should ignore the ticker and use the usertokenid from the cookie...   
		   if the ticket is ok, we should update the cookie with the new usertokenid.. 
		   and if the usertokenid from the cookie is invalid (id, answer invalif drom sts) we should delete the cookie  
		   (but not if we get exception i.e. if sts is down...)
		 */
        String userTicket = getFromRequest_UserTicket(request);
        String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
        String userTokenXml = null;

        CookieManager.addSecurityHTTPHeaders(response);
        boolean isValidTicket = false;


        /**
         *
         * Logic:
         *
         * a) check ticket
         * b) verify access from ticket
         */
        try {
            //try ticket
            if (userTicket != null && userTicket.length() > 3) {
                log.trace("Find UserToken - Using userTicket:{}", userTicket);
                userTokenXml = getServiceClient().getUserTokenByUserTicket(userTicket);
                log.trace("Found UserToken - :{}", userTokenXml);
                isValidTicket = false;
                if (userTokenXml != null) {
                    if (hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
                        Integer tokenRemainingLifetimeSeconds = WhydahUAWAServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                        CookieManager.updateUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);
                        model.addAttribute(ConstantValue.USER_TOKEN_ID, userTokenId);
                        model.addAttribute(ConstantValue.USERTICKET, userTicket);
                        return userTokenXml;

                    }
                }
            }
            //if ticket is invalid, use cookie
            if (userTokenId != null && userTokenId.length() > 3) { //from cookie
                log.trace("Find UserToken - Using userTokenID from cookie");
                userTokenXml = getServiceClient().getUserTokenByUserTokenID(userTokenId);
                log.trace("Found UserToken - :{}", userTokenXml);
                if (userTokenXml != null) {
                    if (hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
                        Integer tokenRemainingLifetimeSeconds = WhydahUAWAServiceClient.calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                        CookieManager.updateUserTokenCookie(userTokenId, tokenRemainingLifetimeSeconds, request, response);
                        model.addAttribute(ConstantValue.USER_TOKEN_ID, userTokenId);
                        return userTokenXml;

                    }
                }
            }

            if (userTokenXml == null || userTokenXml.length() < ConstantValue.MIN_USER_TOKEN_LENGTH) {
                log.trace("Find UserToken - no session found");
                CookieManager.clearUserTokenCookie(request, response);
            }


        } catch (Exception e) {
            log.warn("welcome redirect - SecurityTokenException exception: ", e);
            return null;
        }
        return userTokenXml;
    }

    public WhydahUAWAServiceClient getServiceClient() {
        return serviceClient;
    }

    public String getMD5Str(String chechSum) {
	    String hashedPass = null;
	    try {
	      java.security.MessageDigest messageDigest = java.security.MessageDigest.getInstance("MD5");
	      messageDigest.update(chechSum.getBytes(), 0, chechSum.length());
	      hashedPass = new BigInteger(1, messageDigest.digest()).toString(16);
	      if (hashedPass.length() < 32) {
	        hashedPass = "0" + hashedPass;
	      }
	    } catch (Exception e) {
            log.debug("failed to create md5");
            log.debug(e.getMessage());
        }
	    return hashedPass;
	  }

    public static boolean hasUserAdminRight(String userTokenXml, String applicationId) {
        return UserXpathHelper.hasRoleFromUserToken(userTokenXml, applicationId, "WhydahUserAdmin");
    }





}
