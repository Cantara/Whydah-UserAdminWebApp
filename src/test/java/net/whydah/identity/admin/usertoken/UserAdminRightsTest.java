package net.whydah.identity.admin.usertoken;

import net.whydah.identity.admin.dao.SessionUserAdminDao;
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
        addToEnv.put("IAM_MODE", "TEST");
        setEnv(addToEnv);

    }
    @Test
    public void testAccessVerifyer() throws Exception {

        String testUserTokenWithoutRightRole = "";
        assertFalse(hasUserAdminRight(testUserTokenWithoutRightRole, "2219"));


        String testUserTokenWithRightRole = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"a96a517f-cef3-4be7-92f5-f059b65e4071\">\n" +
                "    <uid></uid>\n" +
                "    <timestamp></timestamp>\n" +
                "    <lifespan>3600000</lifespan>\n" +
                "    <issuer>/token/issuer/tokenverifier</issuer>\n" +
                "    <securitylevel>0</securitylevel>\n" +
                "    <username>test_name</username>\n" +
                "    <firstname>Olav</firstname>\n" +
                "    <lastname>Nordmann</lastname>\n" +
                "    <email></email>\n" +
                "    <personRef></personRef>\n" +
                "    <lastSeen></lastSeen>  <!-- Whydah 2.1 date and time of last registered usersession -->\n" +
                "    <application ID=\"2349785543\">\n" +
                "        <applicationName>Whydah.net</applicationName>\n" +
                "           <organizationName>Kunde 3</organizationName>\n" +
                "              <role name=\"styremedlem\" value=\"\"/>\n" +
                "              <role name=\"president\" value=\"\"/>\n" +
                "           <organizationName>Kunde 4</organizationName>\n" +
                "              <role name=\"styremedlem\" value=\"\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"2219\">\n" +
                "        <applicationName>whydag.org</applicationName>\n" +
                "        <organizationName>Kunde 1</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"Valla\"/>\n" +
                "    </application>\n" +
                " \n" +
                "    <ns2:link type=\"application/xml\" href=\"/\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">8a37ef9624ed93db4873035b0de3d1ca</hash>\n" +
                "</usertoken>";
        assertTrue(hasUserAdminRight(testUserTokenWithRightRole, "2219"));

    }

    @Test
    public void testTottoHasNotAdmin(){
        String tottoToken="<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"a90a586d-a757-4a8e-a74c-46b09e625b04\">\n" +
                "    <uid>c015c712-62af-48b4-91f1-1712893ad791</uid>\n" +
                "    <username>91905054</username>\n" +
                "    <timestamp>1444818564786</timestamp>\n" +
                "    <lifespan>1209600000</lifespan>\n" +
                "    <lastseen></lastseen>\n" +
                "    <issuer>http://id.opplysningen.no/tokenservice/user/1f0e3dad99908345f7439f8ffabdffc4/validate_usertokenid/a90a586d-a757-4a8e-a74c-46b09e625b04</issuer>\n" +
                "    <securitylevel>1</securitylevel>\n" +
                "    <DEFCON></DEFCON>\n" +
                "    <firstname>Thor Henning</firstname>\n" +
                "    <lastname>Hetland</lastname>\n" +
                "    <email>totto@totto.org</email>\n" +
                "    <cellphone>91905054</cellphone>\n" +
                "    <personref></personref>\n" +
                "\n" +
                "    <application ID=\"99\">\n" +
                "        <applicationName>Opplysningen</applicationName>\n" +
                "        <organizationName>magento.oid.capra.cc</organizationName>\n" +
                "        <role name=\"oidaddress\" value=\"Karl Johans gate 6, 0154 Oslo\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"99\">\n" +
                "        <applicationName>Opplysningen</applicationName>\n" +
                "        <organizationName>magento.oid.capra.cc</organizationName>\n" +
                "        <role name=\"deliveryAddress\" value=\"Møllefaret 30E, 0750 Oslo\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"200\">\n" +
                "        <applicationName>Magento</applicationName>\n" +
                "        <organizationName>magento.oid.capra.cc</organizationName>\n" +
                "        <role name=\"username\" value=\"totto@totto.org\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"201\">\n" +
                "        <applicationName>Wordpress</applicationName>\n" +
                "        <organizationName>wordpress.oid.capra.cc</organizationName>\n" +
                "        <role name=\"username\" value=\"totto@totto.org\"/>\n" +
                "    </application>\n" +
                "\n" +
                "    <ns2:link type=\"application/xml\" href=\"http://id.opplysningen.no/tokenservice/user/1f0e3dad99908345f7439f8ffabdffc4/validate_usertokenid/a90a586d-a757-4a8e-a74c-46b09e625b04\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">39e6940126a4ca55ec45451c1fccd2bc</hash>\n" +
                "</usertoken>\n";
        assertFalse(hasUserAdminRight(tottoToken, "2219"));
    }

    @Test
    public void testAccessVerifyer2() {

        String testUserTokenWithoutRightRole = "";
        assertFalse(hasUserAdminRight(testUserTokenWithoutRightRole, "2219"));


        String testUserTokenWithRightRole = "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"37fbd0a0-8fee-436c-810f-b61741456629\">\n" +
                "    <uid>useradmin</uid>\n" +
                "    <timestamp>1511257841655</timestamp>\n" +
                "    <lifespan>86400000</lifespan>\n" +
                "    <issuer></issuer>\n" +
                "    <securitylevel>1</securitylevel>\n" +
                "    <DEFCON>DEFCON5</DEFCON>\n" +
                "    <username>useradmin</username>\n" +
                "    <firstname>UserAdmin</firstname>\n" +
                "    <lastname>UserAdminWebApp</lastname>\n" +
                "    <cellphone>87654321</cellphone>\n" +
                "    <email>whydahadmin@getwhydah.com</email>\n" +
                "    <personref>42</personref>\n" +
                "    <application ID=\"2219\">\n" +
                "        <applicationName>Whydah-UserAdminWebApp</applicationName>\n" +
                "        <organizationName>Support</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"1\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"2212\">\n" +
                "        <applicationName>Whydah-UserAdminService</applicationName>\n" +
                "        <organizationName>Whydah</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"1\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"2210\">\n" +
                "        <applicationName>Whydah-UserIdentityBackend</applicationName>\n" +
                "        <organizationName>Whydah</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"1\"/>\n" +
                "    </application>\n" +
                "\n" +
                "    <ns2:link type=\"application/xml\" href=\"https://whydahdev.cantara.no/tokenservice/user/c1378e0d3171d0b970f6b9bf990f18a1/validate_usertokenid/37fbd0a0-8fee-436c-810f-b61741456629\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">55926edd39d2ef5d6599756ea506ca5c</hash>\n" +
                "</usertoken>";

        assertTrue(UserXpathHelper.hasRoleFromUserToken(testUserTokenWithRightRole, "2219", "WhydahUserAdmin"));
        assertFalse(UserXpathHelper.hasRoleFromUserToken(testUserTokenWithRightRole, "3219", "WhydahUserAdmin"));
        assertTrue(hasUserAdminRight(testUserTokenWithRightRole, "2219"));

    }

    @Test
    public void testProperty() {
        assertTrue("2219".equalsIgnoreCase(SessionUserAdminDao.instance.UAWA_APPLICATION_ID));
    }

}
