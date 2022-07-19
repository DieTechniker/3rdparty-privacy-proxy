/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.routing;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import de.tk.opensource.privacyproxy.config.ProviderRequestMethod;
import de.tk.opensource.privacyproxy.config.UrlPattern;
import de.tk.opensource.privacyproxy.util.ProxyHelper;
import de.tk.opensource.privacyproxy.util.ProxyRoutePlanner;
import de.tk.opensource.privacyproxy.util.RequestUtils;
import de.tk.opensource.privacyproxy.util.RestTemplateProxyCustomizer;

/**
 * This component will allow you to take back control over information being sent to 3rd Party
 * Providers. The idea is to allow Whitelisting and Blacklisting. The request to the 3PP Server will
 * come from this service / server. No header and cookie information will be available by default
 * and has to be whitelisted. If you know about certain parameters you need to forbid or filtered,
 * use blacklisting. If the service delivers a response, this also has to be kind of whitelisted.
 * Cookies will be set by this service and thus will always be 1st party! You have to implement your
 * own RoutingHandler per provider. E.g. you could write a RoutingProvider to proxy traffic to an
 * external Matomo instance. REQUIREMENT: You have to be able to configure the 3rd Party JS to talk
 * to this service URL instead of their server directly. If they don't allow this without patching
 * their code by yourself, look for another service provider. There is no technical requirement for
 * not allowing this.
 */
@Controller
@RequestMapping(value = UrlPattern.Contexts.PROXY)
public abstract class RoutingHandler {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String[] DEFAULT_RETURN_VALUE = new String[0];

	private final RestTemplateProxyCustomizer restTemplateProxyCustomizer;

	private final ProxyRoutePlanner proxyRoutePlanner;

	private final ProxyHelper proxyHelper;

	RoutingHandler(
		RestTemplateProxyCustomizer restTemplateProxyCustomizer,
		ProxyRoutePlanner			proxyRoutePlanner,
		ProxyHelper					proxyHelper
	) {
		this.restTemplateProxyCustomizer = restTemplateProxyCustomizer;
		this.proxyRoutePlanner = proxyRoutePlanner;
		this.proxyHelper = proxyHelper;
	}

	/**
	 * Basic implementation for requests, which are routed through the privacy-proxy. It can be
	 * configured by overriding certain methods. Every routing endpoint must have a dedicated
	 * specific handler.
	 */
	public ResponseEntity<Resource> handleGenericRequestInternal(
		final String			  targetEndpoint,
		final Map<String, String> queryStrings,
		final HttpServletRequest  request,
		@Nullable
		final String			  body,
		final HttpMethod		  method
	) {
		final String queryString = filterQueryString(queryStrings);
		final URI uri =
			UriComponentsBuilder.fromUriString(targetEndpoint).query(queryString).build(true)
			.toUri();

		final HttpHeaders headers = new HttpHeaders();
		addHeaders(request, headers);
		addWhitelistedCookies(request, headers);

		final HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
		try {
			final RestTemplate restTemplate =
				new RestTemplateBuilder(restTemplateProxyCustomizer).build();
			logger.debug("Calling {}", uri);
			final ResponseEntity<Resource> responseEntity =
				restTemplate.exchange(uri, method, httpEntity, Resource.class);

			final HttpHeaders responseHeaders = responseEntity.getHeaders();
			final ResponseEntity<Resource> customResponseEntity =
				ResponseEntity.status(responseEntity.getStatusCode()).headers(
					getResponseHeaders(responseHeaders)
				)
				.body(responseEntity.getBody());

			log(targetEndpoint, queryString.getBytes().length, customResponseEntity);

			return customResponseEntity;
		} catch (Exception e) {
			logger.warn(
				"Failed to proxy request. Endpoint: " + targetEndpoint + ", Error: "
				+ e.getMessage(),
				e
			);
			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
		}
	}

