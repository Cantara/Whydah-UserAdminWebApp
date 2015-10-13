package net.whydah.identity.admin.usertoken;

import net.whydah.sso.user.UserHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


public class UserAdminRightsTest {


    private static final Logger log = LoggerFactory.getLogger(UserAdminRightsTest.class);


    @Test
    public void testAccessVerifyer() throws Exception {

        String testUserTokenWithoutRightRole = "";
        assertFalse(UserTokenXpathHelper.hasUserAdminRight(testUserTokenWithoutRightRole));

        testUserTokenWithoutRightRole = UserHelper.getDummyUserToken();
        assertFalse(UserTokenXpathHelper.hasUserAdminRight(testUserTokenWithoutRightRole));

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
                "    <application ID=\"19\">\n" +
                "        <applicationName>whydag.org</applicationName>\n" +
                "        <organizationName>Kunde 1</organizationName>\n" +
                "        <role name=\"WhydahUserAdmin\" value=\"Valla\"/>\n" +
                "    </application>\n" +
                " \n" +
                "    <ns2:link type=\"application/xml\" href=\"/\" rel=\"self\"/>\n" +
                "    <hash type=\"MD5\">8a37ef9624ed93db4873035b0de3d1ca</hash>\n" +
                "</usertoken>";
        assertTrue(UserTokenXpathHelper.hasUserAdminRight(testUserTokenWithRightRole));

    }


}
