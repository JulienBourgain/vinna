package vinna.response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public interface Response {
    // TODO define methods

    OutputStream getOutputStream() throws IOException;

    void setStatus(int status);

    int getStatus();

    PrintWriter getWriter() throws IOException;

    void setContentType(String contentType);

    String getContentType();

    HttpServletResponse getHttpServletResponse();
}
