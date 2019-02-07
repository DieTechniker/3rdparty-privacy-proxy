/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.Charset;

/**
* This is an example implementation of the AssetDeliveryController
* Please implement your own DeliveryControllers per provider and file type.
 * Dependent on what should be delivered, a specific FontDeliveryController could be another useful delivery.
*/
@Controller
public class ExampleScriptDeliveryController extends AssetDeliveryController {

	/**
	* Sets up Cachegroups as well as the URL mapping under which this content should be accessible.
	* Make sure to set the both following annotations properly to your usecase.
	* Also ensure that the URL mapping is unique. Add a slug like /js/{provider...}/{script...} or /fonts/....
	*/
	@Cacheable(
		cacheNames = "js",
		key = "#provider + #script"
	)
	@GetMapping(
		value = "/{provider:[a-zA-Z\\-]+$}/{script:[a-zA-Z0-9\\.\\-\\_]+$}",
		produces = "application/javascript"
	)
	public ResponseEntity getScript(
		@PathVariable("provider") String provider,
		@PathVariable("script") String script
	) {
		return super.getAssetInternal(provider, script);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
