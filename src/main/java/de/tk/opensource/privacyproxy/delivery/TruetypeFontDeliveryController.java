/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.delivery;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
* This is an implementation of the AssetDeliveryController to deliver truetype fonts.
*/
@Controller
public class TruetypeFontDeliveryController extends AssetDeliveryController {

	/**
	* Sets up cachegroup as well as the URL mapping under which this content should be accessible.
	*/
	@Cacheable(
		cacheNames = "ttf",
		key = "#provider + #font"
	)
	@GetMapping(
		value = "/{provider:[a-zA-Z\\-]+$}/{font:[a-zA-Z0-9\\.\\-\\_]+\\.ttf$}",
		produces = "font/ttf"
	)
	public ResponseEntity getFont(
		@PathVariable("provider") String provider,
		@PathVariable("font") String font
	) {
		return super.getAssetInternal(provider, font);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
