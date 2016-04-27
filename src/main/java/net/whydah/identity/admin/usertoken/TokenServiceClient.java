package net.whydah.identity.admin.usertoken;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class TokenServiceClient {
    private static final Logger log = LoggerFactory.getLogger(TokenServiceClient.class);

    private static WhydahApplicationSession was;


    public TokenServiceClient() throws IOException {
        String sts = AppConfig.readProperties().getProperty("tokenservice");
        String applicationid = AppConfig.readProperties().getProperty("applicationid");
        String applicationname = AppConfig.readProperties().getProperty("applicationname");
        String applicationsecret = AppConfig.readProperties().getProperty("applicationsecret");
        ApplicationCredential appCredential=new ApplicationCredential();

        appCredential.setApplicationID(applicationid);
        appCredential.setApplicationPassord(applicationsecret);
        was = WhydahApplicationSession.getInstance(sts,applicationid,applicationname,applicationsecret);
    }


    public String getUserTokenFromUserTokenId(String userTokenId) {
        String userTokenXML = new CommandGetUsertokenByUsertokenId(URI.create(was.getSTS()),was.getActiveApplicationTokenId(),was.getActiveApplicationTokenXML(),userTokenId).execute();
        if (userTokenXML==null || userTokenXML.length()<10){
            throw new RuntimeException("getUserTokenFromUserTokenId failed " );
        }
        return userTokenXML;
    }


    public String getUserTokenByUserTicket(String userticket) {
        String userTokenXML = new CommandGetUsertokenByUserticket(URI.create(was.getSTS()),was.getActiveApplicationTokenId(),was.getActiveApplicationTokenXML(),userticket).execute();
        if (userTokenXML==null || userTokenXML.length()<10){
            throw new RuntimeException("getUserTokenByUserTicket failed " );
        }
        return userTokenXML;
    }



    public static Integer calculateTokenRemainingLifetimeInSeconds(String userTokenXml) {
        Integer tokenLifespanMs = UserTokenXpathHelper.getLifespan(userTokenXml);
        Long tokenTimestampMsSinceEpoch = UserTokenXpathHelper.getTimestamp(userTokenXml);

        if (tokenLifespanMs == null || tokenTimestampMsSinceEpoch == null) {
            return null;
        }

        long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanMs;
        long remainingLifeMs = endOfTokenLifeMs - System.currentTimeMillis();
        return (int) (remainingLifeMs / 1000);
    }


}

