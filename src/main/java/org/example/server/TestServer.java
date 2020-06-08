package org.example.server;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServer {
    private static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private static final int PORT = 7478;
    private static final String ENDPOINT = "/api";
    private static final String WHOLE_PATH = "/*";
    private static final String ROLE_NAME = "compute";
    private static final String INTERFACE = "0.0.0.0";
    private Server server;

    private final HandlerCollection handlers = new HandlerCollection(true);

    public void startServer() {
        server = new Server(PORT);
        try (final ServerConnector httpServerConnector = new ServerConnector(server)) {
            httpServerConnector.setHost(INTERFACE);
        }

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(ServerApi.class);
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder servletHolder = new ServletHolder();
        servletHolder.setServlet(servletContainer);

        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath(ENDPOINT);
        servletContextHandler.addServlet(servletHolder, "/*");
        servletContextHandler.setAllowNullPathInfo(true);
        handlers.addHandler(servletContextHandler);

        final SecurityHandler authHandler = createBasicAuthenticationHandler("user", "pass");
        authHandler.setHandler(handlers);

        //sets server for all handlers in the chain
        authHandler.setServer(server);
        server.setHandler(authHandler);
        try {
            handlers.start();
            server.start();
        } catch (Exception e) {
            LOG.error("Failed to start embedded Web Server on interface {}, port {}", INTERFACE,
                    PORT, e);
        }
        LOG.info("Embedded web server started on port {}", PORT);

    }

    public void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            LOG.error("Failed to stop embedded Web Server on port {}", PORT, e);
        }
    }

    private static SecurityHandler createBasicAuthenticationHandler(String username,
            String password) {

        final UserStore userStore = new UserStore();
        userStore.addUser(username, Credential.getCredential(password), new String[]{ROLE_NAME});

        final HashLoginService loginService = new HashLoginService();
        loginService.setUserStore(userStore);

        final Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{ROLE_NAME});
        constraint.setAuthenticate(true);

        final ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec(WHOLE_PATH);

        final ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
        constraintSecurityHandler.setAuthenticator(new BasicAuthenticator());
        constraintSecurityHandler.addConstraintMapping(constraintMapping);
        constraintSecurityHandler.setLoginService(loginService);

        return constraintSecurityHandler;
    }
}
