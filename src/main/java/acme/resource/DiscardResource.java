package acme.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Path("discard")
public class DiscardResource {
    @PATCH
    public Response patch(InputStream src) {
        return discard(src);
    }
    @POST
    public Response post(InputStream src) {
        return discard(src);
    }
    @PUT
    public Response put(InputStream src) {
        return discard(src);
    }

    private Response discard(InputStream src) {
        try {
            src.transferTo(OutputStream.nullOutputStream());
            return Response.ok().build();
        } catch (IOException ioe) {
            throw new InternalServerErrorException();
        }
    }
}
