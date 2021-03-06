package vinna.route;

import vinna.exception.VuntimeException;
import vinna.http.Cookie;
import vinna.http.MultipartRequest;
import vinna.http.UploadedFile;
import vinna.util.Conversions;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface ActionArgument {

    public static class Const<T> implements ActionArgument {
        private final T value;

        public Const(T value) {
            this.value = value;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            return value;
        }

        @Override
        public boolean compatibleWith(Class<?> type) {
            //TODO: handle primtives/object duos
            return value == null || type.isAssignableFrom(value.getClass());
        }

        @Override
        public String toString() {
            return "const['" + value + "']";
        }
    }

    public static class Variable extends ChameleonArgument {
        private final String name;

        public Variable(String name) {
            this.name = name;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            String value = env.matchedVars.get(name);
            if (targetType.isAssignableFrom(Collection.class)) {
                //TODO: simply do not expose asCollection for path variables ?
                if (typeArg != null) {
                    Object convertedValue = Conversions.convertString(value, typeArg);
                    return Collections.unmodifiableCollection(Arrays.asList(convertedValue));
                } else {
                    throw new VuntimeException("need an argType when the target is a collection");
                }
            }
            return Conversions.convertString(value, targetType);
        }

        @Override
        public String toString() {
            return "variable['" + name + "']";
        }
    }

    public static class RequestParameter extends ChameleonArgument {

        private final String name;

        public RequestParameter(String name) {
            this.name = name;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            if (targetType.isAssignableFrom(Collection.class)) {
                if (typeArg != null) {
                    return Conversions.convertCollection(env.request.getParameters(name), typeArg);
                } else {
                    throw new VuntimeException("need an argType when the target is a collection");
                }
            }
            return Conversions.convertString(env.request.getParameter(name), targetType);
        }

        @Override
        public String toString() {
            return "req.param['" + name + "']";
        }
    }

    public static class RequestPart implements ActionArgument {
        private final String name;

        public RequestPart(String name) {
            this.name = name;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            if (env.request instanceof MultipartRequest) {
                final MultipartRequest request = (MultipartRequest) env.request;
                return request.getPart(name);
            } else {
                throw new VuntimeException("Trying to get a file from a non multipart request");
            }
        }

        @Override
        public boolean compatibleWith(Class<?> type) {
            return type.isAssignableFrom(UploadedFile.class);
        }
    }

    public static class RequestBody implements ActionArgument {

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            try {
                return env.request.getInputStream();
            } catch (IOException e) {
                throw new VuntimeException("unexpected exception while reading the request", e);
            }
        }

        @Override
        public boolean compatibleWith(Class<?> type) {
            return type.isAssignableFrom(InputStream.class);
        }

        @Override
        public String toString() {
            return "req.body";
        }
    }

    public static class Header extends ChameleonArgument {

        private final String headerName;

        public Header(String headerName) {
            this.headerName = headerName;
        }

        public Header(String headerName, Class<?> collectionType) {
            type = Collection.class;
            typeArg = collectionType;
            this.headerName = headerName;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            if (targetType.isAssignableFrom(Collection.class)) {
                if (typeArg != null) {
                    return Conversions.convertCollection(env.request.getHeaderValues(headerName), typeArg);
                } else {
                    throw new VuntimeException("need an argType when the target is a collection");
                }
            }
            return Conversions.convertString(env.request.getHeader(headerName), targetType);
        }

        @Override
        public String toString() {
            return "req.header['" + headerName + "']";
        }
    }

    public static class Headers implements ActionArgument {

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            return env.request.getHeaders();
        }

        @Override
        public boolean compatibleWith(Class<?> type) {
            return type.isAssignableFrom(Map.class);
        }

        @Override
        public String toString() {
            return "req.headers";
        }
    }

    public static class RequestParameters implements ActionArgument {

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            return env.request.getParameters();
        }

        @Override
        public boolean compatibleWith(Class<?> type) {
            return type.isAssignableFrom(Map.class);
        }

        @Override
        public String toString() {
            return "req.params";
        }
    }

    public static class CookieArgument extends ChameleonArgument {

        private final String cookieName;

        public CookieArgument(String cookieName) {
            this.cookieName = cookieName;
        }

        @Override
        public Object resolve(RouteResolution.Action.Environment env, Class<?> targetType) {
            final Cookie cookie = env.request.getCookiesMap().get(cookieName);
            if (targetType.isAssignableFrom(Cookie.class)) {
                return cookie;
            } else if (cookie == null) {
                return null;//FIXME: beware the primitive types
            } else {
                return Conversions.convertString(cookie.getValue(), targetType);
            }
        }

        public Cookie asCookie() {
            this.type = Cookie.class;
            return null;
        }

        @Override
        public String toString() {
            return "req.cookie['" + cookieName + "']";
        }
    }

    public static abstract class ChameleonArgument implements ActionArgument {
        protected Class<?> type;
        protected Class<?> typeArg;

        public final long asLong() {
            return 42;
        }

        public final int asInt() {
            return 42;
        }

        public final short asShort() {
            return 42;
        }

        public final byte asByte() {
            return 42;
        }

        public final float asFloat() {
            return 42.0f;
        }

        public final double asDouble() {
            return 42.0;
        }

        public final BigDecimal asBigDecimal() {
            return BigDecimal.TEN;
        }

        public final BigInteger asBigInteger() {
            return BigInteger.TEN;
        }

        public final String asString() {
            return "42";
        }

        public final boolean asBoolean() {
            return false;
        }

        public final <T> Collection<T> asCollection(Class<T> clazz) {
            this.type = Collection.class;
            this.typeArg = clazz;
            return null;
        }

        @Override
        public boolean compatibleWith(Class<?> argType) {
            return (type == null || argType.isAssignableFrom(type));
        }
    }

    Object resolve(RouteResolution.Action.Environment env, Class<?> targetType);

    boolean compatibleWith(Class<?> type);
}
