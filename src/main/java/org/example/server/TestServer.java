package org.example.server;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class TestServer {
    private static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private static final int PORT = 7478;
    private static final String ENDPOINT = "/api";
    private static final String WHOLE_PATH = "/*";
    private static final String ROLE_NAME = "compute";
    private static final String INTERFACE = "0.0.0.0";
    private Server server;

    private final HandlerCollection handlers = new HandlerCollection(true);

    private final RequestLogHandler requestLogHandler = new RequestLogHandler();

    public void startServer() {
        server = new Server(PORT);
        try (final ServerConnector httpServerConnector = new ServerConnector(server)) {
            httpServerConnector.setHost(INTERFACE);
        }

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(ServerApi.class);
        resourceConfig.register(new RestLoggingFilter(getClass().getCanonicalName(), true));
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        final ServletHolder servletHolder = new ServletHolder();
        servletHolder.setServlet(servletContainer);

        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath(ENDPOINT);
        servletContextHandler.addServlet(servletHolder, "/*");
        servletContextHandler.setAllowNullPathInfo(true);

        requestLogHandler.setRequestLog(
                new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.NCSA_FORMAT));

        handlers.addHandler(servletContextHandler);

        final SecurityHandler authHandler = createSecurityHandler();
        authHandler.setHandler(handlers);
        requestLogHandler.setHandler(authHandler);
        //sets server for all handlers in the chain
        requestLogHandler.setServer(server);
        server.setHandler(requestLogHandler);
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

    private ConstraintSecurityHandler constructSecurityHandler() {
        final HttpSecurityHandler httpSecurityHandler = new CustomAuthResource();
        return HttpSecurityHandlerBasedSecurityHandler.create("custom-auth-security-handler",
                httpSecurityHandler, true);
    }


    public SecurityHandler createSecurityHandler() {
        // A security handler is a jetty handler that secures content behind a
        // particular portion of a url space. The ConstraintSecurityHandler is a
        // more specialized handler that allows matching of urls to different
        // constraints. The server sets this as the first handler in the chain,
        // effectively applying these constraints to all subsequent handlers in
        // the chain.
        ConstraintSecurityHandler security =
                HttpSecurityHandlerBasedSecurityHandler.create("auth", new CustomAuthResource(),
                        true);
        server.setHandler(security);

        // This constraint requires authentication and in addition that an
        // authenticated user be a member of a given set of roles for
        // authorization purposes.
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{"compute"});

        // Binds a url pattern with the previously created constraint. The roles
        // for this constraint mapping are mined from the Constraint itself
        // although methods exist to declare and bind roles separately as well.
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        // First you see the constraint mapping being applied to the handler as
        // a singleton list, however you can passing in as many security
        // constraint mappings as you like so long as they follow the mapping
        // requirements of the servlet api. Next we set a BasicAuthenticator
        // instance which is the object that actually checks the credentials
        // followed by the LoginService which is the store of known users, etc.
        security.setConstraintMappings(Collections.singletonList(mapping));
        return security;
    }
}
