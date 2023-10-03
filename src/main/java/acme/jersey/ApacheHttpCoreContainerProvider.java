package acme.jersey;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Application;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.ContainerProvider;

public class ApacheHttpCoreContainerProvider implements ContainerProvider {
    @Override
    public <T> T createContainer(Class<T> type, Application application) throws ProcessingException {
        if (HttpRequestHandler.class == type || ApacheHttpCoreContainer.class == type) {
            return type.cast(new ApacheHttpCoreContainer(application));
        }
        return null;
    }
}
