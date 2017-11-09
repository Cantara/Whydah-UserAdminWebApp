package net.whydah.identity.admin.dao;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.sso.session.baseclasses.BaseWhydahServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class WhydahUAWAServiceClient extends BaseWhydahServiceClient {
    private static final Logger log = LoggerFactory.getLogger(WhydahUAWAServiceClient.class);


    public WhydahUAWAServiceClient() throws IOException {
        super(AppConfig.readProperties());
    }

    public WhydahUAWAServiceClient(Properties pros) throws IOException {
        super(pros);
    }

}

