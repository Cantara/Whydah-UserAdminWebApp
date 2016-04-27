package net.whydah.identity.admin.usertoken;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class TokenServiceClient {
    private static final Logger log = LoggerFactory.getLogger(TokenServiceClient.class);

    private static WhydahApplicationSession was;
    private UserToken activeUserToken;


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

    public WhydahApplicationSession getWAS(){
        return was;
    }

    public String getUserTokenFromUserTokenId(String userTokenId) {
        String userTokenXML = new CommandGetUsertokenByUsertokenId(URI.create(was.getSTS()),was.getActiveApplicationTokenId(),was.getActiveApplicationTokenXML(),userTokenId).execute();
        if (userTokenXML==null || userTokenXML.length()<10){
            throw new RuntimeException("getUserTokenFromUserTokenId failed " );
        }
        activeUserToken = UserTokenMapper.fromUserTokenXml(userTokenXML);
        return userTokenXML;
    }


    public String getUserTokenByUserTicket(String userticket) {
        String userTokenXML = new CommandGetUsertokenByUserticket(URI.create(was.getSTS()),was.getActiveApplicationTokenId(),was.getActiveApplicationTokenXML(),userticket).execute();
        if (userTokenXML==null || userTokenXML.length()<10){
            throw new RuntimeException("getUserTokenByUserTicket failed " );
        }
        activeUserToken = UserTokenMapper.fromUserTokenXml(userTokenXML);
        return userTokenXML;
    }

    public String getMyUserTokenId(){
        return activeUserToken.getTokenid();
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

