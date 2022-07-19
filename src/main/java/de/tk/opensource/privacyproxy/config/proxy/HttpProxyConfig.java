/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class HttpProxyConfig {

	@Bean
	public Proxy proxy(
		@Value("${http.proxyHost:''}") String proxyHost,
		@Value("${http.proxyPort:-1}") Integer proxyPort
	) {
		if (!StringUtils.hasText(proxyHost) || proxyPort == -1) {
			return Proxy.NO_PROXY;
		} else {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		}
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
