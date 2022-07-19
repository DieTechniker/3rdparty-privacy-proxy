/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateHelper {

	private final RestTemplateProxyCustomizer restTemplateProxyCustomizer;

	public RestTemplateHelper(RestTemplateProxyCustomizer restTemplateProxyCustomizer) {
		this.restTemplateProxyCustomizer = restTemplateProxyCustomizer;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder(restTemplateProxyCustomizer).build();
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
