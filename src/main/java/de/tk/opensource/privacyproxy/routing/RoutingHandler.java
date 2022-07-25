package de.tk.opensource.privacyproxy.routing;

import de.tk.opensource.privacyproxy.config.CookieNameMatchType;
import de.tk.opensource.privacyproxy.config.ProviderRequestMethod;
import de.tk.opensource.privacyproxy.config.UrlPattern;
import de.tk.opensource.privacyproxy.util.ProxyHelper;
import de.tk.opensource.privacyproxy.util.RequestUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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

    public static final String EXCEPTION_PROXY_MESSAGE =
            "Failed to proxy request. Endpoint: %s, Error: %s";
    private static final String[] DEFAULT_RETURN_VALUE = new String[0];
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProxyHelper proxyHelper;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Basic implementation for requests, which are routed through the privacy-proxy. It can be
     * configured by overriding certain methods. Every routing endpoint must have a dedicated
     * specific handler.
     */
    public ResponseEntity<Resource> handleGenericRequestInternal(
            final String targetEndpoint,
            final Map<String, String> queryStrings,
            final HttpServletRequest request,
            @Nullable final String body,
            final HttpMethod method
    ) {
        if (method == HttpMethod.POST && body == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final String queryString = filterQueryString(queryStrings);
        final URI uri =
                UriComponentsBuilder.fromUriString(targetEndpoint).query(queryString).build(true)
                        .toUri();

        final HttpHeaders headers = getRequestHeaders(request);
        addWhitelistedCookies(request, headers);

        final HttpEntity<String> httpEntity =
                body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
        try {
            logger.debug("Calling {} with method {}", uri, method);
            final ResponseEntity<Resource> responseEntity =
                    restTemplate.exchange(uri, method, httpEntity, Resource.class);

            final HttpHeaders responseHeaders = responseEntity.getHeaders();
            final ResponseEntity<Resource> customResponseEntity =
                    ResponseEntity.status(responseEntity.getStatusCode()).headers(
                                    whitelistResponseHeaders(responseHeaders)
                            )
                            .body(responseEntity.getBody());

            log(targetEndpoint, queryString.getBytes().length, customResponseEntity, body);

            return customResponseEntity;
        } catch (HttpStatusCodeException e) {
            logger.warn(
                    String.format(EXCEPTION_PROXY_MESSAGE, targetEndpoint, e.getMessage())
                            + ", with status code: " + e.getStatusCode(),
                    e
            );
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
        } catch (IOException ee) {
            logger.warn(
                    String.format(EXCEPTION_PROXY_MESSAGE, targetEndpoint, ee.getMessage()),
                    ee
            );
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
        }
    }

    /**
     * @deprecated Use {@linkplain #handleGenericRequestInternal(String, Map, HttpServletRequest,
     * String, HttpMethod)} instead. Basic implementation for requests, which are
     * routed through the privacy-proxy. It can be configured by overriding certain
     * methods. Every routing endpoint must have a dedicated specific handler.
     */
    @Deprecated
    public ResponseEntity<Object> handlePostInternal(
            HttpServletRequest request,
            Map<String, String> data,
            String trackingEndpoint
    ) {
        try (
                final CloseableHttpClient httpClient =
                        proxyHelper.getCloseableHttpClient()
        ) {
            HttpRequestBase httpRequest;

            final String queryString = filterQueryString(data);
            final URI url =
                    new URI(trackingEndpoint + (!"".equals(queryString) ? "?" + queryString : ""));

            if (getRequestMethod() == ProviderRequestMethod.POST) {
                httpRequest = new HttpPost(url);
            } else {
                httpRequest = new HttpGet(url);
            }

            addLegacyWhitelistedCookiesToRequest(httpRequest, request);

            for (final String headerName : getWhitelistedRequestHeaders()) {
                final String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    httpRequest.addHeader(headerName, headerValue);
                }
            }

            for (
                    final Map.Entry<String, String> header
                    : getAdditionalRequestHeaders(request).entrySet()
            ) {
                httpRequest.addHeader(header.getKey(), header.getValue());
            }

            logger.debug("Calling {}", httpRequest.getURI());

            final HttpResponse response = httpClient.execute(httpRequest);
            final HttpHeaders responseHeaders = getHttpHeaders(response);

            logger.debug(
                    "Route request to 3rd party. Url={}, bytes sent={}, bytes received={}",
                    trackingEndpoint,
                    queryString.getBytes().length,
                    response.getEntity() != null ? response.getEntity().getContentLength() : 0
            );

            MediaType mediaType = getResponseMediaType(response);

            final int statusCode = response.getStatusLine().getStatusCode();
            logger.debug(
                    "Response to caller: Content Type {} | Status Code {}",
                    mediaType.toString(),
                    statusCode
            );

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
        }
    }

    @Deprecated
    private HttpHeaders getHttpHeaders(HttpResponse response) {

        //J-
        final Map<String, List<String>> headers = Arrays
                .stream(response.getAllHeaders())
                .collect(Collectors.toMap(
                                NameValuePair::getName, e -> Collections.singletonList(e.getValue())
                        )
                );
        //J+
        final MultiValueMapAdapter<String, String> springHeaders =
                new MultiValueMapAdapter<>(headers);

        return whitelistResponseHeaders(new HttpHeaders(springHeaders));
    }

    @Deprecated
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

    protected HttpHeaders whitelistResponseHeaders(final HttpHeaders sourceHeaders) {
        final HttpHeaders whitelistedResponseHeaders = new HttpHeaders();
        whitelistedResponseHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        if (sourceHeaders.getContentType() != null) {
            whitelistedResponseHeaders.add(
                    HttpHeaders.CONTENT_TYPE,
                    sourceHeaders.getContentType().toString()
            );
        }

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

    HttpHeaders getRequestHeaders(final HttpServletRequest request) {
        final HttpHeaders headers = new HttpHeaders();
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
        return headers;
    }

    void addWhitelistedCookies(final HttpServletRequest request, final HttpHeaders headers) {
        if (getWhitelistedCookieNames().length > 0) {
            headers.add("Cookie", getWhitelistedCookies(request).toString());
        }
    }

    /**
     * @param connection
     * @param request
     * @return
     * @deprecated Use {@linkplain #getWhitelistedCookies(HttpServletRequest)} instead. adds
     * selected cookies to the request
     */
    @Deprecated
    private HttpRequestBase addLegacyWhitelistedCookiesToRequest(
            HttpRequestBase connection,
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
            final CookieNameMatchType cookieNameMatchType = getCookieNameMatchType();
            if (cookieNameMatchType == CookieNameMatchType.FULL) {
                appendCookie(cookies, WebUtils.getCookie(request, cookieName));
            } else if (cookieNameMatchType == CookieNameMatchType.PREFIX) {
                for (
                        final Cookie cookieStartingWithPrefix : getCookiesByPrefix(request, cookieName)
                ) {
                    appendCookie(cookies, cookieStartingWithPrefix);
                }
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
     * @param routingEndpoint routing endpoint url
     * @param requestSize     size of request in bytes
     * @param responseEntity  logged responseEntity
     * @param body
     */
    protected void log(
            final String routingEndpoint,
            long requestSize,
            final ResponseEntity<Resource> responseEntity,
            final String body
    ) throws IOException {
        final Resource responseBody = responseEntity.getBody();

        if (logger.isDebugEnabled()) {
            logger.debug("Request body was: {}", body);
            logger.debug("Truncated responseEntity: {}", responseEntity);
            logger.debug(
                    "Route request to 3rd party. Url={}, query bytes sent={}, bytes received={}",
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
            logger.debug("Response body: {}", Arrays.toString(responseBodyBytes));
        }
    }

    protected String[] getWhitelistedRequestHeaders() {
        return DEFAULT_RETURN_VALUE;
    }

    /**
     * @return
     * @deprecated because the {@linkplain #handleGenericRequestInternal(String, Map,
     * HttpServletRequest, String, HttpMethod)} can handle both method types.
     */
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
