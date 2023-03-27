package net.whydah.identity.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.identity.ServerRunner;
import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.dao.ConstantValue;
import net.whydah.identity.admin.dao.SessionUserAdminDao;
import net.whydah.identity.admin.dao.WhydahUAWAServiceClient;
import net.whydah.identity.errorhandling.AppException;
import net.whydah.identity.errorhandling.AppExceptionCode;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.Tag;
import net.whydah.sso.commands.adminapi.application.CommandAddApplication;
import net.whydah.sso.commands.adminapi.application.CommandDeleteApplication;
import net.whydah.sso.commands.adminapi.application.CommandGetApplication;
import net.whydah.sso.commands.adminapi.application.CommandUpdateApplication;
import net.whydah.sso.commands.adminapi.user.*;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandDeleteUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandGetUserRoles;
import net.whydah.sso.commands.adminapi.user.role.CommandUpdateUserRole;
import net.whydah.sso.commands.application.CommandListApplications;
import net.whydah.sso.commands.application.CommandSearchForApplications;
import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.commands.extensions.statistics.CommandGetAppSessionStats;
import net.whydah.sso.commands.extensions.statistics.CommandGetUserSessionStats;
import net.whydah.sso.extensions.useractivity.helpers.UserActivityHelper;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.util.StringConv;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static net.whydah.identity.admin.dao.SessionUserAdminDao.hasUserAdminRight;

//import org.apache.commons.httpclient.*;
//import org.apache.commons.httpclient.HttpMethod;
//import org.apache.commons.httpclient.methods.*;
//import org.apache.commons.httpclient.params.HttpMethodParams;
//import org.apache.commons.httpclient.util.URIUtil;

/**
 * Created by Leon on 25.04.14.
 */
@RequestMapping("/{apptokenid}/{usertokenid}")
@Controller
public class UASProxyController {
    private static final Logger log = LoggerFactory.getLogger(UASProxyController.class);
    private static final String JSON_DATA_KEY = "jsondata";
    public static final String JSON_KEY = "json";
    public static final String CONTENTTYPE_JSON_UTF8 = "application/json; charset=utf-8";

    private final String userAdminServiceUrl;
    // private final HttpClient httpClient;
    private WhydahUAWAServiceClient tokenServiceClient = new WhydahUAWAServiceClient();
    private static Map<String, Integer> preImportUsersProgress = new HashMap<String, Integer>();
    private static Map<String, Integer> importUsersProgress = new HashMap<String, Integer>();
    private static Map<String, Integer> importAppsProgress = new HashMap<String, Integer>();
    private static Map<String, String> map_importedFileNames = new HashMap<String, String>();
    private static Path currentDir = ServerRunner.getCurrentPath();
    private static Path tempUploadDir = currentDir.resolve("data_import_dir");
    private static String stats = "{}";
    private static final long TIME_TO_REFRESH_STATS_LOGS = 2 * 60 * 1000;
    private static long lastTimeStatsReceived = 0;
    private static Map<String, String> appId_Stats = new HashMap<>();
    private static Map<String, String> userId_Stats = new HashMap<>();

    public UASProxyController() throws IOException {
        Properties properties = AppConfig.readProperties();
        userAdminServiceUrl = properties.getProperty("useradminservice");
        // httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        ServerRunner.createDirectories(tempUploadDir);
    }

