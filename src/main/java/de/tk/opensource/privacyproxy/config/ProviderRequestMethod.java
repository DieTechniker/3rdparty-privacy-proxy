package de.tk.opensource.privacyproxy.config;

import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @deprecated because {@linkplain
 * de.tk.opensource.privacyproxy.routing.RoutingHandler#handleGenericRequestInternal(String,
 * Map, HttpServletRequest, String, HttpMethod)} can handle both method types.
 */
@Deprecated
public enum ProviderRequestMethod {

    POST, GET
}
