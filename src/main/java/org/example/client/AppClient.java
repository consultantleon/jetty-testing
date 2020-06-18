package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class AppClient {
    private static final Logger LOG = LoggerFactory.getLogger(AppClient.class);
    private static final int MAX_TESTS = 1000;
    private static final int INTERVAL = 30000;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public AppClient(String realm, String clientId, String clientSecret) {
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public static void main(String[] args) {
        final String realm = args[0];
        final String clientId = args[1];
        final String clientSecret = args[2];

        new AppClient(realm, clientId, clientSecret).doTests();
    }

    private void doTests() {
        int successful = 0;
        int failures = 0;
        for (int n = 0; n < MAX_TESTS; n++) {
            if (n > 0) {
                delay();
            }
            final ClientManager clientManager = new ClientManager(realm, clientId, clientSecret);
            clientManager.init(true);
            try {
                LOG.info("Make request #{}", n);
                makePostRequest(clientManager, "http://localhost:7478/api/compute");
                successful++;
            } catch (RuntimeException e) {
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

    private void delay() {
        try {
            Thread.sleep(INTERVAL);
        } catch (InterruptedException e) {
            LOG.warn("interrupted", e);
        }
    }

    private void makePostRequest(ClientManager clientManager, String url) {
        LOG.info("Make request");
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
