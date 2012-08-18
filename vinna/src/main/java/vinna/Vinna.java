package vinna;

import vinna.exception.ConfigException;
import vinna.exception.VuntimeException;
import vinna.request.Request;
import vinna.route.*;

import java.io.*;
import java.util.*;

public class Vinna {
    private Map<String, Object> config;
    protected final String basePackage;
    private Router router;
    private ControllerFactory controllerFactory;
    private List<ActionArgument> routeParameters;
    protected final RequestBuilder req = new RequestBuilder();


    public Vinna() {
        this(Collections.<String, Object>emptyMap());
    }

    public Vinna(Map<String, Object> config) {
        this.config = new HashMap<>(config);
        this.basePackage = config.get("base-package") == null ? getClass().getPackage().getName() : (String) config.get("base-package");
        this.controllerFactory = controllerFactory(config);

        this.router = new Router();
        routes(config);
    }


    /**
     * Override to define the app routes
     *
     * @param config
     */
    protected void routes(Map<String, Object> config) {
        String[] routesPaths;
        if (config.get("routes") == null) {
            String path = this.basePackage.replace(".", "/") + "/routes";
            final Reader reader = getRoutesReader(path);
            if (reader == null) {
                path = "vinna/routes";
            } else {
                try {
                    reader.close();
                } catch (IOException e) {
                    //warning ("Cannot close routes file '" + path + "'", e);
                }
            }
            routesPaths = new String[]{path};
        } else {
            routesPaths = ((String) config.get("routes")).trim().split("\\s*,\\s*");
        }
        for (String routesPath : routesPaths) {
            Reader reader = getRoutesReader(routesPath);
            if (reader == null) {
                throw new VuntimeException("Cannot open routes file '" + routesPath + "'");
            }
            loadRoutes(reader);
        }

        /*try {
            final URL resource = getClass().getClassLoader().getResource(routeFile);
            if (resource != null) {
                if ("file".equals(resource.getProtocol())) {

                }
                System.out.println(resource);
                File file = new File(resource.toURI());
                System.out.println(file.exists());
            } else {

            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
*/
        //TODO: define default catchall routes
    }

    protected Reader getRoutesReader(String routesPath) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(routesPath);
        if (stream == null) {
            return null;
        } else {
            try {
                return new InputStreamReader(stream, "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new VuntimeException("Error opening routes file '" + routesPath + "'", e);
            }
        }
    }

    /**
     * override to provide a custom controller factory
     *
     * @param config
     * @return a ... you guessed right, a controller factory
     */
    protected ControllerFactory controllerFactory(Map<String, Object> config) {
        if (config.get("controller-factory") == null) {
            return new DefaultControllerFactory(this.basePackage);
        } else {
            try {
                Class<ControllerFactory> clz = (Class<ControllerFactory>) Thread.currentThread().getContextClassLoader().loadClass((String) config.get("controller-factory"));
                return clz.newInstance();
            } catch (ClassNotFoundException e) {
                throw new ConfigException("Cannot find the controller factory class", e);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ConfigException("Cannot instantiate the controller factory", e);
            }
        }
    }

    public final Object createController(String id, Class<?> clazz) {
        return controllerFactory.create(id, clazz);
    }

    public final RouteResolution match(Request request) {
        return router.match(request);
    }

    public final void addRoute(Route route) {
        this.router.addRoute(route);
    }

    protected void loadRoutes(Reader reader) {
        List<Route> routes = new RoutesParser(reader).load();
        router.addRoutes(routes);
    }

    protected final RouteBuilder get(String path) {
        routeParameters = new ArrayList<>();
        return new RouteBuilder("GET", path, this, routeParameters);
    }

    protected final RouteBuilder post(String path) {
        routeParameters = new ArrayList<>();
        return new RouteBuilder("POST", path, this, routeParameters);
    }

    protected final ActionArgument.Variable param(String name) {
        ActionArgument.Variable param = new ActionArgument.Variable(name);
        routeParameters.add(param);
        return param;
    }

    protected final <T> T constant(T value) {
        ActionArgument param = new ActionArgument.Const<>(value);
        routeParameters.add(param);
        return value;
    }

    protected final class RequestBuilder {

        private RequestBuilder() {
        }

        public final ActionArgument.Header header(String name) {
            ActionArgument.Header param = new ActionArgument.Header(name);
            routeParameters.add(param);
            return param;
        }

        public final Map<String, Collection<String>> headers() {
            ActionArgument.Headers headers = new ActionArgument.Headers();
            routeParameters.add(headers);
            return null;
        }

        public final ActionArgument.RequestParameter param(String name) {
            ActionArgument.RequestParameter param = new ActionArgument.RequestParameter(name);
            routeParameters.add(param);
            return param;
        }

        public final Map<String, Collection<String>> params() {
            ActionArgument.RequestParameters headers = new ActionArgument.RequestParameters();
            routeParameters.add(headers);
            return null;
        }

        public final InputStream body() {
            ActionArgument.RequestBody bodyActionArgument = new ActionArgument.RequestBody();
            routeParameters.add(bodyActionArgument);
            return null;
        }
    }
}
