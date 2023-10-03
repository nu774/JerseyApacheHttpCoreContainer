package acme.resource;

import acme.CharacterGenerator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import java.nio.charset.StandardCharsets;

@Path("chargen")
public class CharacterGeneratorResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput get() {
        return (sink) -> {
            for (final var line: new CharacterGenerator()) {
                sink.write((line + "\r\n").getBytes(StandardCharsets.US_ASCII));
            }
        };
    }
}
