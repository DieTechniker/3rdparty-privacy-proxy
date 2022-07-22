package de.tk.opensource.privacyproxy.delivery;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * This is an implementation of the AssetDeliveryController to deliver JavsScript files.
 */
@Controller
public class JavscriptDeliveryController extends AssetDeliveryController {

	/**
	 * Sets up a cachegroup as well as the URL mapping under which this content should be
	 * accessible.
	 */
	@Cacheable(
		cacheNames = "js",
		key = "#provider + #script"
	)
	@GetMapping(
		value = "/{provider:[a-zA-Z\\-]+$}/{script:[a-zA-Z0-9\\.\\-\\_]+\\.js$}",
		produces = "application/javascript"
	)
	public ResponseEntity getScript(
		@PathVariable("provider") String provider,
		@PathVariable("script") String script
	) {
		return super.getAssetInternal(provider, script);
	}
}