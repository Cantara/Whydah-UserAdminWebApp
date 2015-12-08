package net.whydah.identity.admin;

import com.sun.jersey.api.client.Client;
import net.whydah.identity.admin.config.AppConfig;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.util.Properties;

/**
 * Created by Leon on 25.04.14.
 */
@RequestMapping("/{apptokenid}/{usertokenid}")
@Controller
public class UserAdminUasController {
    private static final Logger log = LoggerFactory.getLogger(UserAdminUasController.class);
    private static final String JSON_DATA_KEY = "jsondata";
    public static final String JSON_KEY = "json";
    public static final String CONTENTTYPE_JSON_UTF8 = "application/json; charset=utf-8";
    private final String userAdminServiceUrl;
    private final HttpClient httpClient;
    private final Client tokenServiceClient = Client.create();


    public UserAdminUasController() throws IOException {
        Properties properties = AppConfig.readProperties();
        userAdminServiceUrl = properties.getProperty("useradminservice");
        httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/users/find/{query}", method = RequestMethod.GET)
    public String findUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("query") String query, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("findUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        if (usertokenid == null || usertokenid.length() < 7) {
            usertokenid = CookieManager.getUserTokenIdFromCookie(request);
            log.trace("findUsers - Override usertokenid={}", usertokenid);
        }
        String utf8query = query;
        try {
            utf8query = new String(query.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {

        }
        log.trace("findUsers - Finding users with query: " + utf8query);
        HttpMethod method = new GetMethod();
        String url;
        try {
            url = buildUasUrl(apptokenid, usertokenid, "users/find/" + URIUtil.encodeAll(utf8query));
        } catch (URIException urie) {
            log.warn("Error in handling URIencoding", urie);
            url = buildUasUrl(apptokenid, usertokenid, "users/find/" + query);
        }
        makeUasRequest(method, url, model, response);
        return JSON_KEY;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.GET)
    public String getUserIdentity(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                  @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("getUserIdentity with uid={}", uid);
        HttpMethod method = new GetMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        makeUasRequest(method, url, model, response);
        log.trace("getUserIdentity with uid={} returned the following jsondata=\n{}", uid, model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    //Not currently used. Json fetch useridentity + roles currently.
    /*
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/useraggregate/{uid}/", method = RequestMethod.GET)
    public String getUserAggregate(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpMethod method = new GetMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "useraggregate/" + uid);
        makeUasRequest(method, url, model, response);
        log.trace("getUserAggregate with uid={} returned the following jsondata=\n{}", uid, model.asMap().get(JSON_DATA_KEY));
        response.setContentType("application/json; charset=utf-8");
        return "json";
    }
    */

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/", method = RequestMethod.POST)
    public String createUserIdentity(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                     HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("createUserIdentity was called");
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        PostMethod method = new PostMethod();
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "user/");
        makeUasRequest(method, url, model, response);
        log.trace("createUserIdentity with the following jsondata=\n{}", model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.PUT)
    public String updateUserIdentity(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                     @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("updateUserIdentity with uid={}", uid);
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        PutMethod method = new PutMethod();
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        makeUasRequest(method, url, model, response);
        log.trace("updateUserIdentity for uid={} with the following jsondata=\n{}", uid, model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/", method = RequestMethod.DELETE)
    public String deleteUser(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                             @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("Deleting user with uid: " + uid);
        DeleteMethod method = new DeleteMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid);
        makeUasRequest(method, url, model, response);
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }


    // ROLES

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/roles", method = RequestMethod.GET)
    public String getUserRoles(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                               @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("Getting user roles for user with uid={}", uid);
        HttpMethod method = new GetMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/roles");
        makeUasRequest(method, url, model, response);
        log.trace("getUserRoles with uid={} returned the following jsondata=\n{}", uid, model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/", method = RequestMethod.POST)
    public String postUserRole(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                               @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("postUserRole for uid={}", uid);
        PostMethod method = new PostMethod();
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/");
        makeUasRequest(method, url, model, response);
        log.trace("postUserRole for uid={} with the following jsondata=\n{}", uid, model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/{roleId}", method = RequestMethod.DELETE)
    public String deleteUserRole(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid, @PathVariable("roleId") String roleId, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("Deleting role with roleId: " + roleId + ", for user with uid: " + uid);
        DeleteMethod method = new DeleteMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/" + roleId);
        makeUasRequest(method, url, model, response);
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/role/{roleId}", method = RequestMethod.PUT)
    public String putUserRole(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                              @PathVariable("uid") String uid, @PathVariable("roleId") String roleId, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("putUserRole with roleId: " + roleId + ", for user with uid: " + uid);
        PutMethod method = new PutMethod();
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "user/" + uid + "/role/" + roleId);
        makeUasRequest(method, url, model, response);
        log.trace("putUserRole for uid={}, roleId={} with the following jsondata=\n{}", uid, roleId, model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }


    // PASSWORD

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/user/{uid}/resetpassword", method = RequestMethod.POST)
    public String resetPassword(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("uid") String uid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("Resetting password for user: " + uid);
        PostMethod method = new PostMethod();
 //       String url = userAdminServiceUrl + "password/" + apptokenid +"/reset/username/" + username;
        String url = userAdminServiceUrl  + apptokenid +"/user/" + uid+"/reset_password";
        makeUasRequest(method, url, model, response);
//        response.setContentType(CONTENTTYPE_JSON_UTF8);
        response.setContentType(MediaType.APPLICATION_JSON);
        return JSON_KEY;
    }

    // APPLICATION
    @GET
    @Produces(MediaType.APPLICATION_JSON+ ";charset=utf-8")
    @RequestMapping(value = "application/{applicationId}", method = RequestMethod.GET)
    public String getApplication(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                 @PathVariable("applicationId") String applicationId, HttpServletRequest request,
                                 HttpServletResponse response, Model model) {
        log.trace("getApplication - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid, usertokenid, applicationId);
        usertokenid = findValidUserTokenId(usertokenid, request);

//        String resourcePath = "application/"+applicationId;
        String url = buildUasUrl(apptokenid, usertokenid, "application/"+applicationId);
        GetMethod method = new GetMethod();
        String jsonResult = makeUasRequest(method, url, model, response);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    protected String findValidUserTokenId(@PathVariable("usertokenid") String usertokenid, HttpServletRequest request) {
        if (usertokenid == null || usertokenid.length() < 7) {
            usertokenid = CookieManager.getUserTokenIdFromCookie(request);
            log.trace("getApplications - Override usertokenid={}", usertokenid);
        }
        return usertokenid;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/", method = RequestMethod.POST)
    public String createApplicationSpecification(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                     HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("createApplicationSpecification was called");
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        PostMethod method = new PostMethod();
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "application/");
        makeUasRequest(method, url, model, response);
        log.trace("createApplicationSpecification with the following jsondata=\n{}", model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/{applicationId}/", method = RequestMethod.PUT)
    public String updateApplicationSpecification(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                                 @PathVariable("applicationId") String applicationId, HttpServletRequest request, HttpServletResponse response,  Model model) {
        log.trace("createApplicationSpecification was called");
        InputStreamRequestEntity inputStreamRequestEntity = null;
        try {
            inputStreamRequestEntity = new InputStreamRequestEntity(request.getInputStream());
        } catch (IOException e) {
            log.error("", e);
        }
        PutMethod method = new PutMethod();
        method.setRequestEntity(inputStreamRequestEntity);
        String url = buildUasUrl(apptokenid, usertokenid, "application/" + applicationId);
        makeUasRequest(method, url, model, response);
        log.trace("updateApplicationSpecification with the following jsondata=\n{}", model.asMap().get(JSON_DATA_KEY));
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/application/{applicationId}/", method = RequestMethod.DELETE)
    public String deleteApplicationSpecification(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                                 @PathVariable("applicationId") String applicationId, HttpServletRequest request, HttpServletResponse response,  Model model) {
        log.trace("Deleting application with applicationId {} ",applicationId);
        DeleteMethod method = new DeleteMethod();
        String url = buildUasUrl(apptokenid, usertokenid, "application/" + applicationId);
        makeUasRequest(method, url, model, response);
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // APPLICATIONS

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applications", method = RequestMethod.GET)
    public String getApplications(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("getApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        usertokenid = findValidUserTokenId(usertokenid, request);

        String url = buildUasUrl(apptokenid, usertokenid, "applications");
        GetMethod method = new GetMethod();
        String jsonResult =makeUasRequest(method, url, model, response);
        log.trace("applicationsJson=" + jsonResult);


        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applications/find/{query}", method = RequestMethod.GET)
    public String findApplications(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                   @PathVariable("query") String query, HttpServletRequest request, HttpServletResponse response, Model model) {
        log.trace("findApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        if (usertokenid == null || usertokenid.length() < 7) {
            usertokenid = CookieManager.getUserTokenIdFromCookie(request);
            log.trace("findApplications - Override usertokenid={}", usertokenid);
        }
        String utf8query = query;
        try {
            utf8query = new String(query.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {

        }
        log.trace("findApplications - Finding users with query: " + utf8query);
        HttpMethod method = new GetMethod();
        String url;
        try {
            url = buildUasUrl(apptokenid, usertokenid, "applications/find/" + URIUtil.encodeAll(utf8query));
        } catch (URIException urie) {
            log.warn("Error in handling URIencoding", urie);
            url = buildUasUrl(apptokenid, usertokenid, "applications/find/" + query);
        }
        makeUasRequest(method, url, model, response);
        return JSON_KEY;
    }

    private String buildUasUrl(String apptokenid, String usertokenid, String s) {
        return userAdminServiceUrl + apptokenid + "/" + usertokenid + "/" + s;
    }

    private String makeUasRequest(HttpMethod method, String url, Model model, HttpServletResponse response) {
        HttpMethodParams params = new HttpMethodParams();
        StringBuilder responseBody=new StringBuilder();
        params.setHttpElementCharset("UTF-8");
        params.setContentCharset("UTF-8");
        method.setParams(params);
        try {
            method.setURI(new URI(url, true));
            int rescode = httpClient.executeMethod(method);
            // TODO: check rescode?
            if (rescode == 204) { // Delete
                // Do something
            } else {
                InputStream responseBodyStream = method.getResponseBodyAsStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(responseBodyStream));
                responseBody = new StringBuilder();
                String line;
                while ((line = in.readLine()) !=null) {
                    responseBody.append(line);
                }
                if (rescode == 500) {
                    log.warn("Failed connection to UAS. Reason {}", responseBody.toString() );
                    String msg = "{\"error\":\"Failed connection to backend. Please investigate the logs for reason.\"}";
                    model.addAttribute(JSON_DATA_KEY,msg);
                } else {
                    model.addAttribute(JSON_DATA_KEY, responseBody.toString());
                    response.setContentType(CONTENTTYPE_JSON_UTF8);
                }
            }
            response.setStatus(rescode);
        } catch (IOException e) {
            response.setStatus(503);
            log.error("IOException", e);
        } catch (NullPointerException e) {
            response.setStatus(503);
            log.error("Nullpointer:", e);
        } finally {
            method.releaseConnection();
        }
        if (!model.containsAttribute(JSON_DATA_KEY)) {
            log.error("jsondata attribute not set when fetching data from URL: {}", url);
        }
        return responseBody.toString();
    }
}
