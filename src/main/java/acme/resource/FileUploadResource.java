package acme.resource;

import acme.multipart.JerseyRequestContext;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

@Path("upload")
public class FileUploadResource {
    private final Logger logger = LoggerFactory.getLogger(FileUploadResource.class);

    @PATCH
    public Response patch(@Context HttpHeaders headers, InputStream inputStream) {
        return upload(headers, inputStream);
    }
    @POST
    public Response post(@Context HttpHeaders headers, InputStream inputStream) {
        return upload(headers, inputStream);
    }
    @PUT
    public Response put(@Context HttpHeaders headers, InputStream inputStream) {
        return upload(headers, inputStream);
    }
    private Response upload(HttpHeaders headers, InputStream inputStream) {
        final var requestContext = new JerseyRequestContext(headers, inputStream);
        final var fileUpload = new FileUpload();
        try {
            final var itemIterator = fileUpload.getItemIterator(requestContext);
            while (itemIterator.hasNext()) {
                final var item = itemIterator.next();
                if (item.isFormField()) continue;
                logger.info("start field={} name=[{}] type={}", item.getFieldName(), item.getName(), item.getContentType());
                final long transferred = item.openStream().transferTo(OutputStream.nullOutputStream());
                logger.info("transferred {} bytes", transferred);
            }
            return Response.ok().build();
        } catch (FileUploadBase.InvalidContentTypeException e) {
            throw new NotSupportedException();
        } catch (Exception e) {
            throw new InternalServerErrorException();
        }
    }
}
