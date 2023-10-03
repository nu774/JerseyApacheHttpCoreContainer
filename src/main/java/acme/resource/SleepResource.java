package acme.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;

@Path("sleep/{seconds}")
public class SleepResource {
    @GET
    public Response get(@PathParam("seconds") final long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
            return Response.ok().build();
        } catch (InterruptedException ie) {
            throw new InternalServerErrorException();
        }
    }
}
