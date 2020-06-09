package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Future;

public class AppClient {
    private static final Logger LOG = LoggerFactory.getLogger(AppClient.class);
    private static final int MAX_TESTS = 100;

    public static void main(String[] args) {
        new AppClient().doTests();
    }

    private void doTests() {
        int successful = 0;
        int failures = 0;
        for (int n = 0; n < MAX_TESTS && failures == 0; n++) {
            final ClientManager clientManager = new ClientManager();
            clientManager.init(true);
            try {
                LOG.info("Make request #{}", n);
                makePostRequestAsync(clientManager, "http://localhost:7478/api/compute");
                successful++;
            } catch (Exception e) {
                LOG.error("Caught exception", e);
                failures++;
            } finally {
                clientManager.close();
            }
        }
        if (failures > 0) {
            LOG.warn("Test #{} failed", successful + 1);
        } else {
            LOG.info("All {} tests succeeded", MAX_TESTS);
        }
    }

    private void makePostRequestAsync(ClientManager clientManager, String url) throws Exception {
        LOG.info("Make request ASYNC");
        try {
            final Future<Response> responseFuture = clientManager
                    .getClient()
                    .target(url)
                    .request()
                    .async()
                    .method("PUT", Entity.entity("[1,2,3,10]", MediaType.APPLICATION_JSON_TYPE));
            final Response response = responseFuture.get();
            LOG.info("RESPONSE, status={}: {}", response.getStatus(),
                    response.readEntity(String.class));
        } catch (RuntimeException e) {
            LOG.error("Request failed", e);
        }
    }

    private void makePostRequestSync(ClientManager clientManager, String url) {
        LOG.info("Make request SYNC");
        try {
            final Response response = clientManager
                    .getClient()
                    .target(url)
                    .request()
                    .method("PUT", Entity.entity("[1,2,3,10]", MediaType.APPLICATION_JSON_TYPE));

            LOG.info("RESPONSE, status={}: {}", response.getStatus(),
                    response.readEntity(String.class));
        } catch (RuntimeException e) {
            LOG.error("Request failed", e);
        }
    }
}
