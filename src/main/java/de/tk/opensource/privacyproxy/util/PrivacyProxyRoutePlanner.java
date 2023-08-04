package de.tk.opensource.privacyproxy.util;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;

public class PrivacyProxyRoutePlanner extends DefaultProxyRoutePlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyProxyRoutePlanner.class);

    private final ProxyHelper proxyHelper;

    public PrivacyProxyRoutePlanner(final ProxyHelper proxyHelper, final HttpHost httpHost) {
        super(httpHost);
        this.proxyHelper = proxyHelper;
    }

    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context)
            throws HttpException {
        try {
            if (
                    Proxy.NO_PROXY.equals(
                            proxyHelper.selectProxy(new URL(request.getRequestUri()))
                    )
            ) {
                LOGGER.debug("No Proxy for - {}", host);
                return new HttpRoute(host);
            }
        } catch (MalformedURLException e) {
            LOGGER.error(
                    "Could not build URL for proxy/no-proxy evaluation. Uri: '{}'",
                    request.getRequestUri(),
                    e
            );
        }
        LOGGER.debug("Using Proxy for {}", host);
        return super.determineRoute(host, context);
    }
}
