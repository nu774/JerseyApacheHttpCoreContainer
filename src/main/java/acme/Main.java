package acme;

import acme.jersey.ApacheHttpCoreServerBootstrap;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        //SLF4JBridgeHandler.install();

        var listenAddress = new InetSocketAddress(8080);
        if (args.length > 0) {
            URI uri = new URI("http://" + args[0]);
            listenAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
        }
        final var app = new App();
        final var socketConfig = SocketConfig
                .custom()
                .setSoReuseAddress(true)
                .setBacklogSize(1024)
                .build();
        final var http1Config = Http1Config
                .custom()
                .setChunkSizeHint(1) // We need this for immediate echo back
                .build();
        final var finalListenAddress = listenAddress;
        final var server = new ApacheHttpCoreServerBootstrap(app)
                .configure(bootstrap -> {
                    bootstrap.setLocalAddress(finalListenAddress.getAddress())
                            .setListenerPort(finalListenAddress.getPort())
                            .setSocketConfig(socketConfig)
                            .setHttp1Config(http1Config)
                            .setStreamListener(app)
                            .setExceptionListener(app);
                })
                .create();
        app.start(server);
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> server.close(CloseMode.IMMEDIATE)));
        logger.info("listening at {}:{}", server.getInetAddress().getHostAddress(), server.getLocalPort());
        server.awaitTermination(TimeValue.MAX_VALUE);
    }
}
