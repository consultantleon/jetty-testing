package org.example.client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class ClientManager {
    private ClientBuilder clientBuilder;
    private Client client;

    public Client getClient() {
        return client;
    }

    public void init() {
        clientBuilder = ClientBuilder.newBuilder();
        final ClientConfig clientConfig = new ClientConfig();
        // Register our custom reader/writer implementations
        clientConfig.connectorProvider(new JettyConnectorProvider());
        clientBuilder = clientBuilder.withConfig(clientConfig);
        client = clientBuilder.build();
    }

    public void close() {
        client.close();
    }
}
