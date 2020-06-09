package org.example.client;

import org.glassfish.jersey.client.ClientLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class AuthFeature implements Feature {
    private static final Logger LOG = LoggerFactory.getLogger(AuthFeature.class);

    @Override
    public boolean configure(FeatureContext context) {
        LOG.info("Configure auth feature");
        final ClientManager clientManager = new ClientManager();
        try {
            clientManager.init();
        } catch (Exception e) {
            LOG.error("Failed to init client manager", e);
            return false;
        }
        context.register(new AuthFilter(clientManager));
        context.register(new ClientLifecycleListener() {
            @Override
            public void onInit() {
            }

            @Override
            public void onClose() {
                clientManager.close();
            }
        });
        return true;
    }
}
