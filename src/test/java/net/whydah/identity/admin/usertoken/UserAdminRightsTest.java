package net.whydah.identity.admin.usertoken;

import net.whydah.identity.admin.dao.SessionUserAdminDao;
import net.whydah.sso.config.ApplicationMode;
import net.whydah.sso.user.helpers.UserXpathHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static net.whydah.identity.admin.dao.SessionUserAdminDao.hasUserAdminRight;
import static net.whydah.identity.admin.usertoken.UserStressTest.setEnv;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class UserAdminRightsTest {

    @BeforeClass
    public static void setup() throws Exception {
        Map<String, String> addToEnv = new HashMap<>();
        addToEnv.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
        setEnv(addToEnv);
        System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);

    }

    @Test
    public void testAccessVerifyer() throws Exception {

        String testUserTokenWithoutRightRole = "";
        assertFalse(hasUserAdminRight(testUserTokenWithoutRightRole, "2219"));


        String testUserTokenWithRightRole = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <usertoken xmlns:ns2="http://www.w3.org/1999/xhtml" id="a96a517f-cef3-4be7-92f5-f059b65e4071">
                    <uid></uid>
                    <timestamp></timestamp>
                    <lifespan>3600000</lifespan>
                    <issuer>/token/issuer/tokenverifier</issuer>
                    <securitylevel>0</securitylevel>
                    <username>test_name</username>
                    <firstname>Olav</firstname>
                    <lastname>Nordmann</lastname>
                    <email></email>
                    <personRef></personRef>
                    <lastSeen></lastSeen>  <!-- Whydah 2.1 date and time of last registered usersession -->
                    <application ID="2349785543">
                        <applicationName>Whydah.net</applicationName>
                           <organizationName>Kunde 3</organizationName>
                              <role name="styremedlem" value=""/>
                              <role name="president" value=""/>
                           <organizationName>Kunde 4</organizationName>
                              <role name="styremedlem" value=""/>
                    </application>
                    <application ID="2219">
                        <applicationName>whydag.org</applicationName>
                        <organizationName>Kunde 1</organizationName>
                        <role name="WhydahUserAdmin" value="Valla"/>
                    </application>
                \s
                    <ns2:link type="application/xml" href="/" rel="self"/>
                    <hash type="MD5">8a37ef9624ed93db4873035b0de3d1ca</hash>
                </usertoken>""";
        assertTrue(hasUserAdminRight(testUserTokenWithRightRole, "2219"));

    }

    @Test
    public void testTottoHasNotAdmin(){
        String tottoToken = """
                <usertoken xmlns:ns2="http://www.w3.org/1999/xhtml" id="a90a586d-a757-4a8e-a74c-46b09e625b04">
                    <uid>c015c712-62af-48b4-91f1-1712893ad791</uid>
                    <username>91905054</username>
                    <timestamp>1444818564786</timestamp>
                    <lifespan>1209600000</lifespan>
                    <lastseen></lastseen>
                    <issuer>http://id.opplysningen.no/tokenservice/user/1f0e3dad99908345f7439f8ffabdffc4/validate_usertokenid/a90a586d-a757-4a8e-a74c-46b09e625b04</issuer>
                    <securitylevel>1</securitylevel>
                    <DEFCON></DEFCON>
                    <firstname>Thor Henning</firstname>
                    <lastname>Hetland</lastname>
                    <email>totto@totto.org</email>
                    <cellphone>91905054</cellphone>
                    <personref></personref>
                
                    <application ID="99">
                        <applicationName>Opplysningen</applicationName>
                        <organizationName>magento.oid.capra.cc</organizationName>
                        <role name="oidaddress" value="Karl Johans gate 6, 0154 Oslo"/>
                    </application>
                    <application ID="99">
                        <applicationName>Opplysningen</applicationName>
                        <organizationName>magento.oid.capra.cc</organizationName>
                        <role name="deliveryAddress" value="Møllefaret 30E, 0750 Oslo"/>
                    </application>
                    <application ID="200">
                        <applicationName>Magento</applicationName>
                        <organizationName>magento.oid.capra.cc</organizationName>
                        <role name="username" value="totto@totto.org"/>
                    </application>
                    <application ID="201">
                        <applicationName>Wordpress</applicationName>
                        <organizationName>wordpress.oid.capra.cc</organizationName>
                        <role name="username" value="totto@totto.org"/>
                    </application>
                
                    <ns2:link type="application/xml" href="http://id.opplysningen.no/tokenservice/user/1f0e3dad99908345f7439f8ffabdffc4/validate_usertokenid/a90a586d-a757-4a8e-a74c-46b09e625b04" rel="self"/>
                    <hash type="MD5">39e6940126a4ca55ec45451c1fccd2bc</hash>
                </usertoken>
                """;
        assertFalse(hasUserAdminRight(tottoToken, "2219"));
    }

    @Test
    public void testAccessVerifyer2() {

        String testUserTokenWithoutRightRole = "";
        assertFalse(hasUserAdminRight(testUserTokenWithoutRightRole, "2219"));


        String testUserTokenWithRightRole = """
                <usertoken xmlns:ns2="http://www.w3.org/1999/xhtml" id="37fbd0a0-8fee-436c-810f-b61741456629">
                    <uid>useradmin</uid>
                    <timestamp>1511257841655</timestamp>
                    <lifespan>86400000</lifespan>
                    <issuer></issuer>
                    <securitylevel>1</securitylevel>
                    <DEFCON>DEFCON5</DEFCON>
                    <username>useradmin</username>
                    <firstname>UserAdmin</firstname>
                    <lastname>UserAdminWebApp</lastname>
                    <cellphone>87654321</cellphone>
                    <email>whydahadmin@getwhydah.com</email>
                    <personref>42</personref>
                    <application ID="2219">
                        <applicationName>Whydah-UserAdminWebApp</applicationName>
                        <organizationName>Support</organizationName>
                        <role name="WhydahUserAdmin" value="1"/>
                    </application>
                    <application ID="2212">
                        <applicationName>Whydah-UserAdminService</applicationName>
                        <organizationName>Whydah</organizationName>
                        <role name="WhydahUserAdmin" value="1"/>
                    </application>
                    <application ID="2210">
                        <applicationName>Whydah-UserIdentityBackend</applicationName>
                        <organizationName>Whydah</organizationName>
                        <role name="WhydahUserAdmin" value="1"/>
                    </application>
                
                    <ns2:link type="application/xml" href="https://whydahdev.cantara.no/tokenservice/user/c1378e0d3171d0b970f6b9bf990f18a1/validate_usertokenid/37fbd0a0-8fee-436c-810f-b61741456629" rel="self"/>
                    <hash type="MD5">55926edd39d2ef5d6599756ea506ca5c</hash>
                </usertoken>""";

        assertTrue(UserXpathHelper.hasRoleFromUserToken(testUserTokenWithRightRole, "2219", "WhydahUserAdmin"));
        assertFalse(UserXpathHelper.hasRoleFromUserToken(testUserTokenWithRightRole, "3219", "WhydahUserAdmin"));
        assertTrue(hasUserAdminRight(testUserTokenWithRightRole, "2219"));

    }

    @Test
    public void testProperty() {
        assertTrue("2219".equalsIgnoreCase(SessionUserAdminDao.instance.UAWA_APPLICATION_ID));
    }

}
