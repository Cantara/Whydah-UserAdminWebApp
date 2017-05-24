package net.whydah.identity;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.config.SSLTool;
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
    public static final int PORT_NO = 9996;
    //public static final String TESTURL = "http://localhost:" + PORT_NO + "/action";
	private static final Logger log = LoggerFactory.getLogger(ServerRunner.class);

	public static void main(String[] arguments) throws Exception {
		// Property-overwrite of SSL verification to support weak ssl certificates
		Properties properties = AppConfig.readProperties();
		if ("disabled".equalsIgnoreCase(properties.getProperty("sslverification"))) {
			SSLTool.disableCertificateValidation();

		}

		RuntimeDelegate.setInstance(new
				com.sun.jersey.server.impl.provider.RuntimeDelegateImpl());

		Server server = new Server(PORT_NO);
		ServletContextHandler context = new ServletContextHandler(server, "/useradmin");

		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		dispatcherServlet.setContextConfigLocation("classpath:webapp/web/mvc-config.xml");

		ServletHolder servletHolder = new ServletHolder(dispatcherServlet);
		context.addServlet(servletHolder, "/*");

		Path currentDir = getCurrentPath();
		Path tempUploadDir = currentDir.resolve("data_export_dir");
		createDirectories(tempUploadDir);

		server.start();
		server.join();
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
