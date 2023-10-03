package acme.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Properties;

@Path("env")
public class EnvironmentResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Properties get() {
        return System.getProperties();
    }
}
