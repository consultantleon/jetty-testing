package org.example.server;


import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class CustomAuthResource implements HttpAuthorizationHandler {
    @Override
    public AuthorizationResult authorize(HttpServletRequest request,
            Map<String, String> requestProperties) throws AuthorizationException {
        final String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new AuthorizationException(HttpServletResponse.SC_UNAUTHORIZED,
                    "please provide a valid token", "Custom realm=\"Custom\"");
        } else {
            return new AuthorizationResult(null, token, Lists.newArrayList("compute"));
        }
    }
}
