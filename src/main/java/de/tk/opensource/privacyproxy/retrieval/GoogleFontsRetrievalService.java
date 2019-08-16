/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.tk.opensource.privacyproxy.config.RetrievalEndpoint;

/**
 * This is an implementation of the AssetRetrievalService to download Google Fonts.
 */
@Component
@ConditionalOnProperty("google.fonts.update")
@ConfigurationProperties(prefix = "google.fonts")
public class GoogleFontsRetrievalService extends AssetRetrievalService {

	private List<RetrievalEndpoint> endpoints;

	public List<RetrievalEndpoint> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<RetrievalEndpoint> endpoints) {
		this.endpoints = endpoints;
	}

	/**
	 * Downloads the Google Fonts into the configured folder. This method is scheduled and evicts
	 * the ttf cache.
	 */
	@CacheEvict(
		cacheNames = "ttf",
		allEntries = true
	)
	@Override
	@Scheduled(cron = "${google.fonts.interval}")
	public void updateAssets() {
		updateAssetsInternal("google", endpoints);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
