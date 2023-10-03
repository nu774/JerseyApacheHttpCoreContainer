package acme.resource;

import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.InputStream;
import java.util.Base64;

@Path("base64")
public class Base64Resource {
    @PATCH
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput patch(InputStream src) {
        return base64(src);
    }
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput post(InputStream src) {
        return base64(src);
    }
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput put(InputStream src) {
        return base64(src);
    }

    private StreamingOutput base64(InputStream src) {
        return (sink) -> {
            final var encoder = Base64.getMimeEncoder(76, "\r\n".getBytes());
            try (final var wrappedSink = encoder.wrap(sink)) {
                src.transferTo(wrappedSink);
            }
        };
    }
}
