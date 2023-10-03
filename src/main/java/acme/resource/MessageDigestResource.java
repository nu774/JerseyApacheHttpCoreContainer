package acme.resource;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Path("hash/{algorithm}")
public class MessageDigestResource {
    @PATCH
    public String patch(InputStream src, @PathParam("algorithm") String algorithm) {
        return digest(src, algorithm);
    }
    @POST
    public String post(InputStream src, @PathParam("algorithm") String algorithm) {
        return digest(src, algorithm);
    }
    @PUT
    public String put(InputStream src, @PathParam("algorithm") String algorithm) {
        return digest(src, algorithm);
    }

    private String digest(InputStream src, String algorithm) {
        try {
            final var digester = MessageDigest.getInstance(algorithm);
            int n;
            final var buffer = new byte[8192];
            while ((n = src.read(buffer)) > 0) {
                digester.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digester.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new NotSupportedException();
        } catch (IOException ioe) {
            throw new BadRequestException();
        }
    }
}
