/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
@EnableCaching
@EnableScheduling
public class PrivacyProxyConfig {

	@Bean
	public ConversionService conversionService() {
		return new DefaultConversionService();
	}

	@Bean
	public Proxy proxy(
		@Value("${http.proxyHost}") String proxyHost,
		@Value("${http.proxyPort}") Integer proxyPort
	) {
		if (proxyHost == null || proxyPort == null) {
			return Proxy.NO_PROXY;
		} else {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		}
	}

}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