	/**
	 * @deprecated  Use {@linkplain #handleGenericRequestInternal(String, Map, HttpServletRequest,
	 *              String, HttpMethod)} instead. Basic implementation for requests, which are
	 *              routed through the privacy-proxy. It can be configured by overriding certain
	 *              methods. Every routing endpoint must have a dedicated specific handler.
	 */
	@Deprecated
	public ResponseEntity<Object> handlePostInternal(
		HttpServletRequest  request,
		Map<String, String> data,
		String				trackingEndpoint
	) {
		final CloseableHttpClient httpClient =
			proxyHelper.getCloseableHttpClient(proxyRoutePlanner);
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
			addLegacyWhitelistedCookiesToRequest(httpRequest, request);

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

			logger.debug("Calling {}", httpRequest.getURI());

			// send it
			final HttpResponse response = httpClient.execute(httpRequest);

			// add response headers
			final HttpHeaders responseHeaders =
				getResponseHeaders(
					new HttpHeaders(
						new MultiValueMapAdapter<>(
							Arrays.stream(response.getAllHeaders()).collect(
								Collectors.toMap(
									NameValuePair::getName,
									e -> Collections.singletonList(e.getValue())
								)
							)
						)
					)
				);

			// reporting
			logger.debug(
				"Route request to 3rd party. Url={}, bytes sent={}, bytes received={}",
				trackingEndpoint,
				queryString.getBytes().length,
				response.getEntity() != null ? response.getEntity().getContentLength() : 0
			);

			// media type
			MediaType mediaType = getResponseMediaType(response);

			final int statusCode = response.getStatusLine().getStatusCode();
			logger.debug(
				"Response to caller: Content Type {} | Status Code {}",
				mediaType.toString(),
				statusCode
			);

			// Get the response Body
			byte[] responseBody = null;
			if (response.getEntity() != null) {
				responseBody = IOUtils.toByteArray(response.getEntity().getContent());
			}
			logger.debug("Body:\n{}", Arrays.toString(responseBody));

			return ResponseEntity.status(statusCode).headers(responseHeaders).contentType(mediaType)
			.body(responseBody);

		} catch (Exception e) {
			logger.warn(
				"Failed to proxy request. Endpoint: " + trackingEndpoint + ", Error: "
				+ e.getMessage(),
				e
			);
			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	private MediaType getResponseMediaType(HttpResponse response) {
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (response.getEntity() != null) {
			Header contentType = response.getEntity().getContentType();
			if (contentType != null) {
				mediaType = MediaType.valueOf(contentType.getValue());
			}
		}
		return mediaType;
	}

	protected HttpHeaders getResponseHeaders(final HttpHeaders sourceHeaders) {
		final HttpHeaders whitelistedResponseHeaders = new HttpHeaders();
		whitelistedResponseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");
		if (sourceHeaders.getContentType() != null) {
			whitelistedResponseHeaders.add(
				HttpHeaders.CONTENT_TYPE,
				sourceHeaders.getContentType().toString()
			);
		}

		// add provider specific response headers
		for (final String headerName : getWhitelistedResponseHeaders()) {
			final String headerValue = sourceHeaders.toSingleValueMap().get(headerName);
			if (headerValue != null) {
				whitelistedResponseHeaders.add(headerName, headerValue);
			}
		}
		return whitelistedResponseHeaders;
	}

	/**
	 * Excludes all the blacklisted query params of the request and returns a cleaned query string.
	 */
	String filterQueryString(final Map<String, String> params) {
		return createQueryString(filterBlacklistedData(params));
	}

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

	private Map<String, String> filterBlacklistedData(Map<String, String> data) {
		for (final String blackListedParam : getBlacklistedQueryParams()) {
			data.remove(blackListedParam);
		}
		return data;
	}

	private void addHeaders(HttpServletRequest request, HttpHeaders headers) {
		for (final String headerName : getWhitelistedRequestHeaders()) {
			final String headerValue = request.getHeader(headerName);
			if (headerValue != null) {
				headers.add(headerName, headerValue);
			}
		}

		for (
			final Map.Entry<String, String> header
			: getAdditionalRequestHeaders(request).entrySet()
		) {
			headers.add(header.getKey(), header.getValue());
		}
	}

	private void addWhitelistedCookies(
		final HttpServletRequest request,
		final HttpHeaders		 headers
	) {
		if (getWhitelistedCookieNames().length > 0) {
			headers.add("Cookie", getWhitelistedCookies(request).toString());
		}
	}

	/**
	 * @deprecated  Use {@linkplain #getWhitelistedCookies(HttpServletRequest)} instead. adds
	 *              selected cookies to the request
	 *
	 * @param       connection
	 * @param       request
	 *
	 * @return
	 */
	@Deprecated
	private HttpRequestBase addLegacyWhitelistedCookiesToRequest(
		HttpRequestBase    connection,
		HttpServletRequest request
	) {
		if (getWhitelistedCookieNames().length > 0) {
			connection.addHeader("Cookie", getWhitelistedCookies(request).toString());
		}
		return connection;
	}

	private StringBuilder getWhitelistedCookies(final HttpServletRequest request) {
		final StringBuilder cookies = new StringBuilder();
		for (final String cookieName : getWhitelistedCookieNames()) {
			switch (getCookieNameMatchType()) {

				case FULL :
					appendCookie(cookies, WebUtils.getCookie(request, cookieName));
					break;

				case PREFIX :
					for (Cookie cookieStartingWithPrefix : getCookiesByPrefix(request, cookieName)) {
						appendCookie(cookies, cookieStartingWithPrefix);
					}
					break;

				default :
					break;
			}
		}
		return cookies;
	}

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
	 * debug logging.
	 *
	 * @param  routingEndpoint  routing endpoint url
	 * @param  requestSize      size of request in bytes
	 * @param  responseEntity   logged responseEntity
	 */
	protected void log(
		final String				   routingEndpoint,
		long						   requestSize,
		final ResponseEntity<Resource> responseEntity
	) throws IOException
	{
		final Resource responseBody = responseEntity.getBody();

		if (logger.isDebugEnabled()) {
			logger.debug("truncated responseEntity {}", responseEntity);
			logger.debug(
				"Route request to 3rd party. Url={}, bytes sent={}, bytes received={}",
				routingEndpoint,
				requestSize,
				responseBody != null ? responseBody.contentLength() : 0
			);

			logger.debug(
				"Response to caller: Content Type {} | Status Code {}",
				responseEntity.getHeaders().getContentType(),
				responseEntity.getStatusCode()
			);

			byte[] responseBodyBytes = null;
			if (responseBody != null) {
				responseBodyBytes = IOUtils.toByteArray(responseBody.getInputStream());
			}
			logger.debug("Body:\n {}", Arrays.toString(responseBodyBytes));
		}
	}

	protected String[] getWhitelistedRequestHeaders() {
		return DEFAULT_RETURN_VALUE;
	}

	@Deprecated
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
	 * @param   name   query parameter name
	 * @param   value  query parameter value
	 *
	 * @return  encoded parameter
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
