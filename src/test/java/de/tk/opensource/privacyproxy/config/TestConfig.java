/*--- (C) 1999-2021 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

import de.tk.opensource.privacyproxy.config.proxy.HttpProxyConfig;

@ComponentScan(
	basePackages =
		{
			"de.tk.opensource.privacyproxy.routing", "de.tk.opensource.privacyproxy.util",
			"de.tk.opensource.privacyproxy.config.proxy"
		}
)
@SpringBootConfiguration
public class TestConfig {
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
