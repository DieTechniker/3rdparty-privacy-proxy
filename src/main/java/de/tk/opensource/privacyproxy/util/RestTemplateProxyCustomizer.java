package de.tk.opensource.privacyproxy.util;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RestTemplateProxyCustomizer implements RestTemplateCustomizer {

    private final ProxyHelper proxyHelper;

    public RestTemplateProxyCustomizer(
            ProxyHelper proxyHelper
    ) {
        this.proxyHelper = proxyHelper;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(
                        proxyHelper.getCloseableHttpClient()
                )
        );
    }
}
