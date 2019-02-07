/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This is an example implementation of the AssetRetrievalService.
 * Please implement your own DeliveryControllers per provider and file type.
 * Dependent on what should be delivered, a specific FontRetrievalService could be another useful implementation.
 */
@ConditionalOnProperty("example.scripts.update")
@Service
public class ExampleScriptRetrievalService extends AssetRetrievalService {

	@Value("${example.scripts.endpoints}")
	private List<String> endpoints;

	/**
	 * Downloads the Example Scripts into the configured folder. This method is scheduled and evicts
	 * the script cache.
	 */
	@CacheEvict(
		cacheNames = "js",
		allEntries = true
	)
	@Override
	@Scheduled(cron = "${example.scripts.interval}")
	public void updateAssets() {
		updateAssetsInternal("example", endpoints);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
