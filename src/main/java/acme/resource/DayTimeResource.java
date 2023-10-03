package acme.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Path("daytime")
public class DayTimeResource {
    private final DateTimeFormatter formatter;

    public DayTimeResource() {
        formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, u HH:mm:ss-z").withLocale(Locale.US);
    }
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return formatter.format(LocalDateTime.now().atZone(ZoneId.systemDefault())) + "\r\n";
    }
}
