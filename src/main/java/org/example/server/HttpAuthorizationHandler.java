package org.example.server;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface HttpAuthorizationHandler extends HttpSecurityHandler {
    AuthorizationResult authorize(HttpServletRequest request, Map<String, String> requestProperties)
            throws AuthorizationException;
}
