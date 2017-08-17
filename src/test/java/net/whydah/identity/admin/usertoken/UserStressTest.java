package net.whydah.identity.admin.usertoken;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.baseclasses.BaseHttpDeleteHystrixCommand;
import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;
import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.util.SSLTool;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

public class UserStressTest {

	public static String uawaURI = "https://inn-qa-uaswa.opplysningen.no/useradmin/";
	public String stsURI = "https://inn-qa-sts.opplysningen.no/tokenservice/";

	//	public String uawaURI = "http://localhost:9996/useradmin/";
//    public String stsURI = "http://localhost:9998/tokenservice/";
	public static String apptokenIdFromUASWA = "";


	//NEEDED WHEN ASKING UAWA FOR THE CURRENT APPTOKENID
	public String TEMPORARY_APPLICATION_ID = "101";//"11";
	public String TEMPORARY_APPLICATION_NAME = "Whydah-SystemTests";//"Funny APp";//"11";
	public String TEMPORARY_APPLICATION_SECRET = "55fhRM6nbKZ2wfC6RMmMuzXpk";//"LLNmHsQDCerVWx5d6aCjug9fyPE";
	public String userName = "useradmin";
	public String password = "useradmin42";

	public String myApplicationTokenID = "";
	public static String userTokenId = "";

	//TEST METHODS

	public void setup() throws Exception {

		//initialize the testing service
		ApplicationCredential appCredential = new ApplicationCredential(TEMPORARY_APPLICATION_ID, TEMPORARY_APPLICATION_NAME, TEMPORARY_APPLICATION_SECRET);
		UserCredential userCredential = new UserCredential(userName, password);

        Map<String, String> addToEnv = new HashMap<>();
        addToEnv.put("IAM_MODE", "TEST");
        setEnv(addToEnv);
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
	//@Ignore
	public void doUsersStressTestWith1000Users() throws Exception{
		
		setup();

        int count = 10;

        addTestUsers(0, count); //add 1000 users
        //removeTestUsers(0, 100); //remove 100 users
		//removeTestUsers(100, 100); //remove next 100 users
		//removeTestUsers(200, 100); //remove next 100 users
		
		
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

		//Thread.sleep(3000);
	}

	private void addATestUser(int i) {
        UserAggregate ua = new UserAggregate("j" +
                "m_uid-" + i, "m_username " + i, "firstName " + i, "lastName " + i, "personRef " + i, "tester" + i + "@whydah.com", String.valueOf(RandomUtils.nextInt(1000000000)));
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

    protected static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
