package vinna.route;

import vinna.http.Request;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    // TODO multiple action per route
    public static final class Action {
        public final String controllerId;
        public final Class<?> controllerClass;
        public final String methodName;
        public final Method method;
        public final List<ActionArgument> methodParameters;

        public Action(String controllerId, String methodName, List<ActionArgument> methodParameters) {
            this.controllerId = controllerId;
            this.controllerClass = null;
            this.methodName = methodName;
            this.method = null;
            this.methodParameters = methodParameters;
        }

        public Action(String controllerId, Class<?> controllerClass, Method method, List<ActionArgument> methodParameters) {
            this.controllerId = controllerId;
            this.controllerClass = controllerClass;
            this.methodName = null;
            this.method = method;
            this.methodParameters = methodParameters;
            // TODO check that methodParameters.size() == method.getParameterTypes().length
        }

        @Override
        public String toString() {
            return (controllerClass == null ? controllerId : controllerClass.getName()) + "." + (method == null ? methodName : method.getName());
        }
    }

    private final String verb;
    private final Pattern pathPattern;
    private final Collection<String> pathVariableNames;

    private final Map<String, Pattern> mandatoryQueryParameters;
    private final Map<String, Pattern> mandatoryRequestHeaders;

    private final Action action;

    public Route(String verb, Pattern pathPattern, Collection<String> pathVariableNames, Map<String, Pattern> mandatoryQueryParameters,
                 Map<String, Pattern> mandatoryRequestHeaders, Action action) {
        this.verb = verb;
        this.pathPattern = pathPattern;
        this.mandatoryQueryParameters = mandatoryQueryParameters;
        this.pathVariableNames = pathVariableNames;
        this.action = action;
        this.mandatoryRequestHeaders = mandatoryRequestHeaders;
    }

    public RouteResolution match(Request request) {
        if (request.getMethod().equalsIgnoreCase(verb) || request.getMethod().equalsIgnoreCase("HEAD") && verb.equals("GET")) {

            Matcher m = pathPattern.matcher(request.getPath());
            if (m.matches()) {
                Map<String, String> paramValues = new HashMap<>();

                for (Map.Entry<String, Pattern> paramEntry : mandatoryQueryParameters.entrySet()) {
                    Collection<String> params = request.getParameters(paramEntry.getKey());
                    if (!matchMandatoryCollectionWithPattern(params, paramEntry.getValue())) {
                        return null;
                    }
                }

                for (Map.Entry<String, Pattern> headerEntry : mandatoryRequestHeaders.entrySet()) {
                    Collection<String> headers = request.getHeaderValues(headerEntry.getKey());
                    if (!matchMandatoryCollectionWithPattern(headers, headerEntry.getValue())) {
                        return null;
                    }
                }

                for (String variableName : pathVariableNames) {
                    if (m.group(variableName) != null) {
                        paramValues.put(variableName, m.group(variableName));
                    } else {
                        return null;
                    }
                }

                return new RouteResolution(action, paramValues, request);
            }
        }
        return null;
    }

    public String getVerb() {
        return this.verb;
    }

    private boolean matchMandatoryCollectionWithPattern(Collection<String> collection, Pattern pattern) {
        if (collection.isEmpty()) {
            return false;
        }
        if (pattern != null) {
            for (String element : collection) {
                if (!pattern.matcher(element).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Route{" + verb + " " + pathPattern + " " + action + " }";
    }
}
