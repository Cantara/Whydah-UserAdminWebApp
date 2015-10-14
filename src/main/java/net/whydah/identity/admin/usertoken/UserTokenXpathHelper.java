package net.whydah.identity.admin.usertoken;

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

public class UserTokenXpathHelper {
    private static final Logger log = LoggerFactory.getLogger(UserTokenXpathHelper.class);

    public static String getUserTokenIdFromUserTokenXML(String userTokenXml) {
        if (userTokenXml == null) {
            log.trace("Empty  userToken");
            return "";
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/usertoken/@id";
            XPathExpression xPathExpression = xPath.compile(expression);
            return (xPathExpression.evaluate(doc));
        } catch (Exception e) {
            log.error("", e);
        }
        return "";
    }


    public static  String getApplicationTokenIdFromAppTokenXML(String appTokenXML) {
        log.trace("appTokenXML: {}", appTokenXML);
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(appTokenXML)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/applicationtoken/params/applicationtokenID[1]";
            XPathExpression xPathExpression = xPath.compile(expression);
            String appId = xPathExpression.evaluate(doc);
            log.trace("XML parse: applicationtokenID = {}", appId);
            return appId;
        } catch (Exception e) {
            log.error("getAppTokenIdFromAppToken - Could not get applicationID from XML: " + appTokenXML, e);
        }
        return "";
    }

    public static String getRealName(String userTokenXml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/usertoken/firstname[1]";
            XPathExpression xPathExpression =  xPath.compile(expression);
            String firstname = (xPathExpression.evaluate(doc));
            expression = "/usertoken/lastname[1]";
            xPathExpression = xPath.compile(expression);
            String lastname = (xPathExpression.evaluate(doc));
            return firstname + " " + lastname;
        } catch (Exception e) {
            log.error("", e);
        }
        return "";
    }

    public static boolean hasUserAdminRight(String userTokenXml) {
        if (true){
            return !(getRoleValueFromUserToken(userTokenXml, "19", "WhydahUserAdmin")==null);

        }
        if (userTokenXml == null || userTokenXml.length()<10) {
            log.trace("hasUserAdminRight - Empty  userToken");
            return false;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/usertoken/application[@ID=\"19\"]/role[@name=\"WhydahUserAdmin\"]/@value";
            XPathExpression xPathExpression = xPath.compile(expression);
            log.trace("hasUserAdminRight - token" + userTokenXml + "\nvalue:" + xPathExpression.evaluate(doc));
            String roleValue = (xPathExpression.evaluate(doc));
            if (roleValue != null) {
                if (roleValue.equals("false") || roleValue.equals("") || roleValue.equals("0") || roleValue.equals("disabled")) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("getTimestamp - userTokenXml timestamp parsing error", e);
        }
        return false;
    }


    public static Integer getLifespan(String userTokenXml) {
        if (userTokenXml == null){
            log.debug("userTokenXml was empty, so returning empty lifespan.");
            return null;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/usertoken/lifespan";
            XPathExpression xPathExpression = xPath.compile(expression);
            return Integer.parseInt(xPathExpression.evaluate(doc));
        } catch (Exception e) {
            log.error("getLifespan - userTokenXml lifespan parsing error", e);
        }
        return null;
    }

    public static Long getTimestamp(String userTokenXml) {
        if (userTokenXml==null){
            log.debug("userTokenXml was empty, so returning empty timestamp.");
            return null;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(userTokenXml)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/usertoken/timestamp";
            XPathExpression xPathExpression = xPath.compile(expression);
            log.debug("token" + userTokenXml + "\nvalue:" + xPathExpression.evaluate(doc));
            return Long.parseLong(xPathExpression.evaluate(doc));
        } catch (Exception e) {
            log.error("getTimestamp - userTokenXml timestamp parsing error", e);
        }
        return null;
    }


    public static String getRoleValueFromUserToken(String userTokenXml, String applicationId, String roleName) {
        String userRole = "";
        if (userTokenXml == null) {
            log.debug("userTokenXml was empty, so returning null.");
            return null;
        } else {
            String expression = "count(/usertoken/application[@ID='"+applicationId+"']/role[@name='"+roleName+"'])";
            userRole = findValue(userTokenXml, expression);
            if (userRole==null || "0".equalsIgnoreCase(userRole)){
                return null;
            }
            return findValue(userTokenXml,"/usertoken/application[@ID='"+applicationId+"']/role[@name='"+roleName+"']");
        }
    }

    public static String findValue(String xmlString,  String expression) {
        String value = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlString)));
            XPath xPath = XPathFactory.newInstance().newXPath();


            XPathExpression xPathExpression = xPath.compile(expression);
            value = xPathExpression.evaluate(doc);
        } catch (Exception e) {
            log.warn("Failed to parse xml. Expression {}, xml {}, ", expression, xmlString, e);
        }
        return value;
    }

}
