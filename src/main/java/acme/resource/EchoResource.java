package acme.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

@Path("echo")
public class EchoResource {
    @PATCH
    public Response patch(@Context HttpHeaders headers, InputStream src) {
        return echo(headers, src);
    }
    @POST
    public Response post(@Context HttpHeaders headers, InputStream src) {
        return echo(headers, src);
    }
    @PUT
    public Response put(@Context HttpHeaders headers, InputStream src) {
        return echo(headers, src);
    }

    private Response echo(HttpHeaders headers, InputStream src) {
        return Response.ok(src).type(headers.getMediaType()).build();
    }
}
