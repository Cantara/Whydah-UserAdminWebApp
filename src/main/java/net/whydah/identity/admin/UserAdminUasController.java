package net.whydah.identity.admin;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.whydah.identity.ServerRunner;
import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.dao.SessionUserAdminDao;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.Tag;
import net.whydah.sso.commands.extensions.crmapi.CommandGetCRMCustomer;
import net.whydah.sso.commands.extensions.statistics.CommandListUserActivities;
import net.whydah.sso.extensions.useractivity.helpers.UserActivityHelper;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserRoleMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

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
	private WhydahServiceClient tokenServiceClient = new WhydahServiceClient();
	private static Map<String, Integer> importUsersProgress=new HashMap<String, Integer>();
	private static Map<String, Integer> importAppsProgress=new HashMap<String, Integer>();

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
        if (!utf8query.equalsIgnoreCase("*")) {
            utf8query = "*" + utf8query;
            utf8query = utf8query.replace("@", " ");
        }

        log.info("findUsers - Finding users with query: " + utf8query);
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
		log.info("Deleting user with uid: " + uid);
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
		String url = userAdminServiceUrl + getUAWAApplicationId() + "/user/" + uid + "/reset_password";
		makeUasRequest(method, url, model, response);
		//        response.setContentType(CONTENTTYPE_JSON_UTF8);
		response.setContentType(MediaType.APPLICATION_JSON);
		return JSON_KEY;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value ="/auth/password/reset/username/{username}", method = RequestMethod.POST)
	public String resetUserPassword(@PathVariable("apptokenid") String apptokenid,  @PathVariable("username") String username, HttpServletRequest request, HttpServletResponse response, Model model) {
		log.trace("Resetting password for username: " + username);
		PostMethod method = new PostMethod();
		//       String url = userAdminServiceUrl + "password/" + apptokenid +"/reset/username/" + username;
		String url = userAdminServiceUrl + getUAWAApplicationId() + "/auth/password/reset/username/" + username;
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
	
	@GET
	@Produces(MediaType.APPLICATION_JSON+ ";charset=utf-8")
	@RequestMapping(value = "applicationtags/{applicationId}", method = RequestMethod.GET)
	public String getApplicationJsonTag(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
			@PathVariable("applicationId") String applicationId, HttpServletRequest request,
			HttpServletResponse response, Model model) {
		log.trace("getApplication - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid, usertokenid, applicationId);
		usertokenid = findValidUserTokenId(usertokenid, request);

		//        String resourcePath = "application/"+applicationId;
		String url = buildUasUrl(apptokenid, usertokenid, "application/"+applicationId);
		GetMethod method = new GetMethod();
		String jsonResult = makeUasRequest(method, url, model, response);
		
		Application app = ApplicationMapper.fromJson(jsonResult);
		List<Tag> tagList = new ArrayList<Tag>();
		if(app.getTags()!=null){
			tagList = ApplicationTagMapper.getTagList(app.getTags());
		}
		
		model.addAttribute(JSON_DATA_KEY, ApplicationTagMapper.toJson(tagList));
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

		String jsonResult = getAllApplicationsJsonData(apptokenid, usertokenid,
				response, model);
		log.trace("applicationsJson=" + jsonResult);


		response.setContentType(CONTENTTYPE_JSON_UTF8);
		return JSON_KEY;
	}

    // APPLICATIONTAGS
	@GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "/applicationtags", method = RequestMethod.GET)
    public String getApplicationTagss(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model) throws JsonProcessingException {
        log.trace("getApplications - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
        usertokenid = findValidUserTokenId(usertokenid, request);

        String jsonResult = getAllApplicationsJsonData(apptokenid, usertokenid,
                response, model);
        List<Application> applicationList = ApplicationMapper.fromJsonList(jsonResult);
        ObjectMapper om = new ObjectMapper();
        HashMap<String, List<Tag>> tagList = new HashMap<String, List<Tag>>();
        for (Application application : applicationList) {
        	if(application.getTags()!=null){
        		//tagList.addAll(ApplicationTagMapper.getTagList(application.getTags()));
        		tagList.put(application.getId(), ApplicationTagMapper.getTagList(application.getTags()));
        	}
        }
        //String jsonData = ApplicationTagMapper.toJson(tagList);
        String jsonData = om.writeValueAsString(tagList);
        model.addAttribute(JSON_DATA_KEY, jsonData);
        log.trace("tags=" + jsonData);
        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }
    
	// APPLICATION
	@GET
	@Produces(MediaType.APPLICATION_JSON+ ";charset=utf-8")
	@RequestMapping(value = "applicationlog/{applicationId}", method = RequestMethod.GET)
	public String getApplicationLog(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                                    @PathVariable("applicationId") String applicationId, HttpServletRequest request,
                                    HttpServletResponse response, Model model) {
        log.info("getApplicationLog - entry.  applicationtokenid={},  usertokenid={}, applicationId={}", apptokenid, usertokenid, applicationId);
        usertokenid = findValidUserTokenId(usertokenid, request);

        String jsonresult = "{}";
        try {
            Properties properties = AppConfig.readProperties();

            jsonresult = new CommandListUserActivities(java.net.URI.create(properties.getProperty("statisticsservice")), apptokenid, usertokenid, applicationId.trim()).execute();
            if(jsonresult!=null){
                //we should filter activities for this particular application
                jsonresult = UserActivityHelper.getUserSessionsJsonFromUserActivityJson(jsonresult, null, applicationId);

            }
        } catch (Exception e){
            log.warn("Unable to get statistics for application., returning empty json",e);
        }
        model.addAttribute(JSON_DATA_KEY, jsonresult);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // USERLOG
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "userlog/{uid}", method = RequestMethod.GET)
    public String getUserLog(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                             @PathVariable("uid") String uid, HttpServletRequest request,
                             HttpServletResponse response, Model model) {
        log.info("getUserLog - entry.  applicationtokenid={},  applicationId={}, uid={}", apptokenid, usertokenid, uid);
        usertokenid = findValidUserTokenId(usertokenid, request);

        String jsonresult = "{}";
        try {
            Properties properties = AppConfig.readProperties();

            jsonresult = new CommandListUserActivities(java.net.URI.create(properties.getProperty("statisticsservice")), apptokenid, usertokenid, uid.trim()).execute();
            if (jsonresult != null) {
                //we should filter activities for this particular application
                jsonresult = UserActivityHelper.getUserSessionsJsonFromUserActivityJson(jsonresult, uid);

            }
        } catch (Exception e) {
            log.warn("Unable to get getUserLog., returning empty json", e);
        }
        model.addAttribute(JSON_DATA_KEY, jsonresult);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    // USERCRM
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RequestMapping(value = "usercrm/{personRef}", method = RequestMethod.GET)
    public String getUserCRM(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
                             @PathVariable("personRef") String personRef, HttpServletRequest request,
                             HttpServletResponse response, Model model) {
        log.info("getUserCRM - entry.  usertokenid={},  personRef={}, personRef={}", apptokenid, usertokenid, personRef);

        String jsonresult = "{\"status\":\"Not found\"}";
        try {
            Properties properties = AppConfig.readProperties();

            jsonresult = new CommandGetCRMCustomer(java.net.URI.create(properties.getProperty("crmservice")), apptokenid, usertokenid, personRef).execute();
        } catch (Exception e) {
            log.warn("Unable to get getUserCRM, returning empty json", e);
        }
        model.addAttribute(JSON_DATA_KEY, jsonresult);

        response.setContentType(CONTENTTYPE_JSON_UTF8);
        return JSON_KEY;
    }

    private String getAllApplicationsJsonData(String apptokenid, String usertokenid, HttpServletResponse response, Model model) {
        String url = buildUasUrl(apptokenid, usertokenid, "applications");
		GetMethod method = new GetMethod();
		String jsonResult = makeUasRequest(method, url, model, response);
		return jsonResult;
	}

	private List<UserAggregate> getAllUserAggregates(String apptokenid, String usertokenid, HttpServletResponse response, Model model) throws JsonProcessingException, IOException{

		HttpMethod method = new GetMethod();
		String url = buildUasUrl(apptokenid, usertokenid, "users/find/*");
		String userIdentityList = makeUasRequest(method, url, model, response);
        List<UserAggregate> uaList = UserAggregateMapper.getFromJson(userIdentityList);
        //get roles for each
		for(UserAggregate ua : uaList){
			HttpMethod m = new GetMethod();
			String roles_url = buildUasUrl(apptokenid, usertokenid, "user/" + ua.getUid() + "/roles");
			String roleJson = makeUasRequest(m, roles_url, model, response);
			List<UserApplicationRoleEntry> entryList = UserRoleMapper.fromJsonAsList(roleJson);
			ua.setRoleList(entryList);
		}
		return uaList;
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
		return userAdminServiceUrl + getUAWAApplicationId() + "/" + usertokenid + "/" + s;
	}

	private String makeUasRequest(HttpMethod method, String url, Model model, HttpServletResponse response) {
		log.info("Calling url: " + url);
		HttpMethodParams params = new HttpMethodParams();
		StringBuilder responseBody=new StringBuilder();
		params.setHttpElementCharset("UTF-8");
		params.setContentCharset("UTF-8");
		method.setParams(params);
		try {
			method.setURI(new URI(url, true));
			int rescode = httpClient.executeMethod(method);
			// TODO: check rescode?
			if(rescode==204){
				response.setStatus(200);
				model.addAttribute(JSON_DATA_KEY, "");
				response.setContentType(CONTENTTYPE_JSON_UTF8);
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

	private String getUAWAApplicationId() {
		return tokenServiceClient.getWAS().getActiveApplicationTokenId();
	}


//	@POST
//	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
//	@RequestMapping(value = "/importUsers", method = RequestMethod.POST)
//	public String importUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model,
//			@RequestParam CommonsMultipartFile file, @RequestParam String overridenIds, @RequestParam String skippedIds
//			) throws IOException, ServletException{
//
//		String filename=file.getOriginalFilename();  
//		String progressKey = usertokenid + SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
//		importUsersProgress.put(progressKey, 0);
//
//		try
//		{  
//			List<UserAggregate> oldList = getAllUserAggregates(apptokenid, usertokenid, response, model);
//			Map<String, UserAggregate> oldListMap = new HashMap<String, UserAggregate>();
//			Map<String, UserAggregate> oldListMapByUserName = new HashMap<String, UserAggregate>();
//			for(UserAggregate ua: oldList){
//				oldListMap.put(ua.getUid(), ua);
//				oldListMapByUserName.put(ua.getUsername(), ua);
//			}
//
//			byte[] content = file.getBytes();
//			saveUploadedFile(content, filename);
//			String json = new String(content, "UTF-8");
//			json = json.replace("\uFEFF", "");
//            List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
//            List<String> duplicates = new ArrayList<String>();
//			for(UserAggregate nua : importList){
//				if(nua.getUsername()==null){
//					setFailureMsg(model,"failed to parse the json file");
//					return JSON_KEY;
//				}
//				if(oldListMapByUserName.containsKey(nua.getUsername()) && !overridenIds.contains(nua.getUid()) && !skippedIds.contains(nua.getUid())){
//					//duplicates
//					duplicates.add(oldListMapByUserName.get(nua.getUsername()).getUid());
//				}
//			}
//
//			if(duplicates.size()>0){
//				setMsg(model, "[" + StringUtils.join(duplicates, ',') + "]");
//			} else {
//				Double lastPercentReported = (double) 0;
//				Double workingPercentForEachRow = (importList.size()>0? (double) 100/importList.size(): 0.0);
//				Double currentPercent = (double) 0;
//
//				for(UserAggregate nua : importList){
//
//					currentPercent += workingPercentForEachRow;
//
//					if(nua.getUid()==null){
//						nua.setUid(UUID.randomUUID().toString());
//					}
//					
//					if(oldListMap.containsKey(nua.getUid())&&oldListMapByUserName.containsKey(nua.getUsername())){
//
//						if(!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response, false)){
//							addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(oldListMap.get(nua.getUid())), model, response, false);
//							setFailureMsg(model, "failed to override the user " + nua.getUid() + "-" + nua.getUsername() + ". This process has been rolled back");
//							importUsersProgress.remove(progressKey);
//							return "json";//give me a break now
//						}
//
//					} else {
//
//						if(oldListMap.containsKey(nua.getUid())){
//							//set new id, cos this one is existing for another username
//							nua.setUid(UUID.randomUUID().toString());
//						}
//						
//						//add application as normal
//						if(!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response, true)){
//							setFailureMsg(model,  "failed to add the new user " + nua.getUid() + "-" + nua.getUsername());
//							importUsersProgress.remove(progressKey);
//							return "json";//give me a break now
//						}
//
//					}
//
//					if ((currentPercent >= 1 && lastPercentReported==0)|| (currentPercent - lastPercentReported >= 1)) {
//						lastPercentReported = currentPercent;
//
//						importUsersProgress.put(progressKey, currentPercent.intValue());
//
//					}
//
//				
//				}
//
//				importUsersProgress.put(progressKey, 100);
//
//
//				setOKMsg(model);
//			}
//
//
//		} catch(IllegalArgumentException ex){
//			System.out.println(ex);
//			setFailureMsg(model,"failed to parse the json file");
//		} catch(Exception e){
//			System.out.println(e);
//			setFailureMsg(model, e.getMessage());
//		}  
//
//		return JSON_KEY;
//
//	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/importUsers/progress/{fileNameMD5}", method = RequestMethod.GET)
	public String getUsersImportProgress(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
			HttpServletRequest request, HttpServletResponse response, Model model, @PathVariable("fileNameMD5") String fileNameMD5) {
		log.trace("getUsersImportProgress.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
		if (usertokenid == null || usertokenid.length() < 7) {
			model.addAttribute(JSON_DATA_KEY, "0");
		} else {

			if(importUsersProgress.containsKey(usertokenid + fileNameMD5.toLowerCase())){
				model.addAttribute(JSON_DATA_KEY, importUsersProgress.get(usertokenid + fileNameMD5.toLowerCase()));
				if(importUsersProgress.get(usertokenid + fileNameMD5.toLowerCase())==100){
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
	@RequestMapping(value = "/importApps/progress/{fileNameMD5}", method = RequestMethod.GET)
	public String getAppsImportProgress(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid,
			HttpServletRequest request, HttpServletResponse response, Model model, @PathVariable("fileNameMD5") String fileNameMD5) {
		log.trace("getAppsImportProgress.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
		if (usertokenid == null || usertokenid.length() < 7) {
			model.addAttribute(JSON_DATA_KEY, "0");
		} else {

			if(importAppsProgress.containsKey(usertokenid + fileNameMD5.toLowerCase())){
				model.addAttribute(JSON_DATA_KEY, importAppsProgress.get(usertokenid + fileNameMD5.toLowerCase()));
				if(importAppsProgress.get(usertokenid + fileNameMD5.toLowerCase())==100){
					importAppsProgress.remove(usertokenid + fileNameMD5.toLowerCase());
				}
			} else {
				model.addAttribute(JSON_DATA_KEY, "0");
			}
		}

		return JSON_KEY;

	}


	private boolean addorUpdateUserAggregate(String apptokenid, String usertokenid, String content, Model model,  HttpServletResponse response, boolean createNew) throws UnsupportedEncodingException{

		StringRequestEntity json = new StringRequestEntity(content, "application/json",  "UTF-8");

		if(createNew){
			PostMethod method = new PostMethod();
			method.setRequestEntity(json);
			String url = buildUasUrl(apptokenid, usertokenid, "useraggregate/");
			makeUasRequest(method, url, model, response);
		} else {
			PutMethod method = new PutMethod();
			method.setRequestEntity(json);
			String url = buildUasUrl(apptokenid, usertokenid, "useraggregate/");
			makeUasRequest(method, url, model, response);
		}

		return response.getStatus()==200;

	}

	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequestMapping(value = "/importApps", method = RequestMethod.POST)
	public String importApps(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam CommonsMultipartFile file, @RequestParam String overridenIds, @RequestParam String skippedIds
			) throws IOException, ServletException{

		String filename=file.getOriginalFilename();  
		String progressKey = usertokenid + SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
		importAppsProgress.put(progressKey, 0);

		try{  
			//get old list
			List<Application> oldList = ApplicationMapper.fromJsonList(getAllApplicationsJsonData(apptokenid, usertokenid, response, model));
			Map<String, Application> oldListMap = new HashMap<String, Application>();
			for(Application oapp: oldList){
				oldListMap.put(oapp.getId(), oapp);
			}

			byte[] content =file.getBytes();  
			saveUploadedFile(content, filename);

			//check and update
			//HUY: THERE IS A PROBLEM WITH BOM (byte-order mark) when parsing string, must do json.replace("\uFEFF", "")
			String json = new String(content, "UTF-8");



			List<Application> newList = ApplicationMapper.fromJsonList(json.replace("\uFEFF", ""));
			List<String> duplicates = new ArrayList<String>();
			for(Application napp : newList){
				if(oldListMap.containsKey(napp.getId()) && !overridenIds.contains(napp.getId()) && !skippedIds.contains(napp.getId())){
					//duplicates
					duplicates.add(napp.getId());
				}
			}

			//ask users to handle duplicates
			if(duplicates.size()>0){
				setMsg(model, "[" + StringUtils.join(duplicates, ',') + "]");
			} else {

				Double lastPercentReported = (double) 0;
				Double workingPercentForEachRow = (newList.size()>0? (double) 100/newList.size(): 0.0);
				Double currentPercent = (double) 0;
				
				for(Application napp : newList){

					currentPercent += workingPercentForEachRow;
					
					if(oldListMap.containsKey(napp.getId()) && overridenIds.contains(napp.getId())){
						//override duplicates
						if(!addorUpdateApplication(apptokenid, usertokenid, ApplicationMapper.toJson(napp), model, response, napp.getId())){
							//roll back here for safety?
							addorUpdateApplication(apptokenid, usertokenid, ApplicationMapper.toJson(oldListMap.get(napp.getId())), model, response, napp.getId());
							setFailureMsg(model, "failed to override the application " + napp.getId() + "-" + napp.getName() + ". This process has been rolled back");
							importAppsProgress.remove(progressKey);
							return "json";//give me a break now
						}
					} else {

						if(skippedIds.contains(napp.getId())){
							continue;
						} else {
							//add application as normal
							if(!addorUpdateApplication(apptokenid, usertokenid, ApplicationMapper.toJson(napp), model, response, null)){
								setFailureMsg(model,  "failed to add the new application " + napp.getId() + "-" + napp.getName());
								importAppsProgress.remove(progressKey);
								return "json";//give me a break now
							}
						}
					}
					

					if ((currentPercent >= 1 && lastPercentReported==0)|| (currentPercent - lastPercentReported >= 1)) {
						lastPercentReported = currentPercent;

						importAppsProgress.put(progressKey, currentPercent.intValue());

					}
					
				}
				
				importAppsProgress.put(progressKey, 100);
				
				setOKMsg(model);
			}


		} catch(IllegalArgumentException ex){
			System.out.println(ex);
			setFailureMsg(model,"failed to parse the json file");
		} catch(Exception e){
			System.out.println(e);
			setFailureMsg(model, e.getMessage());
		}  

		return JSON_KEY;

	}

	void setMsg(Model model, String msg){
		model.addAttribute(JSON_DATA_KEY,"{\"result\":\"" + msg + "\"}");
	}

	void setOKMsg(Model model){
		model.addAttribute(JSON_DATA_KEY,"{\"result\":\"ok\"}");
	}

	void setFailureMsg(Model model, String msg){
		model.addAttribute(JSON_DATA_KEY,"{\"result\":\"error: "  +  msg + "\"}");
	}

	private boolean addorUpdateApplication(String apptokenid, String usertokenid, String content, Model model,  HttpServletResponse response, String appId ) throws UnsupportedEncodingException{

		StringRequestEntity json = new StringRequestEntity(content, "application/json",  "UTF-8");
		boolean createNew = (appId==null||appId.isEmpty());
		if(createNew){
			PostMethod method = new PostMethod();
			method.setRequestEntity(json);
			String url = buildUasUrl(apptokenid, usertokenid, "application/" + (appId==null||appId.isEmpty()?"":appId));
			makeUasRequest(method, url, model, response);
		} else {
			PutMethod method = new PutMethod();
			method.setRequestEntity(json);
			String url = buildUasUrl(apptokenid, usertokenid, "application/" + (appId==null||appId.isEmpty()?"":appId));
			makeUasRequest(method, url, model, response);
		}

		return response.getStatus()==200 || response.getStatus()==204;

	}

	private void saveUploadedFile(byte[] fContent, String filename)
			throws FileNotFoundException, IOException {

		filename = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()) + "-" + filename;

        Path currentDir = ServerRunner.getCurrentPath();
        Path tempUploadDir = currentDir.resolve("data_import_dir");
        ServerRunner.createDirectories(tempUploadDir);

        if (new File(tempUploadDir + filename).exists()) {
            new File(tempUploadDir + filename).delete();
        }

        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(tempUploadDir + File.separator + filename));
        bout.write(fContent);
		bout.flush();
		bout.close();


	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequestMapping(value = "/exportAllUsers/{page}", method = RequestMethod.GET)
	public String exportAllUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model) throws JsonProcessingException, IOException{
		List<UserAggregate> list = getAllUserAggregates(apptokenid, usertokenid, response, model);
		
		List<UserAggregate> nlist = new ArrayList<UserAggregate>();
		for(int i = 0; i < 100000; i++){
			nlist.addAll(list);
		}
		ObjectMapper om = new ObjectMapper();
		model.addAttribute(JSON_DATA_KEY, om.writeValueAsString(nlist));
		return JSON_KEY;
	}
	
	public void exportSelectedUsers(){
		
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/users/query/{page}/{query}", method = RequestMethod.GET)
	public String queryUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("page") String page, @PathVariable("query") String query, HttpServletRequest request, HttpServletResponse response, Model model) {
		log.trace("queryUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
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

        log.info("findUsers - Finding users with query: " + utf8query);
        HttpMethod method = new GetMethod();
		String url;
		try {
			url = buildUasUrl(apptokenid, usertokenid, "users/query/" + page + "/" + URIUtil.encodeAll(utf8query));
		} catch (URIException urie) {
			log.warn("Error in handling URIencoding", urie);
			url = buildUasUrl(apptokenid, usertokenid, "users/query/" + page + "/" + query);
		}
		makeUasRequest(method, url, model, response);
		return JSON_KEY;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@RequestMapping(value = "/users/export/{page}", method = RequestMethod.GET)
	public String exportUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, @PathVariable("page") String page, HttpServletRequest request, HttpServletResponse response, Model model) {
		log.trace("exportUsers - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
		if (usertokenid == null || usertokenid.length() < 7) {
			usertokenid = CookieManager.getUserTokenIdFromCookie(request);
			log.trace("findUsers - Override usertokenid={}", usertokenid);
		}
		
        log.info("export uesers for page : " + page);
        HttpMethod method = new GetMethod();
		String url = buildUasUrl(apptokenid, usertokenid, "users/export/" + page);
		makeUasRequest(method, url, model, response);
		return JSON_KEY;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequestMapping(value = "/importUsers", method = RequestMethod.POST)
	public String importUsers(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam CommonsMultipartFile file, @RequestParam String overridenIds, @RequestParam String skippedIds
			) throws IOException, ServletException{

		String filename=file.getOriginalFilename();  
		String progressKey = usertokenid + SessionUserAdminDao.instance.getMD5Str(filename).toLowerCase();
		importUsersProgress.put(progressKey, 0);

		try
		{  
			
			byte[] content = file.getBytes();
			saveUploadedFile(content, filename);
			String json = new String(content, "UTF-8");
			json = json.replace("\uFEFF", "");
            List<UserAggregate> importList = UserAggregateMapper.getFromJson(json);
            
			if(overridenIds.equals("") && skippedIds.equals("")){
				PostMethod method = new PostMethod();
				StringRequestEntity jsonEntity = new StringRequestEntity(json, "application/json",  "UTF-8");
				method.setRequestEntity(jsonEntity);
				String url = buildUasUrl(apptokenid, usertokenid, "users/checkduplicates");
				makeUasRequest(method, url, model, response);
				String duplicates = (String) model.asMap().get(JSON_DATA_KEY);
				if(!duplicates.equals("[]")){
					
					return JSON_KEY;
				}
				else {
					return doImportUsers(apptokenid, usertokenid, response, model, progressKey, overridenIds, skippedIds, importList);
				}
			} else {
				return doImportUsers(apptokenid, usertokenid, response, model, progressKey, overridenIds, skippedIds, importList);
			}


		} catch(IllegalArgumentException ex){
			System.out.println(ex);
			setFailureMsg(model,"failed to parse the json file");
		} catch(Exception e){
			System.out.println(e);
			setFailureMsg(model, e.getMessage());
		}  

		return JSON_KEY;

	}

	private String doImportUsers(String apptokenid, String usertokenid,
			HttpServletResponse response, Model model, String progressKey,
			String overridenUserNames, String skippedUserNames,
			List<UserAggregate> importList) throws UnsupportedEncodingException {
		Double lastPercentReported = (double) 0;
		Double workingPercentForEachRow = (importList.size()>0? (double) 100/importList.size(): 0.0);
		Double currentPercent = (double) 0;

		List<String> skippedItems = Arrays.asList(skippedUserNames.split("\\s*,\\s*"));
		List<String> overridenItems = Arrays.asList(overridenUserNames.split("\\s*,\\s*"));
		
		for(UserAggregate nua : importList){

			currentPercent += workingPercentForEachRow;

			if(nua.getUid()==null){
				nua.setUid(UUID.randomUUID().toString());
			}
			
			if(skippedItems.contains(nua.getUsername())){
				continue;
			}
			
			if(overridenItems.contains(nua.getUsername())){

				if(!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response, false)){
					setFailureMsg(model, "failed to override the user " + nua.getUsername() + "-" + nua.getUsername());
					importUsersProgress.remove(progressKey);
					return "json";//give me a break now
				}

			} else {

				nua.setUid(UUID.randomUUID().toString()); //better create new, which will rule out the uid mess
				//add application as normal
				if(!addorUpdateUserAggregate(apptokenid, usertokenid, UserAggregateMapper.toJson(nua), model, response, true)){
					setFailureMsg(model,  "failed to add the new user " + nua.getUid() + "-" + nua.getUsername());
					importUsersProgress.remove(progressKey);
					return "json";//give me a break now
				}

			}

			if ((currentPercent >= 1 && lastPercentReported==0)|| (currentPercent - lastPercentReported >= 1)) {
				lastPercentReported = currentPercent;

				importUsersProgress.put(progressKey, currentPercent.intValue());

			}

		
		}

		importUsersProgress.put(progressKey, 100);


		setOKMsg(model);
		
		return JSON_KEY;
	}

}
