package org.example.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authorization Result is created after authorization
 */
public class AuthorizationResult {
    private final boolean authenticated;
    private final String username;
    private final String credential;
    private final Set<String> roles = new HashSet<>();
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Authorization Result is created after authorization
     */
    public AuthorizationResult() {
        this.authenticated = false;
        this.username = null;
        this.credential = null;
    }

    /**
     * Authorization Result is created after authorization
     * @param username extracted username
     * @param credential extracted credential
     * @param roles extracted roles
     */
    public AuthorizationResult(String username, String credential, Collection<String> roles) {
        this.authenticated = true;
        this.username = username;
        this.credential = credential;
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    /**
     * Authorization Result is created after authorization
     * @param username extracted username
     * @param credential extracted credential
     * @param roles extracted roles
     * @param properties extracted properties
     */
    public AuthorizationResult(String username, String credential, Collection<String> roles,
            Map<String, String> properties) {
        this(username, credential, roles);
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getUsername() {
        return username;
    }

    public String getCredential() {
        return credential;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
