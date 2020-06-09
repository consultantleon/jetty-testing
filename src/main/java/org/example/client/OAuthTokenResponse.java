package org.example.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
class OAuthTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private Integer expiresIn;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("refresh_expires_in")
    private Integer refreshExpiresIn;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("state")
    private String state;
    @JsonProperty("session_state")
    private String sessionState;

    private OAuthTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Integer getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getSessionState() {
        return sessionState;
    }

    public boolean hasAccessToken() {
        return StringUtils.isNotEmpty(accessToken);
    }

    public boolean hasRefreshToken() {
        return StringUtils.isNotEmpty(refreshToken);
    }

    @Override
    public String toString() {
        return "OAuthTokenResponse{accessToken='" + accessToken + '\'' + ", expiresIn=" + expiresIn
               + ", refreshToken='" + refreshToken + '\'' + ", refreshExpiresIn=" + refreshExpiresIn
               + ", tokenType='" + tokenType + '\'' + ", scope='" + scope + '\'' + ", state='"
               + state + '\'' + ", sessionState='" + sessionState + '\'' + '}';
    }
}
