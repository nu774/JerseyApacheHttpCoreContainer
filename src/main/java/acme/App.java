package acme;

import acme.http.HttpServer;
import acme.resource.AsyncResource;
import acme.resource.CharacterGeneratorResource;
import acme.resource.DayTimeResource;
import acme.resource.DiscardResource;
import acme.resource.EchoResource;
import acme.resource.EnvironmentResource;
import acme.resource.FileUploadResource;
import acme.resource.PipeResource;
import acme.resource.SleepResource;
import acme.resource.SseResource;
import org.apache.hc.core5.http.ExceptionListener;
import org.apache.hc.core5.http.HttpConnection;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.impl.Http1StreamListener;
import org.glassfish.jersey.gson.JsonGsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class App extends ResourceConfig implements Http1StreamListener, ExceptionListener {
    private final Logger logger = LoggerFactory.getLogger(App.class);
    private HttpServer server;

    public App() {
        register(JsonGsonFeature.class);
        packages("acme.resource");
    }
    public void start(HttpServer server) throws IOException {
        this.server = server;
        this.server.start();
    }
    public HttpServer getServer() {
        return server;
    }
    @Override
    public void onRequestHead(HttpConnection connection, HttpRequest request) {
        logger.info("{}: {} {}", connection.getRemoteAddress(), request.getMethod(), request.getPath());
    }
    @Override
    public void onResponseHead(HttpConnection connection, HttpResponse response) {
        logger.info("{}: {} {}", connection.getRemoteAddress(), response.getCode(), response.getReasonPhrase());
    }
    @Override
    public void onExchangeComplete(HttpConnection connection, boolean keepAlive) {
        logger.info("{}: exchange completed: keepAlive={}", connection.getRemoteAddress(), keepAlive);
    }
    @Override
    public void onError(Exception ex) {
        logger.error("{}", ex.toString());
    }
    @Override
    public void onError(HttpConnection connection, Exception ex) {
        logger.error("{}: {}", connection.getRemoteAddress(), ex.toString());
    }
}
