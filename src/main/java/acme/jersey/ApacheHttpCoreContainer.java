package acme.jersey;

import acme.http.VirtualThreadExecutorWrapper;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.io.IOCallback;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApacheHttpCoreContainer implements Container, HttpRequestHandler {
    private static final class OutputStreamWithTimeout extends OutputStream {
        private final OutputStream original;
        private final long timeout;
        private final TimeUnit timeUnit;
        private final ScheduledExecutorService scheduler;

        public OutputStreamWithTimeout(OutputStream original, ScheduledExecutorService scheduler, long timeout, TimeUnit timeUnit) {
            this.original = original;
            this.scheduler = scheduler;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }
        @Override
        public void write(int b) throws IOException {
            final var thread = Thread.currentThread();
            final var future = scheduler.schedule(thread::interrupt, timeout, timeUnit);
            original.write(b);
            future.cancel(false);
        }
        @Override
        public void write(byte[] b, int offset, int length) throws IOException {
            final var thread = Thread.currentThread();
            final var future = scheduler.schedule(thread::interrupt, timeout, timeUnit);
            original.write(b, offset, length);
            future.cancel(false);
        }
    }
    private static final class ResponseWriter implements ContainerResponseWriter {
        private final ExecutorService executor;
        private final ScheduledExecutorService scheduler;
        private final ClassicHttpResponse response;
        private OutputStream sink;
        private final Lock responseSetLock;
        private final Condition responseSetCondition;
        private final Lock outputStreamSetLock;
        private final Condition outputStreamSetCondition;
        private final Lock commitLock;
        private final Condition commitCondition;
        private Throwable throwable;
        private boolean isResponseSet;
        private boolean isCommit;
        private Runnable timeoutTask;
        private ScheduledFuture<?> timeoutFuture;

        ResponseWriter(final ExecutorService executor, final ScheduledExecutorService scheduler, final ClassicHttpResponse response) {
            this.executor = executor;
            this.scheduler = scheduler;
            this.response = response;
            this.responseSetLock = new ReentrantLock();
            this.responseSetCondition = this.responseSetLock.newCondition();
            this.outputStreamSetLock = new ReentrantLock();
            this.outputStreamSetCondition = this.outputStreamSetLock.newCondition();
            this.commitLock = new ReentrantLock();
            this.commitCondition = this.commitLock.newCondition();
            this.isResponseSet = false;
            this.isCommit = false;
        }
        @Override
        public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse context) throws ContainerException {
            try {
                response.setCode(context.getStatus());
                for (var entry: context.getHeaders().entrySet()) {
                    for (var value: entry.getValue()) {
                        response.addHeader(entry.getKey(), value);
                    }
                }
                if (contentLength != 0) {
                    var contentType = Optional.ofNullable(context.getMediaType())
                            .map(mediaType -> ContentType.create(mediaType.getType()))
                            .orElse(ContentType.APPLICATION_OCTET_STREAM);
                    IOCallback<OutputStream> cb = (os) -> {
                        var self = ResponseWriter.this;
                        self.outputStreamSetLock.lock();
                        try {
                            self.sink = new OutputStreamWithTimeout(os, scheduler,1, TimeUnit.MINUTES);
                            self.outputStreamSetCondition.signal();
                        } finally {
                            self.outputStreamSetLock.unlock();
                        }
                        self.commitLock.lock();
                        try {
                            while (!self.isCommit) {
                                self.commitCondition.await();
                            }
                        } catch (InterruptedException ie) {
                        } finally {
                            self.commitLock.unlock();
                        }
                        if (self.throwable != null)
                            throw new RuntimeException(self.throwable);
                    };
                    response.setEntity(HttpEntities.create(cb, contentType));
                }
                responseSetLock.lock();
                try {
                    isResponseSet = true;
                    responseSetCondition.signal();
                } finally {
                    responseSetLock.unlock();
                }
                if (contentLength != 0) {
                    outputStreamSetLock.lock();
                    try {
                        while (sink == null)
                            outputStreamSetCondition.await();
                    } finally {
                        outputStreamSetLock.unlock();
                    }
                }
                return sink;
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
        @Override
        public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
            timeoutTask = () -> executor.execute(() -> timeoutHandler.onTimeout(this));
            if (timeOut > 0) {
                timeoutFuture = scheduler.schedule(timeoutTask, timeOut, timeUnit);
            }
            return true;
        }
        @Override
        public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
            if (timeoutTask == null)
                throw new IllegalStateException();
            if (timeoutFuture != null && !timeoutFuture.cancel(false))
                return;
            timeoutFuture = scheduler.schedule(timeoutTask, timeOut, timeUnit);
        }
        @Override
        public void commit() {
            commitLock.lock();
            try {
                isCommit = true;
                commitCondition.signal();
            } finally {
                commitLock.unlock();
            }
        }
        @Override
        public void failure(Throwable error) {
            responseSetLock.lock();
            try {
                if (!isResponseSet) {
                    response.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    isResponseSet = true;
                    responseSetCondition.signal();
                }
            } finally {
                throwable = error;
                responseSetLock.unlock();
                commit();
            }
        }
        @Override
        public boolean enableResponseBuffering() {
            return false;
        }
        public void waitForReady() {
            responseSetLock.lock();
            try {
                while (!isResponseSet)
                    responseSetCondition.await();
            } catch (InterruptedException ie) {
            } finally {
                responseSetLock.unlock();
            }
        }
    }

    private volatile ApplicationHandler appHandler;
    private final ExecutorService executor = new VirtualThreadExecutorWrapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final static String[] contextAttributes = {
            HttpCoreContext.CONNECTION_ENDPOINT,
            HttpCoreContext.HTTP_REQUEST,
            HttpCoreContext.HTTP_RESPONSE,
            HttpCoreContext.SSL_SESSION,
    };

    public ApacheHttpCoreContainer(final Application application) {
        this.appHandler = new ApplicationHandler(application);
    }
    public ApacheHttpCoreContainer(final Application application, final Object parentContext) {
        this.appHandler = new ApplicationHandler(application, null, parentContext);
    }
    @Override
    public ResourceConfig getConfiguration() {
        return appHandler.getConfiguration();
    }
    @Override
    public ApplicationHandler getApplicationHandler() {
        return appHandler;
    }
    @Override
    public void reload() {
        reload(new ResourceConfig(getConfiguration()));
    }
    @Override
    public void reload(ResourceConfig configuration) {
        appHandler.onShutdown(this);
        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }
    @Override
    public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context) throws HttpException {
        try {
            final var properties = new MapPropertiesDelegate();
            for (final var attr: contextAttributes) {
                properties.setProperty(attr, context.getAttribute(attr));
            }
            final var requestContext = new ContainerRequest(
                    getBaseUri(request),
                    request.getUri(),
                    request.getMethod(),
                    getSecurityContext(context),
                    properties,
                    getConfiguration());
            for (var header: request.getHeaders()) {
                requestContext.headers(header.getName(), header.getValue());
            }
            final var entity = request.getEntity();
            if (entity != null) {
                try {
                    final var stream = entity.getContent();
                    requestContext.setEntityStream(stream);
                } catch (IOException ioe) {}
            }
            final var writer = new ResponseWriter(executor, scheduler, response);
            requestContext.setWriter(writer);
            executor.execute(() -> {
                try {
                    appHandler.handle(requestContext);
                } catch (Exception e) {
                    writer.failure(e);
                }
            });
            writer.waitForReady();
        } catch (Exception e) {
            throw new HttpException(e.getMessage());
        }
    }
    private URI getBaseUri(ClassicHttpRequest request) throws URISyntaxException {
        var uri = request.getUri();
        return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), "/", null, null);
    }
    private SecurityContext getSecurityContext(HttpContext context) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }
            @Override
            public boolean isUserInRole(String role) {
                return false;
            }
            @Override
            public boolean isSecure() {
                return context.getAttribute(HttpCoreContext.SSL_SESSION) != null;
            }
            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };
    }
}
