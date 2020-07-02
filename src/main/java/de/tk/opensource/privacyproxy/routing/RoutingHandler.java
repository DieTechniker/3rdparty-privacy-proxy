/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.WebUtils;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import de.tk.opensource.privacyproxy.config.ProviderRequestMethod;
import de.tk.opensource.privacyproxy.config.UrlPattern;
import de.tk.opensource.privacyproxy.util.ProxyRoutePlanner;
import de.tk.opensource.privacyproxy.util.RequestUtils;

/**
 * This component will allow you to take back control over information being sent to 3rd Party
 * Providers. The idea is to allow Whitelisting and Blacklisting. The request to the 3PP Server will
 * come from this service / server. No header and cookie information will be available by default
 * and has to be whitelisted. If you know about certain parameters you need to forbid or filtered,
 * use blacklisting. If the service delivers a response, this also has to be kind of whitelisted.
 * Cookies will be set by this service and thus will always be 1st party! You have to implement your
 * own RoutingHandler per provider. E.g. you could write a RoutingProvider to proxy traffic to an
 * external Matomo instance.
 * REQUIREMENT: You have to be able to configure the 3rd Party JS to talk to this service URL
 * instead of their server directly. If they don't allow this without patching their code by
 * yourself, look for another service provider. There is no technical requirement for not allowing
 * this.
 */
@Controller
@RequestMapping(value = UrlPattern.Contexts.PROXY)
public abstract class RoutingHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoutingHandler.class);

	private static final String[] DEFAULT_RETURN_VALUE = new String[0];

	private static final int ROUTING_TIMEOUT_IN_SECS = 5;

	@Autowired
	private ProxyRoutePlanner proxyRoutePlanner;

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
		final RequestConfig requestConfig =
			RequestConfig.custom().setConnectTimeout(ROUTING_TIMEOUT_IN_SECS * 1000)
			.setConnectionRequestTimeout(ROUTING_TIMEOUT_IN_SECS * 1000).setSocketTimeout(
				ROUTING_TIMEOUT_IN_SECS * 1000
			)
			.build();
		final CloseableHttpClient httpClient =
			HttpClients.custom().setDefaultRequestConfig(requestConfig).setRoutePlanner(
				proxyRoutePlanner.getRoutePlanner()
			)
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
			addWhitelistedCookiesToRequest(httpRequest, request);

			// add required headers
			// add provider specific response headers
			for (final String headerName : getWhitelistedRequestHeaders()) {
				final String headerValue = request.getHeader(headerName);
				if (headerValue != null) {
					httpRequest.addHeader(headerName, headerValue);
				}
			}

			// add additional headers
			for (
				final Map.Entry<String, String> header
				: getAdditionalRequestHeaders(request).entrySet()
			) {
				httpRequest.addHeader(header.getKey(), header.getValue());
			}

			LOGGER.debug("Calling {}", httpRequest.getURI());

			// send it
			final HttpResponse response = httpClient.execute(httpRequest);

			// add response headers
			final HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");

			// add provider specific response headers
			for (final String headerName : getWhitelistedResponseHeaders()) {
				final String headerValue = response.getFirstHeader(headerName).getValue();
				if (headerValue != null) {
					responseHeaders.add(headerName, headerValue);
				}
			}

			// reporting
			trackRoutingRequest(
				trackingEndpoint,
				queryString.getBytes().length,
				response.getEntity() != null ? response.getEntity().getContentLength() : 0
			);

			// media type
			MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
			if (response.getEntity() != null) {
				Header contentType = response.getEntity().getContentType();
				if (contentType != null) {
					mediaType = MediaType.valueOf(contentType.getValue());
				}
			}

			final int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.debug(
				"Response to caller: Content Type {} | Status Code {}",
				mediaType.toString(),
				statusCode
			);

			// Get the response Body
			byte[] responseBody = null;
			if (response.getEntity() != null) {
				responseBody = IOUtils.toByteArray(response.getEntity().getContent());
			}
			LOGGER.debug("Body:\n{}", Arrays.toString(responseBody));

			return ResponseEntity.status(statusCode).headers(responseHeaders).contentType(mediaType)
			.body(responseBody);

		} catch (Exception e) {
			LOGGER.warn(
				"Failed to proxy request. Endpoint: " + trackingEndpoint + ", Error: "
				+ e.getMessage(),
				e
			);
			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
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
	String filterQueryString(final Map<String, String> params) {
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
				transformQueryParam(entry.getKey(), entry.getValue())
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
	private HttpRequestBase addWhitelistedCookiesToRequest(
		HttpRequestBase    connection,
		HttpServletRequest request
	) {
		if (getWhitelistedCookieNames().length > 0) {
			final StringBuilder cookies = new StringBuilder();
			for (final String cookieName : getWhitelistedCookieNames()) {
				switch (getCookieNameMatchType()) {

					case FULL :
						appendCookie(cookies, WebUtils.getCookie(request, cookieName));
						break;

					case PREFIX :
						for (
							Cookie cookieStartingWithPrefix
							: getCookiesByPrefix(request, cookieName)
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
	protected String[] getWhitelistedRequestHeaders() {
		return DEFAULT_RETURN_VALUE;
	}

	protected ProviderRequestMethod getRequestMethod() {
		return ProviderRequestMethod.POST;
	}

	/**
	 * properties which should be added to the request and aren't added by default
	 */
	protected Map<String, String> getAdditionalRequestHeaders(HttpServletRequest request) {
		return Collections.emptyMap();
	}

	/**
	 * cookies that will be copied from the client request to the endpoint request
	 */
	protected String[] getWhitelistedCookieNames() {
		return DEFAULT_RETURN_VALUE;
	}

	/**
	 * Define how the required cookies will be matched
	 */
	protected CookieNameMatchType getCookieNameMatchType() {
		return CookieNameMatchType.FULL;
	}

	/**
	 * Transform the given query parameter before appending it to the request. The default
	 * implementation applies percent-encoding to the value.
	 *
	 * @param name  query parameter name
	 * @param value query parameter value
	 * @return encoded parameter
	 */
	protected String transformQueryParam(String name, String value) {
		return RequestUtils.urlencode(value);
	}

	/**
	 * query params which should not be transferred to the endpoint
	 */
	protected String[] getBlacklistedQueryParams() {
		return DEFAULT_RETURN_VALUE;
	}

	/**
	 * response headers which should be transferred back to the client
	 */
	protected String[] getWhitelistedResponseHeaders() {
		return DEFAULT_RETURN_VALUE;
	}

}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
