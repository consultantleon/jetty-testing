package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
                makePostRequest(clientManager, "http://localhost:7478/api/compute");
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

    private void makePostRequest(ClientManager clientManager, String url) throws Exception {
        LOG.info("Make request ASYNC");
        final Response response = clientManager
                .getClient()
                .target(url)
                .request()
                .build("PUT", Entity.entity("[1,2,3,10]", MediaType.APPLICATION_JSON_TYPE))
                .invoke();
        LOG.info("RESPONSE, status={}: {}", response.getStatus(),
                response.readEntity(String.class));
    }
}
