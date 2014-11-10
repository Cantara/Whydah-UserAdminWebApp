package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CookieManager {
    public static final String USER_TOKEN_REFERENCE_NAME = "whydahusertoken_sso";
    private static final Logger logger = LoggerFactory.getLogger(CookieManager.class);

    private static String cookiedomain = null;

    private CookieManager() {
    }

    static {
        try {
            cookiedomain = AppConfig.readProperties().getProperty("cookiedomain");
        } catch (IOException e) {
            logger.warn("AppConfig.readProperties failed. cookiedomain was set to {}", cookiedomain, e);
        }
    }



    public static void createAndSetUserTokenCookie(String userTokenId, HttpServletResponse response) {
        Cookie cookie = new Cookie(USER_TOKEN_REFERENCE_NAME, userTokenId);
        //int maxAge = calculateTokenRemainingLifetime(userTokenXml);
        int maxAge = 365 * 24 * 60 * 60; //TODO Calculating TokenLife is hindered by XML with differing schemas

        cookie.setMaxAge(maxAge);
        cookie.setValue(userTokenId);
        if (cookiedomain != null && !cookiedomain.isEmpty()) {
            cookie.setDomain(cookiedomain);
        }
        cookie.setSecure(true);
        logger.trace("Created cookie with name={}, domain={}, value/userTokenId={}, maxAge={}, secure={}", cookie.getName(), cookie.getDomain(), userTokenId, cookie.getMaxAge(), cookie.getSecure());

        response.addCookie(cookie);
    }

    public static void clearUserTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if (cookie != null) {
            logger.trace("Cleared cookie with name={}, domain={}", cookie.getName(), cookie.getDomain());
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setValue("");
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
            logger.debug("getUserTokenCookie: cookie with name={}, path{}, domain={}", cookie.getName(), cookie.getPath(), cookie.getDomain());
            if (USER_TOKEN_REFERENCE_NAME.equalsIgnoreCase(cookie.getName()) && cookiedomain.equalsIgnoreCase(cookie.getDomain())) {
                return cookie;
            }
        }
        return null;
    }


    public static boolean hasRightCookie(HttpServletRequest request) {
        return getUserTokenIdFromCookie(request) != null;
    }
}