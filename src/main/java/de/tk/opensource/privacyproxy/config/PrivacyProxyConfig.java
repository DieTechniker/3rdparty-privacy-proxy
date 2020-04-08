/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

@Configuration
@EnableCaching
@EnableConfigurationProperties
@EnableRetry
@EnableScheduling
public class PrivacyProxyConfig {

	@Bean
	public ConversionService conversionService() {
		return new DefaultConversionService();
	}

	@Bean
	public Proxy proxy(
		@Value("${http.proxyHost:''}") String proxyHost,
		@Value("${http.proxyPort:-1}") Integer proxyPort
	) {
		if (StringUtils.isEmpty(proxyHost) || proxyPort == -1) {
			return Proxy.NO_PROXY;
		} else {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		}
	}

	@Bean
	public List<RetryListener> retryListeners() {
		Logger log = LoggerFactory.getLogger(getClass());

		return Collections.singletonList(
			new RetryListenerSupport() {
				@Override
				public <T, E extends Throwable> void onError(
					RetryContext		context,
					RetryCallback<T, E> callback,
					Throwable			throwable
				) {
					log.warn(
						"Retryable method {} threw {}th exception {}",
						context.getAttribute("context.name"),
						context.getRetryCount(),
						throwable.getMessage()
					);
				}
			}
		);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
