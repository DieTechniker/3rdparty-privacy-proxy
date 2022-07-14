/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateProxyCustomizer implements RestTemplateCustomizer {

	public static final int ROUTING_TIMEOUT_IN_SECS = 5;

	@Autowired
	private ProxyRoutePlanner proxyRoutePlanner;

	@Autowired
	private ProxyHelper proxyHelper;

	@Override
	public void customize(RestTemplate restTemplate) {
		restTemplate.setRequestFactory(
			new HttpComponentsClientHttpRequestFactory(
				proxyHelper.getCloseableHttpClient(proxyRoutePlanner)
			)
		);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
