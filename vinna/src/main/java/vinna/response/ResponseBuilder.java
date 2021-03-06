package vinna.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vinna.exception.PassException;
import vinna.http.Cookie;
import vinna.http.VinnaRequestWrapper;
import vinna.http.VinnaResponseWrapper;
import vinna.util.MultivaluedHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseBuilder implements Response {
    private static final Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);
    private static final Response PASS_RESPONSE = new DoPass();

    private int status;
    private MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
    private Map<String, Cookie> cookies = new HashMap<>();
    private String location;
    private InputStream body;
    private String encoding;
    private boolean isRedirect = false;

    public static ResponseBuilder withStatus(int status) {
        return new ResponseBuilder(status);
    }

    public static Response pass() {
        return PASS_RESPONSE;
    }

    public ResponseBuilder(int status) {
        status(status);
    }

    public final ResponseBuilder status(int status) {
        this.status = status;
        return this;
    }

    public final ResponseBuilder redirect(String location) {
        this.location = location;
        this.isRedirect = true;
        return this;
    }

    public final ResponseBuilder type(String type) {
        setHeader("Content-Type", type);
        return this;
    }

    public final ResponseBuilder encoding(String encoding) {
        this.encoding = encoding;
        setHeader("Content-Encoding", encoding);
        return this;
    }

    public final ResponseBuilder language(String language) {
        setHeader("Content-Language", language);
        return this;
    }

    public final ResponseBuilder variant(String variant) {
        setHeader("Vary", variant);
        return this;
    }

    public final ResponseBuilder location(String location) {
        setHeader("Location", location);
        return this;
    }

    public final ResponseBuilder etag(String etag) {
        setHeader("ETag", etag);
        return this;
    }

    public final ResponseBuilder lastModified(Date lastModified) {
        setHeader("Last-Modified", lastModified);
        return this;
    }

    public final ResponseBuilder cacheControl(String cacheControl) {
        setHeader("Cache-Control", cacheControl);
        return this;
    }

    public final ResponseBuilder expires(Date expires) {
        setHeader("Expires", expires);
        return this;
    }

    public final ResponseBuilder addHeader(String name, Object value) {
        headers.add(name, value);
        return this;
    }

    public final ResponseBuilder setHeader(String name, Object value) {
        headers.putSingle(name, value);
        return this;
    }

    public final ResponseBuilder cookie(Cookie cookie) {
        cookies.put(cookie.getName(), cookie);
        return this;
    }

    public final ResponseBuilder body(InputStream body) {
        this.body = body;
        return this;
    }

    protected void writeBody(ServletOutputStream out) throws IOException {
        logger.debug("Start sending response body");
        if (body != null) {
            int size = 512;//FIXME: make this configurable ?
            byte[] buffer = new byte[size];
            int len;
            while ((len = body.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
            try {
                body.close();
            } catch (IOException e) {
                logger.warn("Cannot close the response body input stream", e);
            }
        }
    }

    public final int getStatus() {
        return status;
    }

    public final Object getFirstHeader(String header) {
        return headers.getFirst(header);
    }

    public final List<Object> getHeaders(String header) {
        return headers.get(header);
    }

    public final String getEncoding() {
        return encoding;
    }

    @Override
    public final void execute(VinnaRequestWrapper request, VinnaResponseWrapper response) throws IOException, ServletException {
        response.setStatus(status);

        for (Map.Entry<String, List<Object>> header : headers.entrySet()) {

            if (header.getValue().size() == 1) {
                response.setHeader(header.getKey(), header.getValue().get(0).toString());
            } else {
                for (Object value : header.getValue()) {
                    response.addHeader(header.getKey(), value.toString());
                }
            }

        }

        for (Cookie cookie : cookies.values()) {
            final javax.servlet.http.Cookie servletCookie = new javax.servlet.http.Cookie(cookie.getName(), cookie.getValue());
            if (cookie.getComment() != null) {
                servletCookie.setComment(cookie.getComment());
            }
            if (cookie.getDomain() != null) {
                servletCookie.setDomain(cookie.getDomain());
            }
            servletCookie.setMaxAge(cookie.getMaxAge());
            if (cookie.getPath() != null) {
                servletCookie.setPath(cookie.getPath());
            }
            servletCookie.setSecure(cookie.isSecure());
            servletCookie.setVersion(cookie.getVersion());
            response.addCookie(servletCookie);
        }

        // FIXME: investigate how to properly handle redirect
        if (isRedirect) {
            if (this.location != null) {
                String locationUrl = response.encodeRedirectURL(location);
                if (!hasScheme(locationUrl)) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(request.getScheme()).append("://").append(request.getServerName()).append(":").append(request.getServerPort());
                    if (!locationUrl.startsWith("/")) {
                        buffer.append(request.getContextPath()).append("/");
                    }
                    buffer.append(locationUrl);
                    locationUrl = buffer.toString();
                }

                response.setHeader("Location", locationUrl);
            }
            return;
        }

        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }

        writeBody(response.getOutputStream());
        response.getOutputStream().flush();
    }

    private boolean hasScheme(String uri) {
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == ':')
                return true;
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || (i > 0 && (c >= '0' && c <= '9' || c == '.' || c == '+' || c == '-'))))
                break;
        }
        return false;
    }

    private static class DoPass implements Response {
        @Override
        public void execute(VinnaRequestWrapper request, VinnaResponseWrapper response) throws IOException, ServletException {
            throw new PassException();
        }
    }
}
