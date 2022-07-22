package de.tk.opensource.privacyproxy.config.proxy;

import de.tk.opensource.privacyproxy.util.ProxyHelper;
import de.tk.opensource.privacyproxy.util.RestTemplateProxyCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpProxyConfig {

    @Bean
    public ProxyHelper proxyHelper() {
        return new ProxyHelper(null, null);
    }

    @Bean
    @DependsOn({"proxyHelper"})
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder(new RestTemplateProxyCustomizer(proxyHelper())).build();
    }
}
