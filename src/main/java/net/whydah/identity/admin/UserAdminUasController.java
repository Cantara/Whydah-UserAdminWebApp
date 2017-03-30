package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	private String getAllApplicationsJsonData(String apptokenid, String usertokenid,
			HttpServletResponse response, Model model) {
		String url = buildUasUrl(apptokenid, usertokenid, "applications");
		GetMethod method = new GetMethod();
		String jsonResult = makeUasRequest(method, url, model, response);
		return jsonResult;
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

	private String getUAWAApplicationId() {
		return tokenServiceClient.getWAS().getActiveApplicationTokenId();
	}

	//    @GET
	//    @RequestMapping(value = "/export", method = RequestMethod.GET)
	//    public void export(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request,
	//                                 HttpServletResponse response, Model model) throws IOException {
	//        log.trace("getApplication - entry.  applicationtokenid={},  usertokenid={}", apptokenid, usertokenid);
	//        String url = buildUasUrl(apptokenid, usertokenid, "applications");
	//        GetMethod method = new GetMethod();
	//        String jsonResult = makeUasRequest(method, url, model, response);
	//        processResponse(response, jsonResult);
	//        
	//    } 
	//    
	//    public static void processResponse(final HttpServletResponse response, final String content) {
	//        try (OutputStream stream = response.getOutputStream()) {
	//            
	//            response.setContentType( "application/octet-stream");
	//            response.setHeader("Content-Disposition", "attachment; filename=applications.js");
	//            stream.write(content.getBytes());
	//            stream.flush(); // commits response!
	//        } catch (IOException ex) {
	//            // clean error handling
	//        }
	//    }

	//	@POST
	//	@RequestMapping(value = "/import", method = RequestMethod.POST)
	//	public String importApps(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model,
	//			@FormDataParam("file") InputStream uploadedInputStream,
	//			@FormDataParam("file") FormDataContentDisposition fileDetail
	//			){
	//		
	//		final Part filePart = request.getPart("file");
	//		final String fileName = getFileName(filePart);
	//
	//		 
	//		writeToFile(filePart.getInputStream(), "H://test.txt");
	//		return null;
	//
	//
	//	}


	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public String importApps(@PathVariable("apptokenid") String apptokenid, @PathVariable("usertokenid") String usertokenid, HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam CommonsMultipartFile file, @RequestParam String overridenIds, @RequestParam String skippedIds
			) throws IOException, ServletException{

		String filename=file.getOriginalFilename();  
		
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
			String json = new String(content);
			
			
			
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
				for(Application napp : newList){
					if(oldListMap.containsKey(napp.getId()) && overridenIds.contains(napp.getId())){
						//override duplicates
						if(!addApplication(apptokenid, usertokenid, json, model, response)){
							//roll back here for safety?
							addApplication(apptokenid, usertokenid, ApplicationMapper.toJson(oldListMap.get(napp.getId())), model, response);
							model.addAttribute(JSON_DATA_KEY, "error: " + "failed to override the application " + napp.getId() + "-" + napp.getName() + ". This process has been rolled back");
							break; //give me a break now
						}
					} else {
						
						if(skippedIds.contains(napp.getId())){
							continue;
						} else {
							//add application as normal
							if(!addApplication(apptokenid, usertokenid, json, model, response)){
								model.addAttribute(JSON_DATA_KEY, "error: " + "failed to add the new application " + napp.getId() + "-" + napp.getName());
								break; //give me a break now
							}
						}
					}
				}
				setOKMsg(model);
			}
			

		} catch(IllegalArgumentException ex){
			System.out.println(ex);
			setFailureMsg(model,"failed to parse the json file");
		} catch(Exception e){
			System.out.println(e);
			setFailureMsg(model, e.getMessage());
		}  
		
		return "json";

	}
	
	void setMsg(Model model, String msg){
		model.addAttribute(JSON_DATA_KEY,"{\"result\":\"" + msg + "\"}");
	}
	
	void setOKMsg(Model model){
		String msg = "{\"result\":\"ok\"}";
		model.addAttribute(JSON_DATA_KEY,msg);
	}
	void setFailureMsg(Model model, String msg){
		model.addAttribute(JSON_DATA_KEY,"{\"result\":\"error: "  +  msg + "\"}");
	}
	
	private boolean addApplication(String apptokenid, String usertokenid, String content, Model model,  HttpServletResponse response ) throws UnsupportedEncodingException{

		StringRequestEntity json = new StringRequestEntity(content, "application/json",  "UTF-8");
		PostMethod method = new PostMethod();
		method.setRequestEntity(json);
		String url = buildUasUrl(apptokenid, usertokenid, "application/");
		makeUasRequest(method, url, model, response);
		return response.getStatus()==200;
		
	}

	private void saveUploadedFile(byte[] fContent, String filename)
			throws FileNotFoundException, IOException {

		filename = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date()) + "-" + filename;

        Path currentDir = getCurrentPath();
        Path tempUploadDir = currentDir.resolve("uploads");
        createDirectories(tempUploadDir);

        if (new File(currentDir + filename).exists()) {
            new File(currentDir + filename).delete();
        }

        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(currentDir + File.separator + filename));
        bout.write(fContent);
        bout.flush();
		bout.close();
		
	
	}


    public static Path getCurrentPath() {
        return Paths.get("").toAbsolutePath();
    }

    public static void createDirectories(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            Path dir;
            if ((dir = Files.createDirectories(directory)) != null) {
                log.trace("Created directory: {}", dir.toString());
            } else {
                log.trace("Unable to create directory: {}", dir.toString());
            }
        }
    }


}
