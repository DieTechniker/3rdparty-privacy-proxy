/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import de.tk.opensource.privacyproxy.config.RetrievalEndpoint;

/**
 * This class downloads files from a remote host and stores them grouped by "provider" on the file
 * system. You can download any arbitrary file. If the endpoint is a ZIP it will be extracted and
 * its content will be stored. Please implement your own Service per provider. The regular fetch is
 * done by the Spring Boot cron system which is setup in your provider retrieval class. All configs
 * for the retrieval and your providers should be done in the application.yml within the Application
 */
public abstract class AssetRetrievalService implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssetRetrievalService.class);

	/**
	 * Configure where the files will be stored on the file system.
	 */
	@Value("${assets.fileLocation}")
	private String location;

	@Autowired
	private AssetRetryRetrievalService assetRetryRetrievalService;

	/**
	 * Method to fetch the assets defined in the proper implementation class. You need to implement
	 * one AssetRetrievalService class per Provider. A provider is a logical group which should
	 * represent the source of the files. Put them into the application.yml If the endpoint delivers
	 * a ZIP file it will be extracted
	 */
	public void updateAssetsInternal(String provider, List<RetrievalEndpoint> endpoints) {
		boolean directoryAvailable;
		File directory = new File(location + File.separator + provider);
		if (directory.exists()) {
			directoryAvailable = true;
		} else {
			directoryAvailable = directory.mkdirs();
		}
		if (directoryAvailable) {
			try {
				LOGGER.info("Updating the {} assets", provider);
				for (final RetrievalEndpoint endpoint : endpoints) {
					assetRetryRetrievalService.retrieveAsset(provider, endpoint);
				}
				LOGGER.info("Done updating the {} assets", provider);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		} else {
			LOGGER.error("unable to create directory {}", location);
		}
	}

	public abstract void updateAssets();

	@Override
	public void afterPropertiesSet() {
		this.updateAssets();
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
