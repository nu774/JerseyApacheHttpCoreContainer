package acme.multipart;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.fileupload.RequestContext;

import java.io.IOException;
import java.io.InputStream;

public class JerseyRequestContext implements RequestContext {
    private final HttpHeaders headers;
    private final InputStream inputStream;
    private final MediaType mediaType;
    private String charset;

    public JerseyRequestContext(HttpHeaders headers, InputStream inputStream) {
        this.headers = headers;
        this.inputStream = inputStream;
        headers.getMediaType();
        mediaType = headers.getMediaType();
        if (mediaType != null) {
            charset = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
        }
    }
    @Override
    public String getCharacterEncoding() {
        return charset;
    }
    @Override
    public String getContentType() {
        return mediaType == null ? null : mediaType.toString();
    }
    @Override
    public int getContentLength() {
        return headers.getLength();
    }
    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }
}
