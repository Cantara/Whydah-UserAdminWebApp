package net.whydah.identity.admin;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.session.baseclasses.BaseWhydahServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WhydahServiceClient extends BaseWhydahServiceClient {
    private static final Logger log = LoggerFactory.getLogger(WhydahServiceClient.class);



    public WhydahServiceClient()  throws IOException {

        super(AppConfig.readProperties());
    }


}

