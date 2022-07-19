/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RoutingHandlerTest {

	@Test
	public void testQueryString() {
		Map<String, String> parameter = new LinkedHashMap<>();
		parameter.put("one", "parameter1");
		parameter.put("two", "some, special chars?");

		RoutingHandler handler =
			new RoutingHandler(null, null, null) {
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

		RoutingHandler handler =
			new RoutingHandler(null, null, null) {
				@Override
				protected String transformQueryParam(String name, String value) {
					return value;
				}
			};

		assertEquals("param=value, test", handler.filterQueryString(parameter));
	}

	@Test
	public void testQueryStringFiltered() {
		Map<String, String> parameter = new LinkedHashMap<>();
		parameter.put("restricted", "hidden value");
		parameter.put("param", "some value");

		RoutingHandler handler =
			new RoutingHandler(null, null, null) {
				@Override
				protected String[] getBlacklistedQueryParams() {
					return new String[] { "restricted" };
				}
			};

		assertEquals("param=some+value", handler.filterQueryString(parameter));
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
