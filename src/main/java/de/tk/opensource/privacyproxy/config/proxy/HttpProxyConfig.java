package de.tk.opensource.privacyproxy.config.proxy;

import de.tk.opensource.privacyproxy.util.ProxyHelper;
import de.tk.opensource.privacyproxy.util.RestTemplateProxyCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpProxyConfig {
    @Value("${http.proxyHost:#{null}}")
    private String proxyHost;

    @Value("${http.proxyPort:#{null}}")
    private Integer proxyPort;

    @Value("${http.nonProxyHosts:#{null}}")
    private String nonProxyHosts;

    @Bean
    public ProxyHelper proxyHelper() {
        return new ProxyHelper(null, proxyHost, proxyPort, nonProxyHosts);
    }

    @Bean
    @DependsOn({"proxyHelper"})
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder(new RestTemplateProxyCustomizer(proxyHelper())).build();
    }
}
