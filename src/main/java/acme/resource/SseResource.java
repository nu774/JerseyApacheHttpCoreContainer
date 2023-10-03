package acme.resource;

import acme.CharacterGenerator;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("sse")
public class SseResource {
    @GET
    @Produces("text/event-stream")
    public void get(@Context Sse sse, @Context SseEventSink sink) {
        final var builder = sse.newEventBuilder();
        long id = 0;
        for (final var line: new CharacterGenerator()) {
            final var event = builder
                    .id(String.valueOf(id++))
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(line)
                    .build();
            sink.send(event);
        }
    }
}
