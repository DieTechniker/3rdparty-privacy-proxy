/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import de.tk.opensource.privacyproxy.util.ProxyHelper;
import de.tk.opensource.privacyproxy.util.ProxyRoutePlanner;
import de.tk.opensource.privacyproxy.util.RestTemplateProxyCustomizer;

@Configuration
public class HttpProxyConfig {

	@Value("${http.proxyHost:#{null}}")
	private String proxyHost;

	@Value("${http.proxyPort:#{null}}")
	private Integer proxyPort;

	@Value("${http.nonProxyHosts:#{null}}")
	private String nonProxyHosts;

	@Bean
	public Proxy proxy() {
		if (proxyHost == null || proxyPort == null) {
			return Proxy.NO_PROXY;
		} else {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		}
	}

	@Bean
	public HttpHost httpHost() {
		if (proxyHost != null && proxyPort != null) {
			return new HttpHost(proxyHost, proxyPort);
		}
		return null;
	}

	@Bean
	@DependsOn("proxy")
	public ProxyHelper proxyHelper() {
		return new ProxyHelper(proxy(), nonProxyHosts);
	}

	@Bean
	@DependsOn({ "httpHost", "proxyHelper" })
	public ProxyRoutePlanner proxyRoutePlanner() {
		return new ProxyRoutePlanner(proxyHelper(), httpHost());
	}

	@Bean
	@DependsOn({ "proxyHelper", "proxyRoutePlanner" })
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder(
			new RestTemplateProxyCustomizer(proxyRoutePlanner(), proxyHelper())
		)
		.build();
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
