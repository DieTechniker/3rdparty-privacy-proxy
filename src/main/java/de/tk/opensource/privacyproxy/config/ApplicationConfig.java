package de.tk.opensource.privacyproxy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.List;

@Configuration("PrivacyProxyConfig")
@EnableCaching
@EnableConfigurationProperties
@EnableRetry
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public ConversionService conversionService() {
        return new DefaultConversionService();
    }

    @Bean
    public List<RetryListener> retryListeners() {
        Logger log = LoggerFactory.getLogger(getClass());

        return Collections.singletonList(
                new RetryListener() {
                    @Override
                    public <T, E extends Throwable> void onError(
                            RetryContext context,
                            RetryCallback<T, E> callback,
                            Throwable throwable
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
