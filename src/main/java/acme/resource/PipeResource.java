package acme.resource;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
@Path("pipe")
public class PipeResource {
    private static class PipeContext {
        private final InputStream inputStream;
        private boolean connected;
        private boolean completed;
        private Throwable throwable;
        private final Lock completionLock = new ReentrantLock();
        private final Condition completionCondition = completionLock.newCondition();

        public PipeContext(InputStream stream) {
            this.inputStream = stream;
        }
        public InputStream connect() {
            this.connected = true;
            return this.inputStream;
        }
        public void complete(Throwable throwable) {
            this.completionLock.lock();
            try {
                this.throwable = throwable;
                this.completed = true;
                this.completionCondition.signal();
            } finally {
                this.completionLock.unlock();
            }
        }
        public Throwable awaitForCompletion(long time, TimeUnit unit) {
            this.completionLock.lock();
            try {
                if (!this.completionCondition.await(time, unit) && !this.connected)
                    throw new InterruptedException();
                while (!this.completed)
                    this.completionCondition.await();
            } catch (InterruptedException ie) {
                this.throwable = ie;
            } finally {
                this.completionLock.unlock();
            }
            return this.throwable;
        }
    }
    private final ConcurrentMap<String, PipeContext> pipes = new ConcurrentHashMap<>();

    @PUT
    @Path("{name}")
    public Response put(@PathParam("name") final String name, final InputStream source) throws Throwable {
        final var pipe = new PipeContext(source);
        if (pipes.putIfAbsent(name, pipe) != null)
            throw new ForbiddenException();
        Throwable throwable = pipe.awaitForCompletion(1, TimeUnit.MINUTES);
        pipes.remove(name);
        if (throwable != null)
            throw new IOException("Broken pipe");
        return Response.ok().build();
    }
    @GET
    @Path("{name}")
    public StreamingOutput get(@PathParam("name") final String name) {
        final var pipe = pipes.remove(name);
        if (pipe == null)
            throw new NotFoundException();
        return (sink) -> {
            try {
                pipe.connect().transferTo(sink);
                pipe.complete(null);
            } catch (IOException ioe) {
                pipe.complete(ioe);
                throw ioe;
            }
        };
    }
}
