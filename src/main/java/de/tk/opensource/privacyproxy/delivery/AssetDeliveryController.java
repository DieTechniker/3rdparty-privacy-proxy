/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Controller
public abstract class AssetDeliveryController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssetDeliveryController.class);
	private static final Charset CHARSET = Charset.forName("UTF-8");

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${assets.fileLocation}")
	private String location;

	protected ResponseEntity getAssetInternal(String provider, String asset) {
		String result = "";
		Resource[] resources = getResources(provider + "/" + asset);
		if (resources.length > 0) {
			Resource file = resources[0];
			try(InputStream stream = file.getInputStream()) {
				result = StreamUtils.copyToString(stream, CHARSET);
			} catch (IOException e) {
				LOGGER.error(
					"An error occured while working on file {}. Exception: {}",
					file.getFilename(),
					e.getMessage()
				);
			}
		}

		HttpHeaders header = new HttpHeaders();
		header.add(HttpHeaders.CACHE_CONTROL, "no-cache");

		if (result.length() > 0) {

			// reporting
			trackAssetRequest(provider + "/" + asset, result.length());

			// asset response
			return ResponseEntity.status(HttpStatus.OK).headers(header).body(result);
		}
		return ResponseEntity.notFound().build();
	}

	private Resource[] getResources(String fileName) {
		String path = "file:" + location + "/" + fileName;
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
		Resource[] resources = new Resource[] {};
		try {
			resources = resolver.getResources(path);
		} catch (IOException e) {
			LOGGER.error("Unable to get resources from path {}. Exception: {}", path, e.getMessage());
		}
		return resources;
	}

	/**
	 * simple implementation of asset request tracking
	 * @param assetUrl asset url
	 * @param responseSize size of reponse in size
	 */
	protected void trackAssetRequest(String assetUrl, long responseSize) {
		LOGGER.debug("Deliver 3rd party asset. Url={}, bytes sent={]", assetUrl, responseSize);
	}

}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
