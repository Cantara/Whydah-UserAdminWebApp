package net.whydah.identity.admin.usertoken;

import org.junit.Test;

import static net.whydah.identity.admin.dao.SessionUserAdminDao.hasUserAdminRight;
import static org.junit.Assert.assertTrue;

public class UserTokenXpathHelperTest {
    @Test
    public void testHasUserAdminRightAllowed() {
        String[] noAccessRoleValues = new String[]{"1", "true", "enabled", ""};
        for (String allowedRoleValue : noAccessRoleValues) {
            assertTrue(hasUserAdminRight(getUserToken(allowedRoleValue), "2219"));
        }
    }

    @Test
    public void testHasUserAdminRightDenied() {
        String[] noAccessRoleValues = new String[]{"0", "false", "disabled"};
        for (String noAccessRoleValue : noAccessRoleValues) {
//            assertFalse(noAccessRoleValue + " was expected to result in access denied.", UserTokenXpathHelper.hasUserAdminRight(getUserToken(noAccessRoleValue)));
        }
    }


    private String getUserToken(String roleValue) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"4bd6ec15-f298-4cf8-9b46-d92775f6292c\">\n" +
                "    <uid>03124866-f094-42ca-9ff3-46d10fa4eff5</uid>\n" +
                "    <timestamp>1412079640658</timestamp>\n" +
                "    <lifespan>3600000</lifespan>\n" +
                "    <issuer></issuer>\n" +
                "    <securitylevel>1</securitylevel>\n" +
                "    <DEFCON>5</DEFCON>\n" +
                "    <username>totto@totto.org</username>\n" +
                "    <firstname>Thor Henning</firstname>\n" +
                "    <lastname>Hetland</lastname>\n" +
                "    <email>totto@totto.org</email>\n" +
                "    <personRef>22</personRef>\n" +
                "    <application ID=\"2219\">\n" +
                "        <applicationName>Whydah-UserAdminWebApp</applicationName>\n" +
                "        <organizationName>Altran</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"" + roleValue + "\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"100\">\n" +
                "        <applicationName>ACS</applicationName>\n" +
                "        <organizationName>Altran</organizationName>\n" +
                "        <role name=\"Employee\" value=\"totto@altran.com\"/>\n" +
                "    </application>\n" +
                "\n" +
                "    <ns2:link type=\"application/xml\" href=\"/4bd6ec15-f298-4cf8-9b46-d92775f6292c\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">21d3f0edf36cb5a0486b592fe84621</hash>\n" +
                "</usertoken>";
    }

    private String getUserTokenWithRoleName(String roleName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<usertoken xmlns:ns2=\"http://www.w3.org/1999/xhtml\" id=\"4bd6ec15-f298-4cf8-9b46-d92775f6292c\">\n" +
                "    <uid>03124866-f094-42ca-9ff3-46d10fa4eff5</uid>\n" +
                "    <timestamp>1412079640658</timestamp>\n" +
                "    <lifespan>3600000</lifespan>\n" +
                "    <issuer></issuer>\n" +
                "    <securitylevel>1</securitylevel>\n" +
                "    <DEFCON>5</DEFCON>\n" +
                "    <username>totto@totto.org</username>\n" +
                "    <firstname>Thor Henning</firstname>\n" +
                "    <lastname>Hetland</lastname>\n" +
                "    <email>totto@totto.org</email>\n" +
                "    <personRef>22</personRef>\n" +
                "    <application ID=\"2219\">\n" +
                "        <applicationName>Whydah-UserAdminWebApp</applicationName>\n" +
                "        <organizationName>Altran</organizationName>\n" +
                "        <role name=\""+roleName+"\" value=\"" + "true" + "\"/>\n" +
                "    </application>\n" +
                "    <application ID=\"100\">\n" +
                "        <applicationName>ACS</applicationName>\n" +
                "        <organizationName>Altran</organizationName>\n" +
                "        <role name=\"Employee\" value=\"totto@altran.com\"/>\n" +
                "    </application>\n" +
                "\n" +
                "    <ns2:link type=\"application/xml\" href=\"/4bd6ec15-f298-4cf8-9b46-d92775f6292c\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">21d3f0edf36cb5a0486b592fe84621</hash>\n" +
                "</usertoken>";
    }
}
