package org.example.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppServer {
    private TestServer server;

    public static void main(String[] args) throws Exception {
        new AppServer().startServer();
    }

    private void startServer() throws Exception {
        server = new TestServer();
        server.startServer();
        Thread.sleep(Long.MAX_VALUE);
    }
}
