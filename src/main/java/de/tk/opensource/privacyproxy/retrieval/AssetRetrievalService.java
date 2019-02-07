/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import de.tk.opensource.privacyproxy.config.RetrievalEndpoint;
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
* This class downloads files from a remote host and stores them grouped by "provider" on the file system.
* You can download any arbitrary file. If the endpoint is a ZIP it will be extracted and its content will be stored.
* Please implement your own Service per provider. See the ExampleScriptRetrievalService for an example.
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
				for (RetrievalEndpoint endpoint : endpoints) {
					// Access the endpoint with a cachebuster to prevent network middleware (corporate proxies) to cache the response
					URL url = new URL(endpoint.getRemoteUrlWithCacheBuster());
					URLConnection connection = url.openConnection(proxy);

					// retrieve remote asset(s) and extract if necessary
					retrieveAsset(connection, provider, endpoint.getFilename());
				}
				LOGGER.info("Done updating the {} assets", provider);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		} else {
			LOGGER.error("unable to create directory {}", location);
		}
	}

	/**
	 * Retrieve asset from connection and extract if it is a ZIP file.
	 */
	private void retrieveAsset(URLConnection connection, String provider, String filename) throws IOException {
		// If the endpoint delivers ZIP, extract it.
		if (filename.endsWith(".zip")) {
			try (ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream())) {
				ZipEntry zipEntry = zipInputStream.getNextEntry();
				while (zipEntry != null) {
					writeFile(zipInputStream, provider, zipEntry.getName());
					zipEntry = zipInputStream.getNextEntry();
				}
				zipInputStream.closeEntry();
			}
		} else {
			try (InputStream in = connection.getInputStream()) {
				writeFile(in, provider, filename);
			}
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
	private void writeFile(InputStream inputStream, String provider, String filename) {
		byte[] buffer = new byte[1024];
		try(
			FileOutputStream fos = new FileOutputStream(location + "/" + provider + "/" + filename)
		) {
			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} catch (FileNotFoundException f) {
			LOGGER.error(String.format("unable to write file %s", filename), f);
		} catch (IOException i) {
			LOGGER.error(String.format("unable to write file %s", filename), i);
		}
	}

}
