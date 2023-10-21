package acme.jersey;

import acme.http.HttpServer;
import acme.http.ServerBootstrap;
import jakarta.ws.rs.core.Application;

import java.util.function.Consumer;

public class ApacheHttpCoreServerBootstrap {
    private final ServerBootstrap bootstrap = ServerBootstrap.bootstrap();
    final ApacheHttpCoreContainerProvider provider = new ApacheHttpCoreContainerProvider();
    final ApacheHttpCoreContainer container;

    public ApacheHttpCoreServerBootstrap(Application application) {
        this.container = provider.createContainer(ApacheHttpCoreContainer.class, application);
    }
    public ApacheHttpCoreServerBootstrap configure(Consumer<ServerBootstrap> consumer) {
        consumer.accept(this.bootstrap);
        return this;
    }
    public HttpServer create() {
        this.bootstrap.register("*", this.container);
        return this.bootstrap.create();
    }
}
