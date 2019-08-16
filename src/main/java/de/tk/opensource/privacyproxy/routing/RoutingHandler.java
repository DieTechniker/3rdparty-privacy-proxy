/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.WebUtils;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import de.tk.opensource.privacyproxy.config.ProviderRequestMethod;
import de.tk.opensource.privacyproxy.util.RequestUtils;
import de.tk.opensource.privacyproxy.util.TkProxyNoProxyRoutePlanner;

/**
 * This is fun code. It will allow you to take back control over information being sent to 3rd Party
 * Providers. So most of those 3PP will not like it ;-) The idea is to allow Whitelisting and
 * Blacklisting. The request to the 3PP Server will come from this service / server. No Header and
 * cookie information will be available by default and has to be whitelisted. If you know about
 * certain parameters you need to forbid or filtered, use blacklisting. If the service delivers a
 * response, this also has to be kind of whitelisted. Cookies will be set by this service and thus
 * will always be 1st party! You have to implement your own RoutingHandler per provider. E.g. you
 * could write a RoutingProvider to proxy traffic to an external Matomo instance. REQUIREMENT: You
 * have to be able to configure a 3PP JS to talk to this service URL instead of their server
 * directly. If they don't allow this without patching their code by yourself, look for another
 * service provider. Therer is no technical requirement for not allowing this.
 */
public abstract class RoutingHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoutingHandler.class);

	private static final String[] DEFAULT = new String[0];

	@Autowired
	private TkProxyNoProxyRoutePlanner tkProxyNoProxyRoutePlanner;

	/**
	 * Basic implementation for requests, which are routed through the privacy-proxy. It can be
	 * configured by overriding certain methods. Every routing endpoint must have a dedicated
	 * specific handler.
	 */
	public ResponseEntity<Object> handlePostInternal(
		HttpServletRequest  request,
		Map<String, String> data,
		String				trackingEndpoint
	) {
		final CloseableHttpClient httpClient =
			HttpClients.custom().setRoutePlanner(tkProxyNoProxyRoutePlanner.getRoutePlanner())
			.build();
		HttpRequestBase httpRequest;
		try {

			// filter unwanted query params
			final String queryString = filterQueryString(data);
			final URI url =
				new URI(trackingEndpoint + (!"".equals(queryString) ? "?" + queryString : ""));

			if (getRequestMethod() == ProviderRequestMethod.POST) {
				httpRequest = new HttpPost(url);
			} else {
				httpRequest = new HttpGet(url);
			}

			// add required Cookies
			addRequiredCookiesToRequest(httpRequest, request);

			// add required headers
			// add provider specific response headers
			for (final String requiredHeader : getRequiredRequestHeaders()) {
				final String header = request.getHeader(requiredHeader);
				if (header != null) {
					httpRequest.addHeader(requiredHeader, header);
				}
			}

			// add additional params
			for (
				final Map.Entry<String, String> entry
				: getAdditionalRequestProperties(request).entrySet()
			) {
				httpRequest.addHeader(entry.getKey(), entry.getValue());
			}

			LOGGER.debug("Calling {}", httpRequest.getURI());

			// send it
			final HttpResponse response = httpClient.execute(httpRequest);

			LOGGER.debug("Response: Code {} | ", response.getStatusLine().getStatusCode());

			// add response headers
			final HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");

			// add provider specific response headers
			for (final String requiredHeader : getRequiredResponseHeaders()) {
				final String header = response.getFirstHeader(requiredHeader).getValue();
				if (header != null) {
					responseHeaders.add(requiredHeader, header);
				}
			}

			// Get the response Body
			final InputStream responseBody = response.getEntity().getContent();

			// reporting
			trackRoutingRequest(
				trackingEndpoint,
				queryString.getBytes().length,
				response.getEntity().getContentLength()
			);

			final String contentType = response.getEntity().getContentType().getValue();
			final MediaType mediaType = contentType != null ? MediaType.valueOf(contentType) : null;
			final int statusCode = response.getStatusLine().getStatusCode();

			LOGGER.debug(
				"Response to caller: Content Type {} | Status Code {}",
				contentType,
				statusCode
			);
			LOGGER.debug("Body:\n{}", responseBody);

			return ResponseEntity.status(statusCode).headers(responseHeaders).contentType(mediaType)
			.body(IOUtils.toByteArray(responseBody));

		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
			return ResponseEntity.status(504).build();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				LOGGER.warn(e.getMessage(), e);
			}
		}
	}

	/**
	 * Excludes all the blacklisted query params of the request and returns a cleaned query string.
	 */
	private String filterQueryString(final Map<String, String> params) {
		return createQueryString(filterBlacklistedData(params));
	}

	/**
	 * creates a String from map
	 */
	private String createQueryString(final Map<String, String> params) {
		final StringBuilder allowedQueryParams = new StringBuilder();
		for (final Map.Entry<String, String> entry : params.entrySet()) {
			if (allowedQueryParams.length() > 0) {
				allowedQueryParams.append("&");
			}
			allowedQueryParams.append(entry.getKey()).append("=").append(
				RequestUtils.urlencode(entry.getValue())
			);
		}
		return allowedQueryParams.toString();
	}

	/**
	 * filter blacklisted entries from the map
	 */
	private Map<String, String> filterBlacklistedData(Map<String, String> data) {
		for (final String blackListedParam : getBlacklistedQueryParams()) {
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
	private HttpRequestBase addRequiredCookiesToRequest(
		HttpRequestBase    connection,
		HttpServletRequest request
	) {
		if (getRequiredCookies().length > 0) {
			final StringBuilder cookies = new StringBuilder();
			for (final String requiredCookie : getRequiredCookies()) {
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
			connection.addHeader("Cookie", cookies.toString());
		}
		return connection;
	}

	/**
	 * Returns cookies with matching name prefix.
	 *
	 * @return  List of {@link Cookie}s or an empty list.
	 */
	private List<Cookie> getCookiesByPrefix(final HttpServletRequest request, final String prefix) {
		final Cookie[] cookies = request.getCookies();
		if (cookies != null && prefix != null) {
			final List<Cookie> result = new ArrayList<>();
			for (final Cookie cookie : cookies) {
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
	private void appendCookie(final StringBuilder builder, final Cookie cookie) {
		if (cookie != null) {
			if (builder.length() > 0) {
				builder.append("; ");
			}
			builder.append(cookie.getName()).append("=").append(cookie.getValue());
			builder.append("; path=/");
			if (cookie.getMaxAge() != -1) {
				builder.append("; expires=").append(cookie.getMaxAge());
			}
		}
	}

	/**
	 * simple implementation of routing request tracking
	 *
	 * @param  routingEndpoint  routing endpoint url
	 * @param  requestSize      size of request in bytes
	 * @param  responseSize     size of reponse in size
	 */
	protected void trackRoutingRequest(
		String routingEndpoint,
		long   requestSize,
		long   responseSize
	) {
		LOGGER.debug(
			"Route request to 3rd party. Url={}, bytes sent={}, bytes received={}",
			routingEndpoint,
			requestSize,
			responseSize
		);
	}

	/**
	 * request headers that should be transferred to the endpoint
	 */
	protected String[] getRequiredRequestHeaders() {
		return DEFAULT;
	}

	protected ProviderRequestMethod getRequestMethod() {
		return ProviderRequestMethod.POST;
	}

	/**
	 * properties which should be added to the request and aren't added by default
	 */
	protected Map<String, String> getAdditionalRequestProperties(HttpServletRequest request) {
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
