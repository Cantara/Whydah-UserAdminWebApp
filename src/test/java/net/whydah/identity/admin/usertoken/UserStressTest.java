package net.whydah.identity.admin.usertoken;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.baseclasses.BaseHttpDeleteHystrixCommand;
import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;
import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.sso.util.SSLTool;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.github.kevinsawicki.http.HttpRequest;

public class UserStressTest {
	
	public String uawaURI = "http://localhost:9996/useradmin/";
	public String stsURI = "http://localhost:9998/tokenservice/";
	public String apptokenIdFromUASWA ="";
	
	
	//NEEDED WHEN ASKING UAWA FOR THE CURRENT APPTOKENID
	public String TEMPORARY_APPLICATION_ID = "101";//"11";
	public String TEMPORARY_APPLICATION_NAME = "Whydah-SystemTests";//"Funny APp";//"11";
	public String TEMPORARY_APPLICATION_SECRET = "55fhRM6nbKZ2wfC6RMmMuzXpk";//"LLNmHsQDCerVWx5d6aCjug9fyPE";
	public String userName = "useradmin";
	public String password = "useradmin42";
	
	public String myApplicationTokenID = "";
	public String userTokenId="";
	
	//TEST METHODS

	public void setup() throws Exception {

		//initialize the testing service
		ApplicationCredential appCredential = new ApplicationCredential(TEMPORARY_APPLICATION_ID, TEMPORARY_APPLICATION_NAME, TEMPORARY_APPLICATION_SECRET);
		UserCredential userCredential = new UserCredential(userName, password);
        SSLTool.disableCertificateValidation();
        
        String myApplicationTokenID = "";
        SSLTool.disableCertificateValidation();
      
        String myAppTokenXml = new CommandLogonApplication(URI.create(stsURI), appCredential).execute();
        myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
        

        String userticket = UUID.randomUUID().toString();
        String userTokenXML = new CommandLogonUserByUserCredential(URI.create(stsURI), myApplicationTokenID, myAppTokenXml, userCredential, userticket).execute();
        userTokenId = UserXpathHelper.getUserTokenId(userTokenXML);
        
        //ask uaswa for its current 
        apptokenIdFromUASWA = new BaseHttpGetHystrixCommand<String>(URI.create(uawaURI), null, null, "STRESS_TEST", 2000) {	
			@Override
			protected String getTargetPath() {
				
				return "/myapptokenid?userticket=" + userticket;
			}
		}.execute();

		
		
	}
	
	@Test
	@Ignore
	public void doUsersStressTestWith1000Users() throws Exception{
		
		setup();

		int count = 1000;
		
		addTestUsers(0, count); //add 1000 users
		removeTestUsers(0, 100); //remove 100 users
		removeTestUsers(100, 100); //remove next 100 users
		removeTestUsers(200, 100); //remove next 100 users
		
		
		//TODO: do the query and the returned result should show 700 users left
	
	}
	
	@Test
	public void doUsersStressTestWith1000UsersAddAndDeleteAtTheSameTime() throws Exception{
		
		

		//TODO: start a thread for adding 1000 users
		//TODO: start a thread for deleting (delay about some seconds for each record to be inserted beforehand) 
		//TODO: query and check the result
		
	}
	
	@Test
	public void doUsersStressTestDoCleanUpAllTestUsers() throws Exception{
		
		//TODO: delete all test users
		
	}

	
	//END TEST METHODS
	
	
	//PRIVATE FUNCTIONS
	
	private void addTestUsers(int startFrom, int countTo) throws InterruptedException {
		
		for(int i = startFrom; i < countTo ; i++){
			
			addATestUser(i);
			
		}
		
		Thread.sleep(3000);
	}

	private void addATestUser(int i) {
		UserAggregate ua = new UserAggregate("uid-" + i, "username " + i, "firstName " + i, "lastName " + i, "personRef " +i, "tester" + i + "@whydah.com", String.valueOf(RandomUtils.nextInt(1000000000)));
		ua.setRoleList(new ArrayList<UserApplicationRoleEntry>());
		String json = UserAggregateMapper.toJson(ua);
		String addCmd = new BaseHttpPostHystrixCommand<String>(URI.create(uawaURI), null, null, "STRESS_TEST", 2000) {
			
			@Override
			protected String getTargetPath() {
				return apptokenIdFromUASWA + "/" + userTokenId + "/user/";
			}
			
			@Override
			protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
				
				return request.contentType("application/json").send(json);
			}
			
		}.execute();
		System.out.println(addCmd);
	}

	private void removeATestUser(int id) throws InterruptedException {
	
		String delCmd = new BaseHttpDeleteHystrixCommand<String>(URI.create(uawaURI), null, null, "STRESS_TEST", 2000) {

			@Override
			protected String getTargetPath() {
				return apptokenIdFromUASWA + "/" + userTokenId + "/user/" + "uid-" + id;
			}
		}.execute();
		System.out.println(delCmd);

	}
	
	private void removeTestUsers(int startFrom, int toCount) throws InterruptedException {
		for(int i = startFrom; i < toCount ; i++){
			removeATestUser(i);
		}
		
		Thread.sleep(3000);
	}

	//END PRIVATE FUNCTIONS
}
