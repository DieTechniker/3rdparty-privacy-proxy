/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
* This class downloads files from a remote host and stores them grouped by "provider" on the file System.
* You can download any arbitrary file. If the endpoint is a ZIP it will be extracted and its content will be stored.
* Please implement your own Service per provider. See the ExampleRetrievalScriptService for an example.
* The regular fetch is done by the Spring Boot cron system which is setup in your provider retrieval class.
*
* All configs for the retrieval and your providers should be done in the application.yml within the Application
*/
public abstract class AssetRetrievalService implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssetRetrievalService.class);

	/**
	* Configure where the files will be stored on the file system.
	*/
	@Value("${assets.fileLocation}")
	private String location;

	@Autowired
	private Proxy proxy;

	/**
	* Method to fetch the assets defined in the proper implementation class. You need to implement one AssetRetrievalService class per Provider.
	* A provider is a logical group which should represent the source of the files.
	* e.g. a "google-fonts" provider would be a useful case
	* The endpoints can be configured as a single URL or a list of comma separated URLs. Put them into the application.yml
	* If the endpoint delivers a ZIP file it will be extracted
	*/
	public void updateAssetsInternal(String provider, List<String> endpoints) {
		boolean directoryAvailable;
		File directory = new File(location + "/" + provider);
		if (directory.exists()) {
			directoryAvailable = true;
		} else {
			directoryAvailable = directory.mkdirs();
		}
		if (directoryAvailable) {
			try {
				LOGGER.info("Updating the {} assets", provider);
				for (String endpoint : endpoints) {
					// Access the endpoint with a cachebuster to prevent network middleware (corporate proxies) to cache the response
					URL url = new URL(endpoint + "?_=" + System.currentTimeMillis());
					URLConnection connection = url.openConnection(proxy);
					// If the endpoint delivers ZIP, extract it.
					// TODO: Allow configuration per endpoint to force extract. 
					// The filename might not be enough (Google Fonts Downloads e.g. do not resolve here)
					if (endpoint.endsWith(".zip")) {
						try(
							ZipInputStream inputStream =
								new ZipInputStream(connection.getInputStream())

						) {
							ZipEntry zipEntry = inputStream.getNextEntry();
							while (zipEntry != null) {
								String fileName = zipEntry.getName();
								writeFile(inputStream, provider, fileName);
								zipEntry = inputStream.getNextEntry();
							}
							inputStream.closeEntry();
						}
					} else {
						try(InputStream in = connection.getInputStream()) {
							writeFile(in, provider, endpoint.substring(endpoint.lastIndexOf("/")));
						}
					}
				}
				LOGGER.info("Done updating the {} assets", provider);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		} else {
			LOGGER.error("unable to create directory %s", location);
		}
	}

	public abstract void updateAssets();

	@Override
	public void afterPropertiesSet() {
		this.updateAssets();
	}

	/**
	* Writes the retrieved files to disk
	*/
	public void writeFile(InputStream inputStream, String provider, String fileName) {
		byte[] buffer = new byte[1024];
		try(
			FileOutputStream fos = new FileOutputStream(location + "/" + provider + "/" + fileName)
		) {
			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} catch (FileNotFoundException f) {
			LOGGER.error(String.format("unable to write file %s", fileName), f);
		} catch (IOException i) {
			LOGGER.error(String.format("unable to write file %s", fileName), i);
		}
	}

}
