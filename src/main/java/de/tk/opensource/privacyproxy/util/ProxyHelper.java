package de.tk.opensource.privacyproxy.util;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ProxyHelper {

    public static final int ROUTING_TIMEOUT_MILLISECONDS = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHelper.class);

    private final String proxyHost;
    private final Integer proxyPort;
    private final String nonProxyHosts;
    private final HttpHost httpProxyHost;

    private Proxy proxy;
    private DefaultRoutePlanner proxyRoutePlanner;

    public ProxyHelper(Proxy proxy, final String proxyHost, final Integer proxyPort, final String nonProxyHosts) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.nonProxyHosts = nonProxyHosts;
        this.proxy = proxy;
        this.httpProxyHost = (this.proxyHost != null && this.proxyPort != null) ? new HttpHost(this.proxyHost, this.proxyPort) : null;
    }

    private Proxy getProxy() {
        if (proxy == null) {
            if (proxyHost == null || proxyPort == null) {
                proxy = Proxy.NO_PROXY;
            } else {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            }
        }
        return proxy;
    }

    public DefaultRoutePlanner getProxyRoutePlanner() {
        if (proxyRoutePlanner == null) {
            if (getHttpProxyHost() != null) {
                proxyRoutePlanner = new PrivacyProxyRoutePlanner(this, getHttpProxyHost());
            }
            else {
                LOGGER.debug("No Proxy configured - Using System (JRE) Default");
                proxyRoutePlanner = new SystemDefaultRoutePlanner(ProxySelector.getDefault());
            }
        }
        return proxyRoutePlanner;
    }

    public HttpHost getHttpProxyHost() {
        return httpProxyHost;
    }

    /**
     * Evaluates if a host should be reached directly, bypassing the proxy. The pattern may start or
     * end with a '*' for wildcards.
     *
     * @param hostname
     * @param pattern
     * @return true if hostname matches the given pattern.
     */
    public static boolean matches(String hostname, String pattern) {

        if (pattern.isEmpty()) {
            return false;
        }

        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.startsWith("*")) {

            if (pattern.endsWith("*") && pattern.length() > 1) {
                return hostname.contains(pattern.substring(1, pattern.length() - 1));
            }

            return hostname.endsWith(pattern.substring(1));
        }

        if (pattern.endsWith("*")) {
            return hostname.startsWith(pattern.substring(0, pattern.length() - 1));
        }

        return pattern.equals(hostname);
    }

    /**
     * Evaluates a list of hosts that should be reached directly, bypassing the proxy. This list is
     * configured using the system property 'http.nonProxyHosts'. The list of patterns is separated
     * by '|'. Any host matching one of these patterns will be reached through a direct connection
     * instead of through a proxy.
     *
     * @param url
     * @return the configured {@link Proxy} instance or {@link Proxy#NO_PROXY} when the hostname is
     * excluded.
     */
    public Proxy selectProxy(URL url) {

        // Skip evaluation if no proxy is configured at all
        if (Proxy.NO_PROXY.equals(getProxy()) || !StringUtils.hasText(nonProxyHosts)) {
            return getProxy();
        }

        // The list of excluded host patterns is separated by '|'.
        Stream<String> excluded = Pattern.compile(Pattern.quote("|")).splitAsStream(nonProxyHosts);

        String hostname = url.getHost();
        boolean isExcluded = excluded.anyMatch(pattern -> matches(hostname, pattern.trim()));

        Proxy selection = isExcluded ? Proxy.NO_PROXY : getProxy();
        LOGGER.debug("Using {} for {} ({})", selection, hostname, nonProxyHosts);
        return selection;
    }

    public CloseableHttpClient getCloseableHttpClient() {
        final RequestConfig requestConfig =
                RequestConfig.custom().setConnectTimeout(ROUTING_TIMEOUT_MILLISECONDS)
                        .setConnectionRequestTimeout(ROUTING_TIMEOUT_MILLISECONDS).setSocketTimeout(
                                ROUTING_TIMEOUT_MILLISECONDS
                        )
                        .build();
        return HttpClients.custom().setDefaultRequestConfig(requestConfig).setRoutePlanner(
                        getProxyRoutePlanner()
                )
                .build();
    }
}