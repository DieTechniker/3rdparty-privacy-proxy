/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;

/**
 * @deprecated  because {@linkplain
 *              de.tk.opensource.privacyproxy.routing.RoutingHandler#handleGenericRequestInternal(String,
 *              Map, HttpServletRequest, String, HttpMethod)} can handle both method types.
 */
@Deprecated
public enum ProviderRequestMethod {

	POST, GET
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
