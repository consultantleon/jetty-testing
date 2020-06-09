package org.example.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.example.jetty.JettyConnectorProviderCustom;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class ClientManager {
    private ClientBuilder clientBuilder;
    private Client client;

    public Client getClient() {
        return client;
    }

    public void init() {
        init(false);
    }

    public void init(boolean enableAuthFeature) {
        clientBuilder = ClientBuilder.newBuilder();
        final ClientConfig clientConfig = new ClientConfig();
        if (enableAuthFeature) {
            clientConfig.register(AuthFeature.class);
        }
        clientConfig.connectorProvider(new JettyConnectorProviderCustom());
        clientBuilder = clientBuilder.withConfig(clientConfig);
        client = clientBuilder.build();
        configureClient(client);
    }

    public void close() {
        client.close();
    }


    private void configureClient(final Client client) {
        // provide our own HttpClient so we get full control over its configuration,
        // (JettyClientProperties does not allow much control!)
        final SSLContext sslContext = client.getSslContext();
        final SslContextFactory sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setSslContext(sslContext);
        final HttpClient httpClient = new HttpClient(sslContextFactory);
        client.register(new JettyHttpClientSupplier(httpClient));

        // default request buffer size is 4KB which can be too small when the request
        // contains large tokens. It is quite standard to set the limit to at least 8KB.
        httpClient.setRequestBufferSize(16384);

        // default response buffer size is 16KB, that should be enough but if huge headers
        // are expected back this can be customized:
        httpClient.setResponseBufferSize(16384);

        // make sure the client times out earlier than the server to prevent spurious
        // earlyEof exceptions
        httpClient.setIdleTimeout(0);

        // remove specific protocol handlers, such as the WWW-Authenticate protocol handler
        // which causes our auth handlers to fail when a 401 UNAUTHORIZED is received
        // without the 'mandatory' WWW-Authenticate header.
        httpClient.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStarted(LifeCycle event) {
                ((HttpClient) event)
                        .getProtocolHandlers()
                        .remove(WWWAuthenticationProtocolHandler.NAME);
            }
        });
    }
}
