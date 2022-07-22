package de.tk.opensource.privacyproxy.delivery;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tk.opensource.privacyproxy.config.UrlPattern;

/**
 * This is the AssetDeliveryController abstract implementation. It will search for a given asset
 * name within a provider and delivers a file to the request. Build your own file type specific
 * delivery controller to deliver JS, CSS, fonts, etc.
 */
@Controller
@RequestMapping(value = UrlPattern.Contexts.DELIVERY)
public abstract class AssetDeliveryController {

	protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

	@Autowired
	private ResourceLoader resourceLoader;

	/**
	 * Configuration where to look for the files on the disk (root folder)
	 */
	@Value("${assets.fileLocation}")
	private String location;

	/**
	 * Tries to fetch the requested file from the file system. It wlll look in a folder named like
	 * the provider for the requested asset.
	 *
	 * @param  provider  Identifier of the provider under which we will look for the file
	 * @param  asset     Identifier of the asset to deliver
	 */
	protected ResponseEntity getAssetInternal(String provider, String asset) {
		byte[] result = null;
		Resource[] resources = getResources(provider + "/" + asset);
		if (resources.length > 0) {
			Resource file = resources[0];
			try(InputStream stream = file.getInputStream()) {
				result = StreamUtils.copyToByteArray(stream);
			} catch (IOException e) {
				LOGGER.error(
					"An error occured while working on file {}. Exception: {}",
					file.getFilename(),
					e.getMessage()
				);
			}
		}

		/**
		 * We do not want to allow any caching of these resources for now.
		 * The expires header is not necessary but just to not deliver human-confusing cache directives
		 * TODO: Allow caching of the assets while they are not being updated (cache time = cron interval from config)
		 */
		HttpHeaders header = new HttpHeaders();
		header.setCacheControl("no-cache");
		header.setExpires(0L);
		header.setContentDisposition(ContentDisposition.builder("inline").filename(asset).build());

		if (result != null) {

			// Log the request to this asset somewhere. Might be useful for usage statistics or other internal statistics
			trackAssetRequest(provider + "/" + asset, result.length);

			// Return the asset file itself
			return ResponseEntity.ok().headers(header).body(result);
		}

		// Return 404 if no asset was found to deliver
		return ResponseEntity.notFound().build();
	}

	/**
	 * Get the requested file from the disk
	 *
	 * @param  filename  the requested filename
	 */
	private Resource[] getResources(String filename) {
		String path = "file:" + location + File.separator + filename;
		PathMatchingResourcePatternResolver resolver =
			new PathMatchingResourcePatternResolver(resourceLoader);
		Resource[] resources = new Resource[] {};
		try {
			resources = resolver.getResources(path);
		} catch (IOException e) {
			LOGGER.error(
				"Unable to get resources from path {}. Exception: {}",
				path,
				e.getMessage()
			);
		}
		return resources;
	}

	/**
	 * simple implementation of asset request tracking
	 *
	 * @param  assetUrl      asset url
	 * @param  responseSize  size of reponse in size
	 */
	protected void trackAssetRequest(String assetUrl, long responseSize) {
		LOGGER.debug("Deliver 3rd party asset. Url={}, bytes sent={}", assetUrl, responseSize);
	}

}