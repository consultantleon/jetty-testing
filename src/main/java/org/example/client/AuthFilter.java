package org.example.client;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.InputStream;

public class AuthFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuthFilter.class);

    private static final String TOKEN_URL =
            "http://localhost:8080/auth/realms/myclient/protocol/openid-connect/token";
    private static final String IS_REPEAT_REQUEST = "repeat.request";
    private static final String SCOPE = "compute";
    private static final String CLIENT_ID = "myclient";
    private static final String CLIENT_SECRET = "512d71d4-d36b-4a6b-880e-82a008125972";
    private static final String REQUEST_FORM_FIELD_GRANT_TYPE = "grant_type";
    private static final String REQUEST_FORM_FIELD_SCOPE = "scope";
    private static final String REQUEST_FORM_FIELD_CLIENT_ID = "client_id";
    private static final String REQUEST_FORM_FIELD_CLIENT_SECRET = "client_secret";
    private final ClientManager clientManager;

    public AuthFilter(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {

    }

    @Override
    public void filter(ClientRequestContext clientRequestContext,
            ClientResponseContext clientResponseContext) {
        LOG.info("response filter, response status={}", clientResponseContext.getStatus());
        final Response.StatusType status = clientResponseContext.getStatusInfo();
        if (status.getStatusCode() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            performLoginAndRetry(clientRequestContext, clientResponseContext);
        }
    }

    private void performLoginAndRetry(ClientRequestContext requestContext,
            ClientResponseContext responseContext) {
        LOG.info("performLoginAndRetry");
        try {
            performLogin(requestContext);
        } catch (RuntimeException e) {
            throw new WebApplicationException("Login failed", e);
        }
        repeatRequest(requestContext, responseContext);
    }

    private void performLogin(ClientRequestContext clientRequestContext) {
        final String token = extractTokenDataFromResponse(doLoginRequest());
        addTokenToRequest(clientRequestContext, token);
    }

    private String extractTokenDataFromResponse(Response response) {
        response.bufferEntity();
        final OAuthTokenResponse oAuthTokenResponse = response.readEntity(OAuthTokenResponse.class);
        if (oAuthTokenResponse == null) {
            throw new WebApplicationException("No authentication data received", response);
        }
        if (!oAuthTokenResponse.hasAccessToken()) {
            throw new WebApplicationException(
                    "token response received does not contain " + "access token", response);
        }
        return oAuthTokenResponse.getAccessToken();
    }

    private Response doLoginRequest() {
        LOG.info("doLoginRequest");

        final WebTarget target = clientManager.getClient().target(TOKEN_URL);
        final Form formData = new Form();
        formData.param(REQUEST_FORM_FIELD_GRANT_TYPE, "client_credentials");
        formData.param(REQUEST_FORM_FIELD_SCOPE, SCOPE);
        formData.param(REQUEST_FORM_FIELD_CLIENT_ID, CLIENT_ID);
        formData.param(REQUEST_FORM_FIELD_CLIENT_SECRET, CLIENT_SECRET);
        return target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .post(Entity.form(formData));
    }

    public static void addTokenToRequest(ClientRequestContext request, final String token) {
        final String authorizationHeaderValue = "Bearer " + token;
        request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
    }

    public static void repeatRequest(ClientRequestContext requestContext,
            ClientResponseContext responseContext) {

        LOG.info("repeatRequest");

        final WebTarget target = requestContext.getClient().target(requestContext.getUri());

        // create request builder for original acceptable media type(s)
        Invocation.Builder builder =
                target.request(requestContext.getAcceptableMediaTypes().toArray(new MediaType[0]));

        // copy original request headers
        //noinspection Convert2Diamond
        builder = builder
                .headers(new MultivaluedHashMap<String, Object>(requestContext.getHeaders()))
                .property(IS_REPEAT_REQUEST, Boolean.TRUE);

        // set original method + if original request had entity set the entity
        final Invocation invocation;
        if (requestContext.hasEntity()) {
            invocation = builder.build(requestContext.getMethod(),
                    Entity.entity(requestContext.getEntity(), requestContext.getMediaType()));
        } else {
            invocation = builder.build(requestContext.getMethod());
        }
        final Response repeatResponse = invocation.invoke();
        LOG.info("request repeated, status={}", repeatResponse.getStatus());
        copyResponseToResponseContext(repeatResponse, responseContext);
    }

    public static void copyResponseToResponseContext(Response response,
            ClientResponseContext responseContext) {
        responseContext.setStatus(response.getStatus());
        if (response.hasEntity()) {
            responseContext.setEntityStream(response.readEntity(InputStream.class));
        } else {
            responseContext.setEntityStream(null);
        }
        final MultivaluedMap<String, String> headers = responseContext.getHeaders();
        headers.clear();
        headers.putAll(response.getStringHeaders());
    }
}
