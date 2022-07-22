package de.tk.opensource.privacyproxy.util;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class ProxyRoutePlanner extends DefaultProxyRoutePlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutePlanner.class);

    private final ProxyHelper proxyHelper;

    public ProxyRoutePlanner(ProxyHelper proxyHelper, HttpHost httpHost) {
        super(httpHost);
        this.proxyHelper = proxyHelper;
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context)
            throws HttpException {
        try {
            if (
                    Proxy.NO_PROXY.equals(
                            proxyHelper.selectProxy(new URL(request.getRequestLine().getUri()))
                    )
            ) {
                LOGGER.debug("No Proxy for - {}", host);
                return new HttpRoute(host);
            }
        } catch (MalformedURLException e) {
            LOGGER.error(
                    "Could not build URL for proxy/no-proxy evaluation. Uri: '{}'",
                    request.getRequestLine().getUri(),
                    e
            );
        }
        LOGGER.debug("Using Proxy for {}", host);
        return super.determineRoute(host, request, context);
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