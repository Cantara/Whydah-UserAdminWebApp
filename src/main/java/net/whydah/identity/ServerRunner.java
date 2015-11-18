package net.whydah.identity;

import net.whydah.identity.admin.config.AppConfig;
import net.whydah.identity.admin.config.SSLTool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.servlet.DispatcherServlet;

import javax.ws.rs.ext.RuntimeDelegate;
import java.util.Properties;

public class ServerRunner {
    public static final int PORT_NO = 9996;
    //public static final String TESTURL = "http://localhost:" + PORT_NO + "/action";

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

		server.start();
		server.join();
	}
}
