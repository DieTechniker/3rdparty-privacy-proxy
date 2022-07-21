/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpProxyConfig {

	@Bean
	public Proxy proxy(
		@Value("${http.proxyHost:#{null}}") String proxyHost,
		@Value("${http.proxyPort:#{null}") Integer proxyPort
	) {
		if (proxyHost == null || proxyPort == null) {
			return Proxy.NO_PROXY;
		} else {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		}
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
