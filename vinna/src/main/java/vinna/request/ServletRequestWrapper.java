package vinna.request;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ServletRequestWrapper implements Request {

    private final HttpServletRequest servletRequest;

    public ServletRequestWrapper(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    @Override
    public String getVerb() {
        return servletRequest.getMethod();
    }

    @Override
    public String getPath() {
        return servletRequest.getServletPath();
    }

    @Override
    public Collection<String> getParam(String name) {
        return Arrays.asList(servletRequest.getParameterValues(name));
    }

    @Override
    public String getHeader(String name) {
        return servletRequest.getHeader(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.list(servletRequest.getHeaders(name));
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        Map<String, Collection<String>> headers = new HashMap<>();

        Enumeration enumeration = servletRequest.getHeaderNames();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                String headerName = (String) enumeration.nextElement();
                headers.put(headerName, getHeaders(headerName));
            }
        }

        return Collections.unmodifiableMap(headers);
    }
}
