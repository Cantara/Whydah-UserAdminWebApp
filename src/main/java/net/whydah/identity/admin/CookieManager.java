package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URL;

public class CookieManager {
    private static final String USER_TOKEN_REFERENCE_NAME = "whydahusertoken_useradminwebapp";
    private static final Logger log = LoggerFactory.getLogger(CookieManager.class);
    private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    private static String cookiedomain = null;
    private static String MY_APP_URI;
    private static boolean IS_MY_URI_SECURED = false;
    
    private CookieManager() {
    }

    static {
//        try {
//            cookiedomain = AppConfig.readProperties().getProperty("cookiedomain");
//        } catch (IOException e) {
//            log.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
//        }
    	
    	try {
            cookiedomain = AppConfig.readProperties().getProperty("cookiedomain");
            MY_APP_URI = AppConfig.readProperties().getProperty("myuri");
           
            //some overrides
            URL uri;
        	if(MY_APP_URI!=null){
            
				 uri = new URL(MY_APP_URI);
				 IS_MY_URI_SECURED = MY_APP_URI.indexOf("https") >= 0;
				 if(cookiedomain==null || cookiedomain.isEmpty()){
					 String domain = uri.getHost();
					 domain = domain.startsWith("www.") ? domain.substring(4) : domain;
					 cookiedomain = domain;
				 }
			 }
           
        } catch (IOException e) {
            log.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
        }
    }


    public static void createAndSetUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        updateCookie(cookie, userTokenId, tokenRemainingLifetimeSeconds, response);
    }

    public static void updateUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if(cookie==null){
        	cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        }
        updateCookie(cookie, userTokenId, tokenRemainingLifetimeSeconds, response);
    }

    private static void updateCookie(Cookie cookie, String cookieValue, Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
        if (cookieValue != null) {
            cookie.setValue(cookieValue);
        }
        //Only name and value are sent back to the server from the browser. The other attributes are only used by the browser to determine of the cookie should be sent or not.
        //http://en.wikipedia.org/wiki/HTTP_cookie#Setting_a_cookie

        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
        cookie.setMaxAge(tokenRemainingLifetimeSeconds);

        if (cookiedomain != null && !cookiedomain.isEmpty()) {
            cookie.setDomain(cookiedomain);
        }
        cookie.setPath("; HttpOnly;");
        cookie.setSecure(IS_MY_URI_SECURED);
        log.debug("Created/updated cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
        response.addCookie(cookie);
    }

    public static void clearUserTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if (cookie != null) {
            cookie.setValue("");

            cookie.setMaxAge(0);
            if (cookiedomain != null && !cookiedomain.isEmpty()) {
                cookie.setDomain(cookiedomain);
            }
            cookie.setPath("; HttpOnly;");
            cookie.setSecure(IS_MY_URI_SECURED);
            log.trace("Cleared cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
            response.addCookie(cookie);
        }
    }


    public static String getUserTokenIdFromCookie(HttpServletRequest request) {
        Cookie userTokenCookie = getUserTokenCookie(request);
        if (userTokenCookie != null && userTokenCookie.getValue().length() > 7) {
            return userTokenCookie.getValue();
        }

        return (userTokenCookie != null ? userTokenCookie.getValue() : null);
    }

    private static Cookie getUserTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            log.debug("getUserTokenCookie: cookie with name={}, value={}", cookie.getName(), cookie.getValue());
            if (USER_TOKEN_REFERENCE_NAME.equalsIgnoreCase(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
    
    public static void addSecurityHTTPHeaders(HttpServletResponse response) {
        //TODO Vi trenger en plan her.
        //response.setHeader("X-Frame-Options", "sameorigin");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    }

}
