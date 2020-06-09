package org.example.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface HttpPermissionHandler extends HttpSecurityHandler {
    boolean checkPermission(String pathInContext, HttpServletRequest request,
            HttpServletResponse response, Map<String, String> requestProperties)
            throws AuthorizationException;
}
