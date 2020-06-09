package org.example.server;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

final class HttpSecurityHandlerBasedSecurityHandler extends ConstraintSecurityHandler {
    private static final Logger LOG =
            LoggerFactory.getLogger(HttpSecurityHandlerBasedSecurityHandler.class);

    private final String logKey;
    private final HttpPermissionHandler httpPermissionHandler;

    private HttpSecurityHandlerBasedSecurityHandler(String logKey,
            HttpSecurityHandler httpSecurityHandler) {
        this.logKey = logKey;
        if (httpSecurityHandler instanceof HttpPermissionHandler) {
            this.httpPermissionHandler = (HttpPermissionHandler) httpSecurityHandler;
        } else {
            this.httpPermissionHandler = null;
        }
    }

    private void init(boolean authenticatorAuthoritative, HttpSecurityHandler httpSecurityHandler) {
        final HttpAuthorizationHandler httpAuthorizationHandler;
        if (httpSecurityHandler instanceof HttpAuthorizationHandler) {
            httpAuthorizationHandler = (HttpAuthorizationHandler) httpSecurityHandler;
        } else {
            httpAuthorizationHandler = null;
        }
        this.setAuthenticator(
                new HttpAuthorizationHandlerAuthenticator(logKey, httpAuthorizationHandler,
                        authenticatorAuthoritative));

        setDenyUncoveredHttpMethods(true);

        setIdentityService(new DefaultIdentityService());
    }

    static HttpSecurityHandlerBasedSecurityHandler create(String logKey,
            HttpSecurityHandler httpSecurityHandler, boolean authenticatorAuthoritative) {
        final HttpSecurityHandlerBasedSecurityHandler httpSecurityHandlerBasedSecurityHandler =
                new HttpSecurityHandlerBasedSecurityHandler(logKey, httpSecurityHandler);
        httpSecurityHandlerBasedSecurityHandler.init(authenticatorAuthoritative,
                httpSecurityHandler);
        return httpSecurityHandlerBasedSecurityHandler;
    }

    @SuppressWarnings("squid:S1142")
    @Override
    protected boolean checkUserDataPermissions(String pathInContext, Request request,
            Response response, RoleInfo roleInfo) throws IOException {
        if (!super.checkUserDataPermissions(pathInContext, request, response, roleInfo)) {
            LOG.trace("{}: no permission handler, permission check result=false", logKey);
            return false;
        }
        if (httpPermissionHandler == null) {
            LOG.trace("{}: no permission handler, permission check result=true", logKey);
            return true;
        }

        try {
            final boolean permissionCheckResult =
                    httpPermissionHandler.checkPermission(pathInContext, request, response,
                            Collections.emptyMap());
            LOG.trace("{}: permission handler check result={}", logKey, permissionCheckResult);
            return permissionCheckResult;
        } catch (AuthorizationException e) {
            LOG.warn("{}: permission handler exception, HTTP Status={}: {}", logKey,
                    e.getStatusCode(), e.getMessage(), e);
            if (StringUtils.isNotEmpty(e.getChallenge())) {
                response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), e.getChallenge());
            }
            try {
                response.sendError(e.getStatusCode());
                request.setHandled(true);
            } catch (RuntimeException | IOException e2) {
                LOG.error("{}: failed to send error response", logKey, e2);
            }
            return false;
        }
    }
}
