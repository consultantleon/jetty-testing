package org.example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class ServerApi {
//    private static final Logger LOG = LoggerFactory.getLogger(ServerApi.class);

    @PUT
    @Path("/compute")
    public void putTextHandler(@Suspended final AsyncResponse ar, final String msg,
            @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest request) {
//        try {
//            Thread.sleep(150);
//        } catch (InterruptedException e) {
//            LOG.trace("interrupted", e);
//        }
        ar.resume("{ \"result\" : \"60\"}");
    }
}
