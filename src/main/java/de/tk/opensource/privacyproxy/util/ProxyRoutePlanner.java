package de.tk.opensource.privacyproxy.util;

import org.apache.http.HttpHost;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated Use {@linkplain PrivacyProxyRoutePlanner} instead
 */
@Deprecated
public class ProxyRoutePlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutePlanner.class);

    private final ProxyHelper proxyHelper;
    private final HttpHost httpHost;

    public ProxyRoutePlanner(final ProxyHelper proxyHelper, final HttpHost httpHost) {
        this.proxyHelper = proxyHelper;
        this.httpHost = httpHost;
    }

    /**
     * @deprecated use {@linkplain ProxyHelper#getProxyRoutePlanner()} instead
     *
     * @return a proxy route planner
     */
    @Deprecated
    public DefaultRoutePlanner getRoutePlanner() {
        return proxyHelper.getProxyRoutePlanner();
    }
}