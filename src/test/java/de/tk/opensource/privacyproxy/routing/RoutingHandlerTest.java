package de.tk.opensource.privacyproxy.routing;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RoutingHandlerTest {

    @Test
    public void testQueryString() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("one", "parameter1");
        parameter.put("two", "some, special chars?");

        RoutingHandler handler = new RoutingHandler() {
        };

        assertEquals(
                "one=parameter1&two=some%2C+special+chars%3F",
                handler.filterQueryString(parameter)
        );
    }

    @Test
    public void testQueryStringWithoutEncoding() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("param", "value, test");

        RoutingHandler handler = new RoutingHandler() {
            @Override
            protected String transformQueryParam(String name, String value) {
                return value;
            }
        };

        assertEquals(
                "param=value, test",
                handler.filterQueryString(parameter)
        );
    }

    @Test
    public void testQueryStringFiltered() {
        Map<String, String> parameter = new LinkedHashMap<>();
        parameter.put("restricted", "hidden value");
        parameter.put("param", "some value");

        RoutingHandler handler = new RoutingHandler() {
            @Override
            protected String[] getBlacklistedQueryParams() {
                return new String[]{"restricted"};
            }
        };

        assertEquals(
                "param=some+value",
                handler.filterQueryString(parameter)
        );
    }
}