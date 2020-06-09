package org.example.server;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

class HttpAuthorizationHandlerAuthenticator implements Authenticator {
    private static final Logger LOG =
            LoggerFactory.getLogger(HttpAuthorizationHandlerAuthenticator.class);

    private static final String AUTHENTICATION_METHOD = "AUTHORIZATION_HANDLER";
    public static final String ANONYMOUS_USER = "anonymous";

    private final String logKey;
    private final HttpAuthorizationHandler httpAuthorizationHandler;
    private final boolean authenticatorAuthoritative;
    private AuthConfiguration authConfiguration;

    HttpAuthorizationHandlerAuthenticator(String logKey,
            HttpAuthorizationHandler httpAuthorizationHandler, boolean authenticatorAuthoritative) {
        this.logKey = logKey;
        this.httpAuthorizationHandler = httpAuthorizationHandler;
        this.authenticatorAuthoritative = authenticatorAuthoritative;
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        this.authConfiguration = configuration;
    }

    @Override
    public String getAuthMethod() {
        return AUTHENTICATION_METHOD;
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        // do nothing
    }

    @Override
    @SuppressWarnings({"squid:S1142", "squid:S2221"})
    public Authentication validateRequest(ServletRequest request, ServletResponse response,
            boolean mandatory) throws ServerAuthException {
        if (httpAuthorizationHandler == null) {
            LOG.trace("{}: skip authentication: no authenticator handler", logKey);
            return Authentication.NOT_CHECKED;
        }
        if (!mandatory) {
            LOG.trace("{}: skip authentication: not mandatory", logKey);
            return Authentication.NOT_CHECKED;
        }
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        try {

            final AuthorizationResult authorizationResult =
                    httpAuthorizationHandler.authorize(httpServletRequest, Collections.emptyMap());
            if (authorizationResult.isAuthenticated()) {
                LOG.trace("{}: authenticated, user={}, roles={}", logKey,
                        authorizationResult.getUsername(), authorizationResult.getRoles());
                return new UserAuthentication(getAuthMethod(),
                        createIdentity(authorizationResult.getUsername(),
                                authorizationResult.getCredential(),
                                authorizationResult.getRoles()));
            } else {
                LOG.trace("{}: authorization handler has not authenticated the request", logKey);
                return Authentication.NOT_CHECKED;
            }
        } catch (AuthorizationException e) {
            if (authenticatorAuthoritative) {
                if (LOG.isTraceEnabled()) {
                    LOG.warn(
                            "{}: authorization handler exception, HTTP status={}, challenge={}: {}",
                            logKey, e.getStatusCode(), e.getChallenge(), e.getMessage(), e);
                } else {
                    LOG.warn(
                            "{}: authorization handler exception, HTTP status={}, challenge={}: {}",
                            logKey, e.getStatusCode(), e.getChallenge(), e.getMessage());
                }
                if (StringUtils.isNotEmpty(e.getChallenge())) {
                    httpServletResponse.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(),
                            e.getChallenge());
                }
                try {
                    httpServletResponse.sendError(e.getStatusCode());
                } catch (Exception e2) {
                    LOG.error("{}: failed to send error response", logKey, e2);
                }
                return Authentication.SEND_FAILURE;
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.warn("{}: non-authoritative authorization failure, HTTP status={}, "
                             + "challenge={}: {}", logKey, e.getStatusCode(), e.getChallenge(),
                            e.getMessage(), e);
                } else {
                    LOG.warn("{}: non-authoritative authorization failure, HTTP status={}, "
                             + "challenge={}: {}", logKey, e.getStatusCode(), e.getChallenge(),
                            e.getMessage());
                }
                return Authentication.NOT_CHECKED;
            }
        } catch (Exception e) {
            LOG.error("{}: authorization handler error", logKey, e);
            throw new ServerAuthException("failed authorize request", e);
        }
    }

    private UserIdentity createIdentity(String username, String sCredential, Set<String> roles) {
        final Credential credential =
                Credential.getCredential(sCredential == null ? "" : sCredential);
        final Principal userPrincipal =
                () -> username == null ? ANONYMOUS_USER : username;
        final Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        subject.getPrivateCredentials().add(credential);
        subject.setReadOnly();
        return authConfiguration
                .getIdentityService()
                .newUserIdentity(subject, userPrincipal, roles.toArray(new String[0]));
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response,
            boolean mandatory, Authentication.User validatedUser) {
        return true;
    }
}
