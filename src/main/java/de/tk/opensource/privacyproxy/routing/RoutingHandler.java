/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

public abstract class RoutingHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoutingHandler.class);

	private static final String[] DEFAULT = new String[0];

	@Autowired
	private Proxy proxy;

	/**
	 * Basic implementation for requests, which are routed through the privacy-proxy. It can be
	 * configured by overriding certain methods. Every routing endpoint must have a dedicated
	 * specific handler.
	 */
	public ResponseEntity<Object> handlePostInternal(
		HttpServletRequest  request,
		Map<String, String> data
	) {
		HttpURLConnection connection = null;
		try {

			// filter unwanted query params
			String queryString = filterQueryString(data);
			URL url = new URL(getRoutingEndpoint() + (queryString != "" ? "?" + queryString : ""));
			connection = (HttpURLConnection) url.openConnection(proxy);
			connection.setRequestMethod("POST");

			// add required Cookies
			connection = addRequiredCookiesToRequest(connection, request);

			// add required headers
			// add provider specific response headers
			for (String requiredHeader : getRequiredRequestHeaders()) {
				String header = request.getHeader(requiredHeader);
				if (header != null) {
					connection.addRequestProperty(requiredHeader, header);
				}
			}

			// add additional params
			Map<String, String> additionalRequestProperties = getAdditionalRequestProperties();
			for (Map.Entry<String, String> entry : additionalRequestProperties.entrySet()) {
				connection.addRequestProperty(entry.getKey(), entry.getValue());
			}

			LOGGER.debug("routing request to {}", connection.getURL());

			// send it
			connection.connect();

			// add response headers
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");

			// add provider specific response headers
			for (String requiredHeader : getRequiredResponseHeaders()) {
				String header = connection.getHeaderField(requiredHeader);
				if (header != null) {
					responseHeaders.add(requiredHeader, header);
				}
			}

			ByteArrayResource content =
				new ByteArrayResource(IOUtils.toByteArray(connection.getInputStream()));

			// reporting
			trackRoutingRequest(
				getRoutingEndpoint(),
				queryString.getBytes().length,
				connection.getContentLengthLong()
			);

			return ResponseEntity.status(connection.getResponseCode()).headers(responseHeaders)
			.contentType(MediaType.valueOf(connection.getContentType())).body(content);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return ResponseEntity.notFound().build();
		} finally {
			IOUtils.close(connection);
		}
	}

	/**
	 * Excludes all the blacklisted query params of the request and returns a cleaned query string.
	 */
	private String filterQueryString(Map<String, String> params) {
		return createQueryString(filterBlacklistedData(params));
	}

	/**
	 * creates a String from map
	 */
	private String createQueryString(Map<String, String> params) {
		StringBuilder allowedQueryParams = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (allowedQueryParams.length() > 0) {
				allowedQueryParams.append("&");
			}
			allowedQueryParams.append(entry.getKey()).append("=").append(entry.getValue());
		}
		return allowedQueryParams.toString();
	}

	/**
	 * filter blacklisted entries from the map
	 */
	private Map<String, String> filterBlacklistedData(Map<String, String> data) {
		for (String blackListedParam : getBlacklistedQueryParams()) {
			data.remove(blackListedParam);
		}
		return data;
	}

	/**
	 * adds selected cookies to the request
	 *
	 * @param   connection
	 * @param   request
	 *
	 * @return
	 */
	private HttpURLConnection addRequiredCookiesToRequest(
		HttpURLConnection  connection,
		HttpServletRequest request
	) {
		if (getRequiredCookies().length > 0) {
			StringBuilder cookies = new StringBuilder();
			for (String requiredCookie : getRequiredCookies()) {
				switch (getCookieNameMatchType()) {

					case FULL :
						appendCookie(cookies, WebUtils.getCookie(request, requiredCookie));
						break;

					case PREFIX :
						for (
							Cookie cookieStartingWithPrefix
							: getCookiesByPrefix(request, requiredCookie)
						) {
							appendCookie(cookies, cookieStartingWithPrefix);
						}
						break;

					default :
						break;
				}
			}
			connection.addRequestProperty("Cookie", cookies.toString());
		}
		return connection;
	}

	/**
	 * Returns cookies with matching name prefix.
	 *
	 * @return  List of {@link Cookie}s or an empty list.
	 */
	private List<Cookie> getCookiesByPrefix(HttpServletRequest request, String prefix) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null && prefix != null) {
			List result = new ArrayList();
			for (Cookie cookie : cookies) {
				if (cookie.getName().startsWith(prefix)) {
					result.add(cookie);
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * appends a cookie to the StringBuilder
	 */
	private void appendCookie(StringBuilder builder, Cookie cookie) {
		if (cookie != null) {
			if (builder.length() > 0) {
				builder.append("; ");
			}
			builder.append(cookie.getName() + "=" + cookie.getValue());
			builder.append("; path=/");
			if (cookie.getMaxAge() != -1) {
				builder.append("; expires=" + cookie.getMaxAge());
			}
		}
	}

	/**
	 * simple implementation of routing request tracking
	 * @param routingEndpoint routing endpoint url
	 * @param requestSize size of request in bytes
	 * @param responseSize size of reponse in size
	 */
	protected void trackRoutingRequest(String routingEndpoint, long requestSize, long responseSize) {
		LOGGER.debug("Route request to 3rd party. Url={}, bytes sent={], bytes received={}", routingEndpoint, requestSize, responseSize);
	}

	/**
	 * request headers that should be transferred to the endpoint
	 */
	protected String[] getRequiredRequestHeaders() {
		return DEFAULT;
	}

	/**
	 * properties which should be added to the request and aren't added by default
	 */
	protected Map<String, String> getAdditionalRequestProperties() {
		return Collections.emptyMap();
	}

	/**
	 * cookies that will be copied from the client request to the endpoint request
	 */
	protected String[] getRequiredCookies() {
		return DEFAULT;
	}

	/**
	 * Define how the required cookies will be matched
	 */
	protected CookieNameMatchType getCookieNameMatchType() {
		return CookieNameMatchType.FULL;
	}

	/**
	 * routing endpoint
	 */
	protected abstract String getRoutingEndpoint();

	/**
	 * query params which should not be transferred to the endpoint
	 */
	protected String[] getBlacklistedQueryParams() {
		return DEFAULT;
	}

	/**
	 * response headers which should be transferred back to the client
	 */
	protected String[] getRequiredResponseHeaders() {
		return DEFAULT;
	}

}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
