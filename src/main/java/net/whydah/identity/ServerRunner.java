package net.whydah.identity;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.config.SSLTool;
import net.whydah.sso.config.ApplicationMode;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerRunner {
    public static int PORT_NO = 9996;
    public static final String CONTEXT = "/useradmin";

    //public static final String TESTURL = "http://localhost:" + PORT_NO + "/action";
    private static final Logger log = LoggerFactory.getLogger(ServerRunner.class);

    private Server server;
    private ServletContextHandler context;
    public static String version;


    public static String getHEALTHURL() {
        return "http://localhost:" + PORT_NO + CONTEXT + "/health";
    }


	public static void main(String[] arguments) throws Exception {
		// Property-overwrite of SSL verification to support weak ssl certificates
		Properties properties = AppConfig.readProperties();
		if ("disabled".equalsIgnoreCase(properties.getProperty("sslverification"))) {
			SSLTool.disableCertificateValidation();

		}
		Path currentDir = getCurrentPath();
		Path tempUploadDir = currentDir.resolve("data_export_dir");
		createDirectories(tempUploadDir);

        ServerRunner serverRunner = new ServerRunner();
        serverRunner.start();


        printConfiguration(AppConfig.readProperties());
//        WhydahUAWAServiceClient tc = new WhydahUAWAServiceClient();
        log.info("UserAdminWebApp started OK. Version = {},IAM_MODE = {}, url: http://localhost:{}{}/login",
                version, ApplicationMode.getApplicationMode(), String.valueOf(PORT_NO), CONTEXT);

        serverRunner.join();

	}


    public ServerRunner() throws IOException {
        this(PORT_NO);
    }

    public ServerRunner(int portNo) throws IOException {
        this.PORT_NO = portNo;
        server = new Server(PORT_NO);
        context = new ServletContextHandler(server, CONTEXT);
        version = this.getClass().getPackage().getImplementationVersion();


        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setContextConfigLocation("classpath:webapp/web/mvc-config.xml");
        ServletHolder servletHolder = new ServletHolder(dispatcherServlet);
        context.addServlet(servletHolder, "/*");

        RuntimeDelegate.setInstance(new
                com.sun.jersey.server.impl.provider.RuntimeDelegateImpl());

       
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public static void printConfiguration(Properties properties) {
        for (Object key : properties.keySet()) {
            log.info("Using Property: {}, value: {}", key, properties.get(key));
        }
    }

	public static Path getCurrentPath() {
		return
				Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		//Paths.get("").toAbsolutePath();
	}

	public static void createDirectories(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			Path dir;
			if ((dir = Files.createDirectories(directory)) != null) {
				log.trace("Created directory: {}", dir.toString());
			} else {
				log.trace("Unable to create directory: {}", dir.toString());
			}
		}
	}

}
