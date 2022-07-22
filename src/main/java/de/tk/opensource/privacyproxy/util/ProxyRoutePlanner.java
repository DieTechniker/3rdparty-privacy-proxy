/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyRoutePlanner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRoutePlanner.class);

	private final ProxyHelper proxyHelper;
	private final HttpHost httpHost;

	private DefaultRoutePlanner routePlanner;

	public ProxyRoutePlanner(ProxyHelper proxyHelper, HttpHost httpHost) {
		this.proxyHelper = proxyHelper;
		this.httpHost = httpHost;
		initRoutePlanner();
	}

	private void initRoutePlanner() {
		if (httpHost != null) {
			routePlanner =
				new DefaultProxyRoutePlanner(httpHost) {
					@Override
					public HttpRoute determineRoute(
						HttpHost    host,
						HttpRequest request,
						HttpContext context
					) throws HttpException
					{

						// evaluate noProxyHosts
						try {
							if (
								Proxy.NO_PROXY.equals(
									proxyHelper.selectProxy(
										new URL(request.getRequestLine().getUri())
									)
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
				};
		} else {

			// Use JRE Default Proxy Settings
			routePlanner = new SystemDefaultRoutePlanner(ProxySelector.getDefault());
			LOGGER.debug("No Proxy configured - Using System (JRE) Default");
		}
	}

	public DefaultRoutePlanner getRoutePlanner() {
		return routePlanner;
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
