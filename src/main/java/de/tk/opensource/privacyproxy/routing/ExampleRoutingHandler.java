/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import de.tk.opensource.privacyproxy.config.UrlPattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * This is an example implementation of the RoutingHandler.
 * Please implement your own RoutingHandlers per provider and endpoint.
 */
@Controller
@RequestMapping(value = UrlPattern.Contexts.PROXY)
public class ExampleRoutingHandler extends RoutingHandler {

	@Value("${example.routing.endpoint}")
	private String routingEndpoint;

	@Value("${example.environment}")
	private String environment;

	@PostMapping(value = UrlPattern.Provider.EXAMPLE)
	@ResponseBody
	public ResponseEntity handleRequest(
		@RequestBody
		MultiValueMap<String, String>			   formData,
		HttpServletRequest						   request
	) {
		return handlePostInternal(request, formData.toSingleValueMap());
	}

	@Override
	protected String getRoutingEndpoint() {
		return routingEndpoint;
	}

	@Override
	protected String[] getRequiredRequestHeaders() {
		return new String[] { "User-Agent" };
	}

	@Override
	protected Map<String, String> getAdditionalRequestProperties() {
		return Collections.singletonMap("env", environment);
	}

	@Override
	protected String[] getRequiredCookies() {
		return new String[] { "_example_pk_" };
	}

	@Override
	protected CookieNameMatchType getCookieNameMatchType() {
		return CookieNameMatchType.PREFIX;
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
