package acme.resource;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Path("async")
public class AsyncResource {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();
    @GET
    public void get(final @Suspended AsyncResponse response) {
        final var future = scheduler.schedule(() -> {
            response.resume(Response.ok().build());
        }, random.nextInt(2000), TimeUnit.MILLISECONDS);
        response.setTimeout(1, TimeUnit.SECONDS);
        response.setTimeoutHandler((resp) -> {
            if (future.cancel(false))
                resp.resume(Response.status(Response.Status.GATEWAY_TIMEOUT).build());
        });
    }
}
