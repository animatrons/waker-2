package com.waker;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("App launched.");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8888"));
        Server server = new Server(port);
        // Create HTTP Config
        HttpConfiguration httpConfiguration = new HttpConfiguration();
        // Add support for X-Forwarded headers
        httpConfiguration.addCustomizer(new ForwardedRequestCustomizer());
        // Create the http connector
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfiguration);
        ServerConnector connector = new ServerConnector(server, connectionFactory);
        // Make sure you set the port on the connector, the port in the Server constructor is overridden by the new connector
        connector.setPort(port);
        server.setConnectors(new ServerConnector[] { connector });

        WebAppContext webAppContext = new WebAppContext();
        server.setHandler(webAppContext);
        // Load static resources from the jar
        URL webAppDir = Main.class.getClassLoader().getResource("resources");
        assert webAppDir != null;
        webAppContext.setResourceBase(webAppDir.toURI().toString());
        // Enable annotations so the server sees classes annotated with @WebServlet.
        webAppContext
                .setConfigurations(new Configuration[] {
                        new AnnotationConfiguration(),
                        new WebInfConfiguration(),
                        new WebXmlConfiguration()
                });
        // Look for annotations in the classes' directory (dev server) and in the
        // jar file (live server)
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/target/classes/|.*waker-back.*\\.jar");

        ServletHolder defaultServletHolder = new ServletHolder(DefaultServlet.class);
        defaultServletHolder.setInitParameter("dirAllowed", "false");
        defaultServletHolder.setInitParameter("cacheControl", "public,max-age=3600,stale-while-revalidate=86400");
        /*webAppContext.addServlet(defaultServletHolder, "/assets/*");
        webAppContext.addServlet(defaultServletHolder, "/public/*");*/
        webAppContext.getServletHandler().addServlet(defaultServletHolder);

        // Start the server!
        server.start();
        System.out.println("Server started!");

        // Keep the main thread alive while the server is running.
        server.join();
    }
}