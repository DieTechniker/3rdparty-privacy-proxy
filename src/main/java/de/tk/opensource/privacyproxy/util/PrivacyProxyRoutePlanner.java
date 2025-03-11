package de.tk.opensource.privacyproxy.util;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class PrivacyProxyRoutePlanner implements HttpRoutePlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyProxyRoutePlanner.class);

    private final ProxyHelper proxyHelper;

    private final DefaultProxyRoutePlanner defaultPlanner;

    public PrivacyProxyRoutePlanner(final ProxyHelper proxyHelper, final HttpHost httpHost) {
        this.proxyHelper = proxyHelper;
        this.defaultPlanner = new DefaultProxyRoutePlanner(httpHost);
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpContext context) throws HttpException {
        try {
            if (
                    Proxy.NO_PROXY.equals(
                            proxyHelper.selectProxy(new URL(host.toURI()))
                    )
            ) {
                LOGGER.debug("No Proxy for - {}", host);
                return new HttpRoute(host);
            }
        } catch (MalformedURLException e) {
            LOGGER.error(
                    "Could not build URL for proxy/no-proxy evaluation. Uri: '{}'",
                    host.toURI(),
                    e
            );
        }
        LOGGER.debug("Using Proxy for {}", host);
        return this.defaultPlanner.determineRoute(host, context);
    }
}
