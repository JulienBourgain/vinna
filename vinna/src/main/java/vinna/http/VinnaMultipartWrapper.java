package vinna.http;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vinna.exception.VuntimeException;
import vinna.util.MultivaluedHashMap;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class VinnaMultipartWrapper extends VinnaRequestWrapper implements MultipartRequest {
    private static Logger logger = LoggerFactory.getLogger(VinnaMultipartWrapper.class);

    private final HttpServletRequest request;
    private final MultivaluedHashMap<String, String> parameters;
    private final Map<String, UploadedFile> files;

    public VinnaMultipartWrapper(HttpServletRequest servletRequest, File temporaryDirectory, int maxSize) {
        super(servletRequest);

        this.request = servletRequest;
        this.parameters = new MultivaluedHashMap<>();
        this.files = new HashMap<>();

        // FIXME do this only if the route is resolved
        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setRepository(temporaryDirectory);
            //factory.setSizeThreshold(1234); // TODO
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(maxSize);

            final String encoding = request.getCharacterEncoding();
            List<FileItem> parts = upload.parseRequest(request);

            for (FileItem part : parts) {
                if (part.isFormField()) {
                    parameters.add(part.getFieldName(), encoding == null ? part.getString() : part.getString(encoding));
                } else {
                    logger.debug("Receive file {}", part.getFieldName());
                    files.put(part.getFieldName(), part);
                }
            }

        } catch (FileUploadException e) {
            logger.error("Error while parsing a multipart request", e);
            throw new VuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public Collection<String> getPartsName() {
        return Collections.unmodifiableCollection(files.keySet());
    }

    @Override
    public InputStream getParts(String name) {
        try {
            FileItem fileItem = files.get(name);
            if (fileItem != null) {
                return fileItem.getInputStream();
            } else {
                throw new VuntimeException("Cannot find file with the name " + name);
            }
        } catch (IOException e) {
            logger.error("unexpected exception while reading the multipart data", e);
            throw new VuntimeException(e);
        }
    }

    @Override
    public String getParameter(String name) {
        return this.parameters.getFirst(name);
    }

    @Override
    public Collection<String> getParameters(String name) {
        return Collections.unmodifiableCollection(this.parameters.get(name));
    }

    @Override
    public Map<String, Collection<String>> getParameters() {
        return Collections.<String, Collection<String>>unmodifiableMap(this.parameters);
    }
}