    private void addModelParams(Model model, String userTokenXml, String realName) {

        model.addAttribute("token", userTokenXml);
        model.addAttribute("realName", realName);
        // model.addAttribute("logOutUrl", LOGOUT_SERVICE);
        model.addAttribute("logOutUrl", SessionUserAdminDao.instance.MY_APP_URI + "logout");
        model.addAttribute("logOutRedirectUrl", SessionUserAdminDao.instance.LOGOUT_SERVICE);
        String baseUrl = "/useradmin/"
                + SessionUserAdminDao.instance.getServiceClient().getWAS().getActiveApplicationTokenId() + "/"
                + UserTokenMapper.fromUserTokenXml(userTokenXml).getUserTokenId() + "/";
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("statUrl", baseUrl);
        try {
            Properties properties = AppConfig.readProperties();
            model.addAttribute("statUrl",
                    properties.getProperty("statisticsservice") == null
                            || properties.getProperty("statisticsservice").equals("") ? baseUrl
                            : properties.getProperty("statisticsservice"));
        } catch (Exception e) {
            log.warn("Unable to read properties and set statUrl value");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/users/find/{query}", method = RequestMethod.GET)
    public String findUsers(@PathVariable("apptokenid") String apptokenid,
                            @PathVariable("usertokenid") String usertokenid, @PathVariable("query") String query,
                            HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("findUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        String utf8query = query;
        try {
            utf8query = new String(query.getBytes("ISO-8859-1"), "UTF-8");

        } catch (UnsupportedEncodingException uee) {

        }
        if (!utf8query.equalsIgnoreCase("*")) {
            utf8query = "*" + utf8query;
            utf8query = utf8query.replace("@", " ");
        }

        log.info("findUsers - Finding users with query: " + utf8query);
        try {
            utf8query = URLEncoder.encode(utf8query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // HttpMethod method = new GetMethod();
        // String url;
        // try {
        // url = buildUasUrl(apptokenid, usertokenid, "users/find/" +
        // URIUtil.encodeAll(utf8query));
        // } catch (URIException urie) {
        // log.warn("Error in handling URIencoding", urie);
        // url = buildUasUrl(apptokenid, usertokenid, "users/find/" + query);
        // }

        // HttpClient replacement
        CommandListUsers cmd = new CommandListUsers(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                utf8query);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    private String checkLogin(String usertokenid, HttpServletRequest request, HttpServletResponse response,
                              Model model) throws AppException {

        if (usertokenid == null || usertokenid.length() < 7) {
            usertokenid = CookieManager.getUserTokenIdFromCookie(request);
            log.trace("findUsers - Override usertokenid={}", usertokenid);
        }

        String userTokenXml = SessionUserAdminDao.instance.getServiceClient().getUserTokenByUserTokenID(usertokenid);
        log.trace("Found UserToken - :{}", userTokenXml);


        if (userTokenXml != null) {
            if (hasUserAdminRight(userTokenXml, SessionUserAdminDao.instance.UAWA_APPLICATION_ID)) {
                Integer tokenRemainingLifetimeSeconds = WhydahUAWAServiceClient
                        .calculateTokenRemainingLifetimeInSeconds(userTokenXml);
                CookieManager.updateUserTokenCookie(usertokenid, tokenRemainingLifetimeSeconds, request, response);
                model.addAttribute(ConstantValue.USER_TOKEN_ID, usertokenid);
            } else {
                CookieManager.clearUserTokenCookie(request, response);
                addModelParams(model, userTokenXml, UserTokenXpathHelper.getRealName(userTokenXml));


                throw AppExceptionCode.USER_ACCESS_DENIED_6007;
            }
        } else {

            throw AppExceptionCode.USER_AUTHENTICATION_FAILED_6000;
        }
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.GET)
    public String getUserIdentity(@PathVariable("apptokenid") String apptokenid,
                                  @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                                  HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("getUserIdentity with uid={}", uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // HttpMethod method = new GetMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        // makeUasRequest(method, url, model, response);
        // log.trace("getUserIdentity with uid={} returned the following jsondata=\n{}",
        // uid, model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);

        // HttpClient replacement
        CommandGetUser cmd = new CommandGetUser(URI.create(userAdminServiceUrl), apptokenid, usertokenid, uid);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    // Not currently used. Json fetch useridentity + roles currently.
    /*
     * @GET
     *
     * @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
     *
     * @RequestMapping(value = "/useraggregate/{uid}/", method = RequestMethod.GET)
     * public String getUserAggregate(@PathVariable("apptokenid") String
     * apptokenid, @PathVariable("usertokenid") String
     * usertokenid, @PathVariable("uid") String uid, HttpServletRequest request,
     * HttpServletResponse response, Model model) { HttpMethod method = new
     * GetMethod(); String url = buildUasUrl(apptokenid, usertokenid,
     * "useraggregate/" + uid); makeUasRequest(method, url, model, response);
     * log.trace("getUserAggregate with uid={} returned the following jsondata=\n{}"
     * , uid, model.asMap().get(JSON_DATA_KEY));
     * response.setContentType("application/json; charset=utf-8"); return "json"; }
     */

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/", method = RequestMethod.POST)
    public String createUserIdentity(@PathVariable("apptokenid") String apptokenid,
                                     @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                     Model model) throws IOException, AppException {
        log.trace("createUserIdentity was called");
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        // PostMethod method = new PostMethod();
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "user/");
        // makeUasRequest(method, url, model, response);
        //
        //
        // log.trace("createUserIdentity with the following jsondata=\n{}",
        // model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);

        // HttpClient replacement
        String json = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandAddUser cmd = new CommandAddUser(URI.create(userAdminServiceUrl), apptokenid, usertokenid, json);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());

    }

    private String handleResponse(HttpServletResponse response, Model model, String result, byte[] raw_response,
                                  int statusCode) throws AppException {
        if (result != null) {
            try {
                model.addAttribute(JSON_DATA_KEY, result.replace("\\'", "'").replace("\'", "'"));
            } catch (Exception e) {
                log.error("Unable to map content to page:", e);

            }
        }

        /*
         * else { String responseMsg = raw_response!=null?
         * StringConv.UTF8(raw_response): "N/A" ;
         * log.warn("Failed connection to UAS. Response code: {} - Response message: {}"
         * , statusCode, responseMsg); String msg =
         * "{\"error\":\"Failed connection to the backend server - " + (statusCode!=0 ?
         * ("Status code: " + statusCode) : "A fallback exception occured." ) + "\"}";
         * model.addAttribute(JSON_DATA_KEY, msg);
         *
         * if(statusCode!=0) { response.setStatus(statusCode); } else {
         * response.setStatus(500); } }
         */

        else {
            String responseMsg = raw_response != null ? StringConv.UTF8(raw_response) : "N/A";
            log.warn("Failed connection to UAS. Response code: {} - Response message: {}", statusCode, responseMsg);

            throw new AppException(statusCode != 0 ? HttpStatus.valueOf(statusCode) : HttpStatus.INTERNAL_SERVER_ERROR,
                    9999, "Failed connection to the backend server. Response message: " + responseMsg, "", "");
        }

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.PUT)
    public String updateUserIdentity(@PathVariable("apptokenid") String apptokenid,
                                     @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                                     HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {
        log.trace("updateUserIdentity with uid={}", uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        // PutMethod method = new PutMethod();
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        // makeUasRequest(method, url, model, response);
        // log.trace("updateUserIdentity for uid={} with the following jsondata=\n{}",
        // uid, model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        String json = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandUpdateUser cmd = new CommandUpdateUser(URI.create(userAdminServiceUrl), apptokenid, usertokenid, uid,
                json);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.DELETE)
    public String deleteUser(@PathVariable("apptokenid") String apptokenid,
                             @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                             HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {

        log.info("Deleting user with uid: " + uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // DeleteMethod method = new DeleteMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        // makeUasRequest(method, url, model, response);
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        CommandDeleteUser cmd = new CommandDeleteUser(URI.create(userAdminServiceUrl), apptokenid, usertokenid, uid);
        return handleResponse(response, model, cmd.execute() ? "" : null, cmd.getResponseBodyAsByteArray(),
                cmd.getStatusCode());
    }

    // ROLES

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/roles", method = RequestMethod.GET)
    public String getUserRoles(@PathVariable("apptokenid") String apptokenid,
                               @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                               HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("Getting user roles for user with uid={}", uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // HttpMethod method = new GetMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/roles");
        // makeUasRequest(method, url, model, response);
        // log.trace("getUserRoles with uid={} returned the following jsondata=\n{}",
        // uid, model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        CommandGetUserRoles cmd = new CommandGetUserRoles(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                uid);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/", method = RequestMethod.POST)
    public String postUserRole(@PathVariable("apptokenid") String apptokenid,
                               @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                               HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {
        log.trace("postUserRole for uid={}", uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        //
        // PostMethod method = new PostMethod();
        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/");
        // makeUasRequest(method, url, model, response);
        // log.trace("postUserRole for uid={} with the following jsondata=\n{}", uid,
        // model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        String json = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandAddUserRole cmd = new CommandAddUserRole(URI.create(userAdminServiceUrl), apptokenid, usertokenid, uid,
                json);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/{roleId}", method = RequestMethod.DELETE)
    public String deleteUserRole(@PathVariable("apptokenid") String apptokenid,
                                 @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                                 @PathVariable("roleId") String roleId, HttpServletRequest request, HttpServletResponse response,
                                 Model model) throws AppException {
        log.trace("Deleting role with roleId: " + roleId + ", for user with uid: " + uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // DeleteMethod method = new DeleteMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/" +
        // roleId);
        // makeUasRequest(method, url, model, response);
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        CommandDeleteUserRole cmd = new CommandDeleteUserRole(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                uid, roleId);
        return handleResponse(response, model, cmd.execute() ? "" : null, cmd.getResponseBodyAsByteArray(),
                cmd.getStatusCode());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/{roleId}", method = RequestMethod.PUT)
    public String putUserRole(@PathVariable("apptokenid") String apptokenid,
                              @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid,
                              @PathVariable("roleId") String roleId, HttpServletRequest request, HttpServletResponse response,
                              Model model) throws IOException, AppException {
        log.trace("putUserRole with roleId: " + roleId + ", for user with uid: " + uid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // PutMethod method = new PutMethod();
        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/" +
        // roleId);
        // makeUasRequest(method, url, model, response);
        // log.trace("putUserRole for uid={}, roleId={} with the following
        // jsondata=\n{}", uid, roleId, model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        // HttpClient replacement
        String json = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandUpdateUserRole cmd = new CommandUpdateUserRole(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                uid, roleId, json);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    // PASSWORD

    // @POST
    // @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    // @RequestMapping(value = "/user/{uid}/resetpassword", method =
    // RequestMethod.POST)
    // public String resetPassword(@PathVariable("apptokenid") String apptokenid,
    // @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String
    // uid, HttpServletRequest request, HttpServletResponse response, Model model) {
    // log.trace("Resetting password for user: " + uid);
    //
    // String result = checkLogin(usertokenid, request, response, model);
    // if(result!=null){
    // return result;
    // }
    //
    // PostMethod method = new PostMethod();
    // // String url = userAdminServiceUrl + "password/" + apptokenid
    // +"/reset/username/" + username;
    // String url = userAdminServiceUrl + getUAWAApplicationId() + "/user/" + uid +
    // "/reset_password";
    // makeUasRequest(method, url, model, response);
    // // response.setContentType(CONTENTTYPE_JSON_UTF8);
    // response.setContentType(MediaType.APPLICATION_JSON);
    // return JSON_KEY;
    // }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/auth/password/reset/username/{username}", method = RequestMethod.POST)
    public String resetUserPassword(@PathVariable("apptokenid") String apptokenid,
                                    @PathVariable("usertokenid") String usertokenid, @PathVariable("username") String username,
                                    HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("Resetting password for username: " + username);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // PostMethod method = new PostMethod();
        // // String url = userAdminServiceUrl + "password/" + apptokenid
        // +"/reset/username/" + username;
        // String url = userAdminServiceUrl + getUAWAApplicationId() +
        // "/auth/password/reset/username/" + username;
        // makeUasRequest(method, url, model, response);
        // // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // response.setContentType(MediaType.APPLICATION_JSON);
        // return JSON_KEY;

        // HttpClient replacement
        CommandResetUserPassword cmd = new CommandResetUserPassword(URI.create(userAdminServiceUrl), apptokenid,
                username);
        return handleResponse(response, model, cmd.execute() ? "" : null, cmd.getResponseBodyAsByteArray(),
                cmd.getStatusCode());
    }

    // APPLICATION
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "application/{applicationId}", method = RequestMethod.GET)
    public String getApplication(@PathVariable("apptokenid") String apptokenid,
                                 @PathVariable("usertokenid") String usertokenid, @PathVariable("applicationId") String applicationId,
                                 HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("getApplication - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid,
                usertokenid, applicationId);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // String resourcePath = "application/"+applicationId;
        // String url = buildUasUrl(apptokenid, usertokenid,
        // "application/"+applicationId);
        // GetMethod method = new GetMethod();
        // makeUasRequest(method, url, model, response);
        //
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        CommandGetApplication cmd = new CommandGetApplication(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                applicationId);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "applicationtags/{applicationId}", method = RequestMethod.GET)
    public String getApplicationJsonTag(@PathVariable("apptokenid") String apptokenid,
                                        @PathVariable("usertokenid") String usertokenid, @PathVariable("applicationId") String applicationId,
                                        HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("getApplication - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid,
                usertokenid, applicationId);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // String resourcePath = "application/"+applicationId;
        // String url = buildUasUrl(apptokenid, usertokenid,
        // "application/"+applicationId);
        // GetMethod method = new GetMethod();
        // String jsonResult = makeUasRequest(method, url, model, response);

        CommandGetApplication cmd = new CommandGetApplication(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                applicationId);
        String jsonResult = cmd.execute();
        if (jsonResult != null) {
            Application app = ApplicationMapper.fromJson(jsonResult);
            List<Tag> tagList = new ArrayList<Tag>();
            if (app.getTags() != null && !app.getTags().isEmpty()) {
                tagList = ApplicationTagMapper.getTagList(app.getTags());
            }
            model.addAttribute(JSON_DATA_KEY, ApplicationTagMapper.toJson(tagList));
        } else {
            String responseMsg = cmd.getResponseBodyAsByteArray() != null
                    ? StringConv.UTF8(cmd.getResponseBodyAsByteArray())
                    : "N/A";
            log.warn("Failed connection to UAS. Response code: {} - Response message: {}", cmd.getStatusCode(),
                    responseMsg);
            String msg = "{\"error\":\"Failed connection to the backend server - "
                    + (cmd.getStatusCode() != 0 ? ("Status code: " + cmd.getStatusCode())
                    : "A fallback exception occured.")
                    + "\"}";
            model.addAttribute(JSON_DATA_KEY, msg);
        }

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // protected String findValidUserTokenId(@PathVariable("usertokenid") String
    // usertokenid, HttpServletRequest request) {
    // if (usertokenid == null || usertokenid.length() < 7) {
    // usertokenid = CookieManager.getUserTokenIdFromCookie(request);
    // log.trace("getApplications - Override usertokenid={}", usertokenid);
    // }
    // return usertokenid;
    // }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/", method = RequestMethod.POST)
    public String createApplicationSpecification(@PathVariable("apptokenid") String apptokenid,
                                                 @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                                 Model model) throws IOException, AppException {
        log.trace("createApplicationSpecification was called");
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        //
        // PostMethod method = new PostMethod();
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "application/");
        // makeUasRequest(method, url, model, response);
        // log.trace("createApplicationSpecification with the following jsondata=\n{}",
        // model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        String applicationJson = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandAddApplication cmd = new CommandAddApplication(URI.create(userAdminServiceUrl), apptokenid, usertokenid,
                applicationJson);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/{applicationId}/", method = RequestMethod.PUT)
    public String updateApplicationSpecification(@PathVariable("apptokenid") String apptokenid,
                                                 @PathVariable("usertokenid") String usertokenid, @PathVariable("applicationId") String applicationId,
                                                 HttpServletRequest request, HttpServletResponse response, Model model) throws IOException, AppException {
        log.trace("createApplicationSpecification was called");
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        // InputStreamRequestEntity inputStreamRequestEntity = null;
        // try {
        // inputStreamRequestEntity = new
        // InputStreamRequestEntity(request.getInputStream());
        // } catch (IOException e) {
        // log.error("", e);
        // }
        // PutMethod method = new PutMethod();
        // method.setRequestEntity(inputStreamRequestEntity);
        // String url = buildUasUrl(apptokenid, usertokenid, "application/" +
        // applicationId);
        // makeUasRequest(method, url, model, response);
        // log.trace("updateApplicationSpecification with the following jsondata=\n{}",
        // model.asMap().get(JSON_DATA_KEY));
        // response.setContentType(CONTENTTYPE_JSON_UTF8);

        String applicationJson = IOUtils.toString(request.getInputStream(), "UTF-8");
        CommandUpdateApplication cmd = new CommandUpdateApplication(URI.create(userAdminServiceUrl), apptokenid,
                usertokenid, applicationId, applicationJson);
        String cmd_result = cmd.execute();
        String res = handleResponse(response, model, cmd_result, cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
        if (cmd_result != null) {
            String update_logo_result = new BaseHttpGetHystrixCommand<String>(URI.create(SessionUserAdminDao.instance.SSO_SERVICE), null, apptokenid, "sso", 30000) {

                @Override
                protected String getTargetPath() {
                    return "/updatelogo/" + apptokenid + "/" + applicationId;
                }

            }.execute();

            log.debug("update logos from SSO - result: " + update_logo_result);
        }
        return res;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/{applicationId}/", method = RequestMethod.DELETE)
    public String deleteApplicationSpecification(@PathVariable("apptokenid") String apptokenid,
                                                 @PathVariable("usertokenid") String usertokenid, @PathVariable("applicationId") String applicationId,
                                                 HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("Deleting application with applicationId {} ", applicationId);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        // DeleteMethod method = new DeleteMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "application/" +
        // applicationId);
        // makeUasRequest(method, url, model, response);
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        CommandDeleteApplication cmd = new CommandDeleteApplication(URI.create(userAdminServiceUrl), apptokenid,
                usertokenid, applicationId);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    // APPLICATIONS

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applications", method = RequestMethod.GET)
    public String getApplications(@PathVariable("apptokenid") String apptokenid,
                                  @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                  Model model) throws AppException {
        log.trace("getApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // String jsonResult = getAllApplicationsJsonData(apptokenid, usertokenid,
        // response, model);
        // log.trace("applicationsJson=" + jsonResult);
        //
        //
        // response.setContentType(CONTENTTYPE_JSON_UTF8);
        // return JSON_KEY;

        CommandListApplications cmd = new CommandListApplications(URI.create(userAdminServiceUrl), apptokenid);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    // APPLICATIONTAGS
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applicationtags", method = RequestMethod.GET)
    public String getApplicationTagss(@PathVariable("apptokenid") String apptokenid,
                                      @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                      Model model) throws JsonProcessingException, AppException {
        log.trace("getApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        // String jsonResult = getAllApplicationsJsonData(apptokenid, usertokenid,
        // response, model);
        //
        CommandListApplications cmd = new CommandListApplications(URI.create(userAdminServiceUrl), apptokenid);
        String jsonResult = cmd.execute();

        if (jsonResult != null) {
            List<Application> applicationList = ApplicationMapper.fromJsonList(jsonResult);
            ObjectMapper om = new ObjectMapper();
            HashMap<String, List<Tag>> tagList = new HashMap<String, List<Tag>>();
            for (Application application : applicationList) {
                if (application.getTags() != null && !application.getTags().isEmpty()) {
                    // tagList.addAll(ApplicationTagMapper.getTagList(application.getTags()));
                    tagList.put(application.getId(), ApplicationTagMapper.getTagList(application.getTags()));
                }
            }
            // String jsonData = ApplicationTagMapper.toJson(tagList);
            String jsonData = om.writeValueAsString(tagList);
            model.addAttribute(JSON_DATA_KEY, jsonData);
            log.trace("tags=" + jsonData);

        } else {
            String responseMsg = cmd.getResponseBodyAsByteArray() != null
                    ? StringConv.UTF8(cmd.getResponseBodyAsByteArray())
                    : "N/A";
            log.warn("Failed connection to UAS. Response code: {} - Response message: {}", cmd.getStatusCode(),
                    responseMsg);
            String msg = "{\"error\":\"Failed connection to the backend server - "
                    + (cmd.getStatusCode() != 0 ? ("Status code: " + cmd.getStatusCode())
                    : "A fallback exception occured.")
                    + "\"}";
            model.addAttribute(JSON_DATA_KEY, msg);
        }

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // APPLICATION
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "log/appsession/{applicationId}", method = RequestMethod.GET)
    public String getApplicationLog(@PathVariable("apptokenid") String apptokenid,
                                    @PathVariable("usertokenid") String usertokenid, @PathVariable("applicationId") String applicationId,
                                    HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.info("getApplicationLog - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid,
                usertokenid, applicationId);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        String jsonresult = "{}";


        try {
            //default instance
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            java.util.Date dt1 = cal.getTime();
            Instant from = dt1.toInstant();
            cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 1);
            java.util.Date dt2 = cal.getTime();
            Instant to = dt2.toInstant();

            try {
                String from_date = request.getParameter("from");
                String to_date = request.getParameter("to");

                if (from_date != null && to_date != null) {
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    Date d1 = format1.parse(from_date);
                    Date d2 = format1.parse(to_date);

                    from = d1.toInstant();
                    to = d2.toInstant();
                }
            } catch (Exception x) {

            }

            Properties properties = AppConfig.readProperties();
            jsonresult = new CommandGetAppSessionStats(java.net.URI.create(properties.getProperty("statisticsservice")), applicationId, from, to).execute();
            if (jsonresult != null) {
                jsonresult = UserActivityHelper.getTimedUserSessionsJsonFromUserActivityJson(jsonresult, null, applicationId);
            }
        } catch (Exception e) {
            log.warn("Unable to get statistics for application., returning empty json", e);
        }

        model.addAttribute(JSON_DATA_KEY, jsonresult);
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // USERLOG
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "log/usersession/{userid}", method = RequestMethod.GET)
    public String getUserLog(@PathVariable("apptokenid") String apptokenid,
                             @PathVariable("usertokenid") String usertokenid, @PathVariable("userid") String userid,
                             HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.info("getUserLog - entry.  applicationtokenid={},  applicationId={}, username={}", apptokenid, usertokenid,
                userid);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }


        String jsonresult = "{}";


        try {
            Properties properties = AppConfig.readProperties();

            //default instance
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            java.util.Date dt1 = cal.getTime();
            Instant from = dt1.toInstant();
            cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 1);
            java.util.Date dt2 = cal.getTime();
            Instant to = dt2.toInstant();

            try {
                String from_date = request.getParameter("from");
                String to_date = request.getParameter("to");

                if (from_date != null && to_date != null) {
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                    Date d1 = format1.parse(from_date);
                    Date d2 = format1.parse(to_date);

                    from = d1.toInstant();
                    to = d2.toInstant();
                }
            } catch (Exception x) {

            }

            jsonresult = new CommandGetUserSessionStats(java.net.URI.create(properties.getProperty("statisticsservice")), userid, from, to).execute();
            if (jsonresult != null) {
                jsonresult = UserActivityHelper.getTimedUserSessionsJsonFromUserActivityJson(jsonresult, userid);
            }
        } catch (Exception e) {
            log.warn("Unable to get getUserLog., returning empty json", e);
        }

        model.addAttribute(JSON_DATA_KEY, jsonresult);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }
	
	/*
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "log/userlogon/{userid}", method = RequestMethod.GET)
	public String getUserLogonLog(@PathVariable("apptokenid") String apptokenid,
			@PathVariable("usertokenid") String usertokenid, @PathVariable("userid") String userid,
			HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
		log.info("getUserLogonLog - entry.  applicationtokenid={},  applicationId={}, username={}", apptokenid, usertokenid,
				userid);
		String result = checkLogin(usertokenid, request, response, model);
		if (result != null) {
			return result;
		}
		
		
		String jsonresult = "{}";
		

			try {
				Properties properties = AppConfig.readProperties();
				
				//default instance
				Calendar cal = Calendar.getInstance();
	            cal.add(Calendar.DATE, -1);
	            java.util.Date dt1 = cal.getTime();
	            Instant from = dt1.toInstant();
	            cal = Calendar.getInstance();
	            cal.add(Calendar.DATE, 1);
	            java.util.Date dt2 = cal.getTime();
				Instant to = dt2.toInstant();
				
				try {
					String from_date = request.getParameter("from");
					String to_date = request.getParameter("to");
					
					if(from_date!=null && to_date!=null) {
						SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
						Date d1 = format1.parse( from_date );
						Date d2 = format1.parse( to_date);
						
						from = d1.toInstant();
						to = d2.toInstant();
					}
				} catch(Exception x) {
					
				}
				
				jsonresult = new CommandGetUserLogonStats(java.net.URI.create(properties.getProperty("statisticsservice")), userid, from, to).execute();
				if (jsonresult != null) {
					jsonresult = getUserLogonJsonFromUserActivityJson(jsonresult);
				}
			} catch (Exception e) {
				log.warn("Unable to get getUserLog., returning empty json", e);
			}
		
		model.addAttribute(JSON_DATA_KEY, jsonresult);

		response.setContentType(CONTENTTYPE_JSON_UTF8);
		return JSON_KEY;
	}
	
	public static String getUserLogonJsonFromUserActivityJson(String userActivityJson) {
        try {
        	ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            if (userActivityJson == null) {
                log.trace("getDataElementsFromUserActivityJson was empty, so returning null.");
            } else {
                List<String> items = JsonPathHelper.findJsonpathList(userActivityJson, "$..userlogons.*");
                if (items == null) {
                    log.debug("jsonpath returned zero hits");
                    return null;
                }
                List<String> list = new ArrayList<>();

                final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Calendar c = new GregorianCalendar();

                int i = 0;
           
                while (i < items.size()) {
                    String timestamp = JsonPathHelper.findJsonpathList(userActivityJson, "$..userlogons[" + i + "]").toString();
                    timestamp = timestamp.substring(1, timestamp.length() - 1);
                    c.setTimeInMillis(Long.parseLong(timestamp));
                    list.add(dateFormat.format(c.getTime()));
                    i++;
                }
                return mapper.writeValueAsString(list);
            }
        } catch (Exception e) {
            log.warn("Could not convert getDataElementsFromUserActivityJson Json}");
        }

        return null;
    }
	
	*/

    // USERCRM
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "usercrm/{personRef}", method = RequestMethod.GET)
    public String getUserCRM(@PathVariable("apptokenid") String apptokenid,
                             @PathVariable("usertokenid") String usertokenid, @PathVariable("personRef") String personRef,
                             HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.info("getUserCRM - entry.  usertokenid={},  personRef={}, personRef={}", apptokenid, usertokenid,
                personRef);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        String jsonresult = "{\"status\":\"Not found\"}";
        try {
            Properties properties = AppConfig.readProperties();

            jsonresult = new CommandGetCRMCustomer(java.net.URI.create(properties.getProperty("crmservice")),
                    apptokenid, usertokenid, personRef).execute();
        } catch (Exception e) {
            log.warn("Unable to get getUserCRM, returning empty json", e);
        }
        model.addAttribute(JSON_DATA_KEY, jsonresult);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applications/find/{query}", method = RequestMethod.GET)
    public String findApplications(@PathVariable("apptokenid") String apptokenid,
                                   @PathVariable("usertokenid") String usertokenid, @PathVariable("query") String query,
                                   HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("findApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }
        String utf8query = query;
        try {
            utf8query = new String(query.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {

        }

        try {
            utf8query = URLEncoder.encode(utf8query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // log.trace("findApplications - Finding users with query: " + utf8query);
        // HttpMethod method = new GetMethod();
        // String url;
        // try {
        // url = buildUasUrl(apptokenid, usertokenid, "find/applications/" +
        // URIUtil.encodeAll(utf8query));
        // } catch (URIException urie) {
        // log.warn("Error in handling URIencoding", urie);
        // url = buildUasUrl(apptokenid, usertokenid, "find/applications/" + query);
        // }
        // makeUasRequest(method, url, model, response);
        // return JSON_KEY;

        CommandSearchForApplications cmd = new CommandSearchForApplications(URI.create(userAdminServiceUrl), apptokenid,
                utf8query);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());
    }

    // private String buildUasUrl(String apptokenid, String usertokenid, String s) {
    // return userAdminServiceUrl + getUAWAApplicationId() + "/" + usertokenid + "/"
    // + s;
    // }
    //
    //
    // private String makeUasRequest(HttpMethod method, String url, Model model,
    // HttpServletResponse response) {
    // log.info("Calling url: " + url);
    // HttpMethodParams params = new HttpMethodParams();
    // StringBuilder responseBody=new StringBuilder();
    // params.setHttpElementCharset("UTF-8");
    // params.setContentCharset("UTF-8");
    // method.setParams(params);
    // try {
    // method.setURI(new URI(url, true));
    // int rescode = httpClient.executeMethod(method);
    // // TODO: check rescode?
    // if(rescode==204){
    // response.setStatus(200);
    // model.addAttribute(JSON_DATA_KEY, "");
    // response.setContentType(CONTENTTYPE_JSON_UTF8);
    // return "";
    // } else {
    // InputStream responseBodyStream = method.getResponseBodyAsStream();
    // BufferedReader in = new BufferedReader(new
    // InputStreamReader(responseBodyStream));
    // responseBody = new StringBuilder();
    // String line;
    // while ((line = in.readLine()) !=null) {
    // responseBody.append(line);
    // }
    // response.setStatus(rescode);
    // if (rescode == 200) {
    //
    // model.addAttribute(JSON_DATA_KEY, responseBody.toString());
    // response.setContentType(CONTENTTYPE_JSON_UTF8);
    // return responseBody.toString();
    //
    // } if (rescode == 400) {
    // String msg = "{\"error\":\"Illegal input value.\"}";
    // model.addAttribute(JSON_DATA_KEY,msg);
    // return msg;
    //
    // } else {
    // log.warn("Failed connection to UAS. Reason {}", responseBody.toString() );
    // String msg = "{\"error\":\"Failed connection to backend. Please investigate
    // the logs for reason.\"}";
    // model.addAttribute(JSON_DATA_KEY,msg);
    // return msg;
    //
    // }
    // }
    //
    // } catch (IOException e) {
    // response.setStatus(503);
    // log.error("IOException", e);
    // } catch (NullPointerException e) {
    // response.setStatus(503);
    // log.error("Nullpointer:", e);
    // } finally {
    // method.releaseConnection();
    // }
    //
    //
    // String msg = "{\"error\":\"Server error exception.\"}";
    // model.addAttribute(JSON_DATA_KEY,msg);
    // return msg;
    // }

    private String getUAWAApplicationId() {
        return tokenServiceClient.getWAS().getActiveApplicationTokenId();
    }

    // @POST
    // @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    // @Consumes(MediaType.MULTIPART_FORM_DATA)
    // @RequestMapping(value = "/importUsers", method = RequestMethod.POST)
    // public String importUsers(@PathVariable("apptokenid") String apptokenid,
    // @PathVariable("usertokenid") String usertokenid, HttpServletRequest request,
    // HttpServletResponse response, Model model,
    // @RequestParam CommonsMultipartFile file, @RequestParam String overridenIds,
    // @RequestParam String skippedIds
    // ) throws IOException, ServletException{
    //
    // String filename=file.getOriginalFilename();
    // String progressKey = usertokenid +
    // SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
    // importUsersProgress.put(progressKey, 0);
    //
    // try
    // {
    // List<UserAggregate> oldList = getAllUserAggregates(apptokenid, usertokenid,
    // response, model);
    // Map<String, UserAggregate> oldListMap = new HashMap<String, UserAggregate>();
    // Map<String, UserAggregate> oldListMapByUserName = new HashMap<String,
    // UserAggregate>();
    // for(UserAggregate ua: oldList){
    // oldListMap.put(ua.getUid(), ua);
    // oldListMapByUserName.put(ua.getUsername(), ua);
    // }
    //
    // byte[] content = file.getBytes();
    // saveUploadedFile(content, filename);
    // String json = new String(content, "UTF-8");
    // json = json.replace("\uFEFF", "");
    // List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
    // List<String> duplicates = new ArrayList<String>();
    // for(UserAggregate nua : importList){
    // if(nua.getUsername()==null){
    // setFailureMsg(model,"failed to parse the json file");
    // return JSON_KEY;
    // }
    // if(oldListMapByUserName.containsKey(nua.getUsername()) &&
    // !overridenIds.contains(nua.getUid()) && !skippedIds.contains(nua.getUid())){
    // //duplicates
    // duplicates.add(oldListMapByUserName.get(nua.getUsername()).getUid());
    // }
    // }
    //
    // if(duplicates.size()>0){
    // setMsg(model, "[" + StringUtils.join(duplicates, ',') + "]");
    // } else {
    // Double lastPercentReported = (double) 0;
    // Double workingPercentForEachRow = (importList.size()>0? (double)
    // 100/importList.size(): 0.0);
    // Double currentPercent = (double) 0;
    //
    // for(UserAggregate nua : importList){
    //
    // currentPercent += workingPercentForEachRow;
    //
    // if(nua.getUid()==null){
    // nua.setUid(UUID.randomUUID().toString());
    // }
    //
    // if(oldListMap.containsKey(nua.getUid())&&oldListMapByUserName.containsKey(nua.getUsername())){
    //
    // if(!addorUpdateUserAggregate(apptokenid, usertokenid,
    // UserAggregateMapper.toJson(nua), model, response, false)){
    // addorUpdateUserAggregate(apptokenid, usertokenid,
    // UserAggregateMapper.toJson(oldListMap.get(nua.getUid())), model, response,
    // false);
    // setFailureMsg(model, "failed to override the user " + nua.getUid() + "-" +
    // nua.getUsername() + ". This process has been rolled back");
    // importUsersProgress.remove(progressKey);
    // return "json";//give me a break now
    // }
    //
    // } else {
    //
    // if(oldListMap.containsKey(nua.getUid())){
    // //set new id, cos this one is existing for another username
    // nua.setUid(UUID.randomUUID().toString());
    // }
    //
    // //add application as normal
    // if(!addorUpdateUserAggregate(apptokenid, usertokenid,
    // UserAggregateMapper.toJson(nua), model, response, true)){
    // setFailureMsg(model, "failed to add the new user " + nua.getUid() + "-" +
    // nua.getUsername());
    // importUsersProgress.remove(progressKey);
    // return "json";//give me a break now
    // }
    //
    // }
    //
    // if ((currentPercent >= 1 && lastPercentReported==0)|| (currentPercent -
    // lastPercentReported >= 1)) {
    // lastPercentReported = currentPercent;
    //
    // importUsersProgress.put(progressKey, currentPercent.intValue());
    //
    // }
    //
    //
    // }
    //
    // importUsersProgress.put(progressKey, 100);
    //
    //
    // setOKMsg(model);
    // }
    //
    //
    // } catch(IllegalArgumentException ex){
    // System.out.println(ex);
    // setFailureMsg(model,"failed to parse the json file");
    // } catch(Exception e){
    // System.out.println(e);
    // setFailureMsg(model, e.getMessage());
    // }
    //
    // return JSON_KEY;
    //
    // }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/importUsers/progress/{fileNameMD5}", method = RequestMethod.GET)
    public String getUsersImportProgress(@PathVariable("apptokenid") String apptokenid,
                                         @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                         Model model, @PathVariable("fileNameMD5") String fileNameMD5) throws AppException {
        log.trace("getUsersImportProgress.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        if (usertokenid == null || usertokenid.length() < 7) {
            model.addAttribute(JSON_DATA_KEY, "0");
        } else {

            if (importUsersProgress.containsKey(usertokenid + fileNameMD5.toLowerCase())) {
                model.addAttribute(JSON_DATA_KEY, importUsersProgress.get(usertokenid + fileNameMD5.toLowerCase()));
                if (importUsersProgress.get(usertokenid + fileNameMD5.toLowerCase()) == 100) {
                    importUsersProgress.remove(usertokenid + fileNameMD5.toLowerCase());
                }
            } else {
                model.addAttribute(JSON_DATA_KEY, "0");
            }
        }

        return JSON_KEY;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/importUsers/preimportprogress/{fileNameMD5}", method = RequestMethod.GET)
    public String getUsersPreImportProgress(@PathVariable("apptokenid") String apptokenid,
                                            @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                            Model model, @PathVariable("fileNameMD5") String fileNameMD5) throws AppException {
        log.trace("getUsersPreImportProgress.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        if (usertokenid == null || usertokenid.length() < 7) {
            model.addAttribute(JSON_DATA_KEY, "0");
        } else {

            if (preImportUsersProgress.containsKey(usertokenid + fileNameMD5.toLowerCase())) {

                model.addAttribute(JSON_DATA_KEY, preImportUsersProgress.get(usertokenid + fileNameMD5.toLowerCase()));

                if (preImportUsersProgress.get(usertokenid + fileNameMD5.toLowerCase()) == 100) {
                    preImportUsersProgress.remove(usertokenid + fileNameMD5.toLowerCase());
                }
            } else {
                model.addAttribute(JSON_DATA_KEY, "0");
            }
        }

        return JSON_KEY;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/importApps/progress/{fileNameMD5}", method = RequestMethod.GET)
    public String getAppsImportProgress(@PathVariable("apptokenid") String apptokenid,
                                        @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                        Model model, @PathVariable("fileNameMD5") String fileNameMD5) throws AppException {
        log.trace("getAppsImportProgress.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        if (usertokenid == null || usertokenid.length() < 7) {
            model.addAttribute(JSON_DATA_KEY, "0");
        } else {

            if (importAppsProgress.containsKey(usertokenid + fileNameMD5.toLowerCase())) {
                model.addAttribute(JSON_DATA_KEY, importAppsProgress.get(usertokenid + fileNameMD5.toLowerCase()));
                if (importAppsProgress.get(usertokenid + fileNameMD5.toLowerCase()) == 100) {
                    importAppsProgress.remove(usertokenid + fileNameMD5.toLowerCase());
                }
            } else {
                model.addAttribute(JSON_DATA_KEY, "0");
            }
        }

        return JSON_KEY;

    }

    public boolean isUASRequestOK(String response) {
        return response != null && !response.startsWith("{\"error\"");
    }

    private boolean addorUpdateUserAggregate(String apptokenid, String usertokenid, String content, Model model,
                                             HttpServletResponse response, boolean createNew) throws UnsupportedEncodingException {

        // StringRequestEntity json = new StringRequestEntity(content,
        // "application/json", "UTF-8");

        if (createNew) {
            // PostMethod method = new PostMethod();
            // method.setRequestEntity(json);
            // String url = buildUasUrl(apptokenid, usertokenid, "useraggregate/");
            // return isUASRequestOK(makeUasRequest(method, url, model, response));

            CommandAddUserAggregate cmd = new CommandAddUserAggregate(URI.create(userAdminServiceUrl), apptokenid,
                    usertokenid, content);
            return isUASRequestOK(cmd.execute());
        } else {
            // PutMethod method = new PutMethod();
            // method.setRequestEntity(json);
            // String url = buildUasUrl(apptokenid, usertokenid, "useraggregate/");
            // return isUASRequestOK(makeUasRequest(method, url, model, response));
            CommandUpdateUserAggregate cmd = new CommandUpdateUserAggregate(URI.create(userAdminServiceUrl), apptokenid,
                    usertokenid, content);
            return isUASRequestOK(cmd.execute());

        }

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestMapping(value = "/importApps", method = RequestMethod.POST)
    public String importApps(@PathVariable("apptokenid") String apptokenid,
                             @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                             Model model, @RequestParam CommonsMultipartFile file, @RequestParam String overridenIds,
                             @RequestParam String skippedIds) throws IOException, ServletException, AppException {

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        String filename = file.getOriginalFilename();
        String progressKey = usertokenid + SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
        importAppsProgress.put(progressKey, 0);

        try {
            // get old list
            CommandListApplications cmd = new CommandListApplications(URI.create(userAdminServiceUrl), apptokenid);
            String appjson = cmd.execute();
            if (appjson != null) {
                List<Application> oldList = ApplicationMapper.fromJsonList(appjson);
                Map<String, Application> oldListMap = new HashMap<String, Application>();
                for (Application oapp : oldList) {
                    oldListMap.put(oapp.getId(), oapp);
                }

                byte[] content = file.getBytes();
                saveUploadedFile(content, filename);

                // check and update
                // HUY: THERE IS A PROBLEM WITH BOM (byte-order mark) when parsing string, must
                // do json.replace("\uFEFF", "")
                String json = new String(content, "UTF-8");

                List<Application> newList = ApplicationMapper.fromJsonList(json.replace("\uFEFF", ""));
                List<String> duplicates = new ArrayList<String>();
                for (Application napp : newList) {
                    if (oldListMap.containsKey(napp.getId()) && !overridenIds.contains(napp.getId())
                            && !skippedIds.contains(napp.getId())) {
                        // duplicates
                        duplicates.add(napp.getId());
                    }
                }

                // ask users to handle duplicates
                if (duplicates.size() > 0) {
                    setMsg(model, "[" + StringUtils.join(duplicates, ',') + "]");
                } else {

                    Double lastPercentReported = (double) 0;
                    Double workingPercentForEachRow = (newList.size() > 0 ? (double) 100 / newList.size() : 0.0);
                    Double currentPercent = (double) 0;

                    for (Application napp : newList) {

                        currentPercent += workingPercentForEachRow;

                        if (oldListMap.containsKey(napp.getId()) && overridenIds.contains(napp.getId())) {
                            // override duplicates
                            if (!addorUpdateApplication(apptokenid, usertokenid, ApplicationMapper.toJson(napp), model,
                                    response, napp.getId())) {
                                // roll back here for safety?
                                addorUpdateApplication(apptokenid, usertokenid,
                                        ApplicationMapper.toJson(oldListMap.get(napp.getId())), model, response,
                                        napp.getId());
                                setFailureMsg(model, "failed to override the application " + napp.getId() + "-"
                                        + napp.getName() + ". This process has been rolled back");
                                importAppsProgress.remove(progressKey);
                                return "json";// give me a break now
                            }
                        } else {

                            if (skippedIds.contains(napp.getId())) {
                                continue;
                            } else {
                                // add application as normal
                                if (!addorUpdateApplication(apptokenid, usertokenid, ApplicationMapper.toJson(napp),
                                        model, response, null)) {
                                    setFailureMsg(model,
                                            "failed to add the new application " + napp.getId() + "-" + napp.getName());
                                    importAppsProgress.remove(progressKey);
                                    return "json";// give me a break now
                                }
                            }
                        }

                        if ((currentPercent >= 1 && lastPercentReported == 0)
                                || (currentPercent - lastPercentReported >= 1)) {
                            lastPercentReported = currentPercent;

                            importAppsProgress.put(progressKey, currentPercent.intValue());

                        }

                    }

                    importAppsProgress.put(progressKey, 100);

                    setOKMsg(model);
                }
            } else {
                setFailureMsg(model, "failed to get application list - status code ");
                String responseMsg = cmd.getResponseBodyAsByteArray() != null
                        ? StringConv.UTF8(cmd.getResponseBodyAsByteArray())
                        : "N/A";
                log.warn("Failed connection to UAS. Response code: {} - Response message: {}", cmd.getStatusCode(),
                        responseMsg);
                String msg = "{\"error\":\"Failed connection to the backend server - "
                        + (cmd.getStatusCode() != 0 ? ("Status code: " + cmd.getStatusCode())
                        : "A fallback exception occured.")
                        + "\"}";
                setFailureMsg(model, msg);
            }

        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
            setFailureMsg(model, "failed to parse the json file");
        } catch (Exception e) {
            System.out.println(e);
            setFailureMsg(model, e.getMessage());
        }

        return JSON_KEY;

    }

    void setMsg(Model model, String msg) {
        model.addAttribute(JSON_DATA_KEY, "{\"result\":\"" + msg + "\"}");
    }

    void setOKMsg(Model model) {
        model.addAttribute(JSON_DATA_KEY, "{\"result\":\"ok\"}");
    }

    void setFailureMsg(Model model, String msg) {
        model.addAttribute(JSON_DATA_KEY, "{\"result\":\"error: " + msg + "\"}");
    }

    private boolean addorUpdateApplication(String apptokenid, String usertokenid, String content, Model model,
                                           HttpServletResponse response, String appId) throws UnsupportedEncodingException {

        // StringRequestEntity json = new StringRequestEntity(content,
        // "application/json", "UTF-8");
        // boolean createNew = (appId==null||appId.isEmpty());
        // if(createNew){
        // PostMethod method = new PostMethod();
        // method.setRequestEntity(json);
        // String url = buildUasUrl(apptokenid, usertokenid, "application/" +
        // (appId==null||appId.isEmpty()?"":appId));
        // makeUasRequest(method, url, model, response);
        // } else {
        // PutMethod method = new PutMethod();
        // method.setRequestEntity(json);
        // String url = buildUasUrl(apptokenid, usertokenid, "application/" +
        // (appId==null||appId.isEmpty()?"":appId));
        // makeUasRequest(method, url, model, response);
        // }
        //
        // return response.getStatus()==200 || response.getStatus()==204;

        boolean createNew = (appId == null || appId.isEmpty());
        if (createNew) {
            CommandAddApplication cmd = new CommandAddApplication(URI.create(userAdminServiceUrl), apptokenid,
                    usertokenid, content);
            cmd.execute();
            return cmd.getStatusCode() == 200;
        } else {
            CommandUpdateApplication cmd = new CommandUpdateApplication(URI.create(userAdminServiceUrl), apptokenid,
                    usertokenid, appId, content);
            cmd.execute();
            return cmd.getStatusCode() == 204;
        }

    }

    private String saveUploadedFile(byte[] fContent, String filename) throws FileNotFoundException, IOException {

        filename = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()) + "-" + filename;

        if (new File(tempUploadDir + filename).exists()) {
            new File(tempUploadDir + filename).delete();
        }

        BufferedOutputStream bout = new BufferedOutputStream(
                new FileOutputStream(tempUploadDir + File.separator + filename));
        bout.write(fContent);
        bout.flush();
        bout.close();
        return filename;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/users/query/{page}/{query}", method = RequestMethod.GET)
    public String queryUsers(@PathVariable("apptokenid") String apptokenid,
                             @PathVariable("usertokenid") String usertokenid, @PathVariable("page") String page,
                             @PathVariable("query") String query, HttpServletRequest request, HttpServletResponse response,
                             Model model) throws AppException {
        log.trace("queryUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        if (usertokenid == null || usertokenid.length() < 7) {
            usertokenid = CookieManager.getUserTokenIdFromCookie(request);
            log.trace("findUsers - Override usertokenid={}", usertokenid);
        }
        String utf8query = query;
        try {
            utf8query = new String(query.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {

        }
        if (!utf8query.equalsIgnoreCase("*")) {
            utf8query = "*" + utf8query;
            utf8query = utf8query.replace("@", " ");
        }

        try {
            utf8query = URLEncoder.encode(utf8query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        log.info("findUsers - Finding users with query: " + utf8query);
        // HttpMethod method = new GetMethod();
        // String url;
        // try {
        // url = buildUasUrl(apptokenid, usertokenid, "users/query/" + page + "/" +
        // utf8query);
        // } catch (URIException urie) {
        // log.warn("Error in handling URIencoding", urie);
        // url = buildUasUrl(apptokenid, usertokenid, "users/query/" + page + "/" +
        // query);
        // }
        // makeUasRequest(method, url, model, response);
        // return JSON_KEY;

        CommandListUsersWithPagination cmd = new CommandListUsersWithPagination(URI.create(userAdminServiceUrl),
                apptokenid, usertokenid, page, utf8query);
        return handleResponse(response, model, cmd.execute(), cmd.getResponseBodyAsByteArray(), cmd.getStatusCode());

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/users/export/{page}", method = RequestMethod.GET)
    public String exportUsers(@PathVariable("apptokenid") String apptokenid,
                              @PathVariable("usertokenid") String usertokenid, @PathVariable("page") String page,
                              HttpServletRequest request, HttpServletResponse response, Model model) throws AppException {
        log.trace("exportUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        log.info("export uesers for page : " + page);
        // HttpMethod method = new GetMethod();
        // String url = buildUasUrl(apptokenid, usertokenid, "users/export/" + page);
        // makeUasRequest(method, url, model, response);
        //
        // return JSON_KEY;

        try {
            CommandExportUsers cmd = new CommandExportUsers(URI.create(userAdminServiceUrl), apptokenid, usertokenid, page);
            String json = cmd.execute();
            byte[] resByteArray = cmd.getResponseBodyAsByteArray();
            int responseCode = cmd.getStatusCode();
            log.info("export uesers for json : " + json);
            log.info("export uesers for responseCode : " + responseCode);
            return handleResponse(response, model, json, resByteArray, responseCode);
        } catch (Exception e) {
            log.error("export uesers Exception", e);
        }
        setFailureMsg(model, "Unable to export users");

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestMapping(value = "/importUsersAfterCheckingDuplicates", method = RequestMethod.POST)
    public String importUsersAfterCheckingDuplicates(@PathVariable("apptokenid") String apptokenid,
                                                     @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                                     Model model, @RequestParam String overridenIds, @RequestParam String skippedIds,
                                                     @RequestParam String encryptedFileName) throws IOException, ServletException, AppException {

        String filename = "";
        byte[] content = null;
        String progressKey = "";

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        progressKey = usertokenid + encryptedFileName.toLowerCase();
        // load content from the map
        filename = map_importedFileNames.get(encryptedFileName);

        if (filename != null) {
            content = FileUtils.readFileToByteArray(new File(tempUploadDir + File.separator + filename));
        }

        importUsersProgress.put(progressKey, 0);
        preImportUsersProgress.put(progressKey, 0);

        if (content == null) {
            setFailureMsg(model, "The content of uploaded file not found. Please try again.");
            return JSON_KEY;
        }

        try {
            // save the content
            String json = new String(content, "UTF-8");
            json = json.replace("\uFEFF", "");
            List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
            return doImportUsers(apptokenid, usertokenid, response, model, progressKey, overridenIds, skippedIds,
                    importList, encryptedFileName);

        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
            setFailureMsg(model, "failed to parse the json file");
        } catch (Exception e) {
            System.out.println(e);
            setFailureMsg(model, e.getMessage());
        }

        return JSON_KEY;

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestMapping(value = "/importUsers", method = RequestMethod.POST)
    public String importUsers(@PathVariable("apptokenid") String apptokenid,
                              @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                              Model model, @RequestParam CommonsMultipartFile file, @RequestParam String overridenIds,
                              @RequestParam String skippedIds, @RequestParam String encryptedFileName)
            throws IOException, ServletException, AppException {

        String filename = "";
        byte[] content = null;
        String progressKey = "";

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        filename = file.getOriginalFilename();
        content = file.getBytes();
        progressKey = usertokenid + SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
        map_importedFileNames.put(encryptedFileName, saveUploadedFile(content, filename)); // save and add to the map

        importUsersProgress.put(progressKey, 0);
        preImportUsersProgress.put(progressKey, 0);

        if (content == null) {
            setFailureMsg(model, "The content of uploaded file not found. Please try again.");
            return JSON_KEY;
        }

        try {
            // save the content
            String json = new String(content, "UTF-8");
            json = json.replace("\uFEFF", "");
            List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
            if (overridenIds.equals("") && skippedIds.equals("")) {
                // don't post the whole json, content with usernames is ok
                List<String> allUserNames = new ArrayList<String>();
                for (UserAggregate ua : importList) {
                    allUserNames.add("\"" + ua.getUsername() + "\"");
                }

                // PostMethod method = new PostMethod();
                // StringRequestEntity jsonEntity = new StringRequestEntity("[" +
                // (allUserNames.size()>0 ? StringUtils.join(allUserNames, ','):"") + "]",
                // "application/json", "UTF-8");
                // method.setRequestEntity(jsonEntity);
                // String url = buildUasUrl(apptokenid, usertokenid, "users/checkduplicates");

                // we cannot know the exact progress of UIB for this request
                // "users/checkduplicates" - it depends on number of imported users (or how big
                // the file is)
                // let's estimate it by looking at how big the list is
                // tested locally at my local; the call "users/checkduplicates" with 2500 users
                // take around 10 - 20 seconds
                // assume search time = 10 milliseconds/user
                estimatePreImportProgress(progressKey, importList);

                // String uasres = makeUasRequest(method, url, model, response);

                CommandCheckDuplicateUsers cmd = new CommandCheckDuplicateUsers(URI.create(userAdminServiceUrl),
                        apptokenid, usertokenid, allUserNames);
                String uasres = cmd.execute();

                if (!isUASRequestOK(uasres)) {
                    setFailureMsg(model, "Error when validating users - status code " + cmd.getStatusCode());
                    return JSON_KEY;
                }

                // String duplicates = (String) model.asMap().get(JSON_DATA_KEY);
                if (!uasres.equals("[]")) {

                    ObjectMapper mapper = new ObjectMapper();
                    List<String> list = mapper.readValue(uasres, new TypeReference<ArrayList<String>>() {
                    });
                    List<String> duplicateJsons = new ArrayList<String>();
                    for (UserAggregate ua : importList) {
                        if (list.contains(ua.getUsername())) {
                            duplicateJsons.add(UserIdentityMapper.toJson(ua));
                        }
                    }

                    model.addAttribute(JSON_DATA_KEY,
                            "[" + (duplicateJsons.size() > 0 ? StringUtils.join(duplicateJsons, ',') : "") + "]");

                    response.setContentType(CONTENTTYPE_JSON_UTF8);

                    preImportUsersProgress.put(progressKey, 100); // parsed file -> preImport process completed 100%

                    return JSON_KEY;
                } else {
                    return doImportUsers(apptokenid, usertokenid, response, model, progressKey, overridenIds,
                            skippedIds, importList, encryptedFileName);
                }
            } else {
                return doImportUsers(apptokenid, usertokenid, response, model, progressKey, overridenIds, skippedIds,
                        importList, encryptedFileName);
            }

        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
            setFailureMsg(model, "failed to parse the json file");
        } catch (Exception e) {
            System.out.println(e);
            setFailureMsg(model, e.getMessage());
        }

        return JSON_KEY;

    }

    private void estimatePreImportProgress(String progressKey, List<UserAggregate> importList) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                int numberOfImportedUsers = importList.size();

                // expect that UIB can search for 20 users in a row at one second interval
                // hence it costs
                int cost = Math.round(numberOfImportedUsers / 20);

                Double lastPercentReported = (double) 0;
                Double workingPercentForASecond = (importList.size() > 0 ? (double) 100 / cost : 0.0);
                Double currentPercent = (double) 0;

                while (cost > 0) {

                    cost = cost - 1;
                    currentPercent += workingPercentForASecond;

                    if (!preImportUsersProgress.containsKey(progressKey)
                            || preImportUsersProgress.get(progressKey) == 100) {
                        break;
                    }

                    if ((currentPercent >= 1 && lastPercentReported == 0)
                            || (currentPercent - lastPercentReported >= 1)) {
                        lastPercentReported = currentPercent;

                        if (preImportUsersProgress.get(progressKey) == 100) {
                            break;
                        } else {
                            preImportUsersProgress.put(progressKey, currentPercent.intValue());
                        }

                        if (currentPercent.intValue() >= 90) {
                            break; // around 90% almost done now. Wait for completion
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

            }
        }).start();
    }

    private String doImportUsers(String apptokenid, String usertokenid, HttpServletResponse response, Model model,
                                 String progressKey, String overridenUserNames, String skippedUserNames, List<UserAggregate> importList,
                                 String encryptedFileName) throws UnsupportedEncodingException {
        Double lastPercentReported = (double) 0;
        Double workingPercentForEachRow = (importList.size() > 0 ? (double) 100 / importList.size() : 0.0);
        Double currentPercent = (double) 0;

        List<String> skippedItems = Arrays.asList(skippedUserNames.split("\\s*,\\s*"));
        List<String> overridenItems = Arrays.asList(overridenUserNames.split("\\s*,\\s*"));

        for (UserAggregate nua : importList) {

            currentPercent += workingPercentForEachRow;

            if (nua.getUid() == null) {
                nua.setUid(UUID.randomUUID().toString());
            }

            if (skippedItems.contains(nua.getUsername())) {
                continue;
            }

            if (overridenItems.contains(nua.getUsername())) {

                if (!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response,
                        false)) {
                    setFailureMsg(model, "failed to override the user " + nua.getUsername() + "-" + nua.getUsername());
                    importUsersProgress.remove(progressKey);
                    return "json";// give me a break now
                }

            } else {

                nua.setUid(UUID.randomUUID().toString()); // better create new, which will rule out the uid mess
                // add application as normal
                if (!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response,
                        true)) {
                    setFailureMsg(model, "failed to add the new user " + nua.getUid() + "-" + nua.getUsername());
                    importUsersProgress.remove(progressKey);
                    return "json";// give me a break now
                }

            }

            if ((currentPercent >= 1 && lastPercentReported == 0) || (currentPercent - lastPercentReported >= 1)) {
                lastPercentReported = currentPercent;

                importUsersProgress.put(progressKey, currentPercent.intValue());

            }

        }

        importUsersProgress.put(progressKey, 100);
        String fileName = map_importedFileNames.get(encryptedFileName);
        // change the filename to "imported_" + filename after importing
        if (fileName != null) {
            new File(tempUploadDir + File.separator + fileName)
                    .renameTo(new File(tempUploadDir + File.separator + "imported_" + fileName));
        }
        map_importedFileNames.remove(encryptedFileName); // ok, delete this out of the map
        setOKMsg(model);

        return JSON_KEY;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequestMapping(value = "/removeUploadedFile", method = RequestMethod.POST)
    public String removeUploadedFile(@PathVariable("apptokenid") String apptokenid,
                                     @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response,
                                     Model model, @RequestParam String encryptedFileName) throws IOException, ServletException, AppException {

        String result = checkLogin(usertokenid, request, response, model);
        if (result != null) {
            return result;
        }

        String filename = "";

        String progressKey = "";

        progressKey = usertokenid + encryptedFileName.toLowerCase();
        // load content from the map
        filename = map_importedFileNames.get(encryptedFileName);

        if (filename != null) {
            new File(tempUploadDir + File.separator + filename).delete();

        }

        preImportUsersProgress.remove(progressKey);

        setOKMsg(model);

        return JSON_KEY;

    }

}
