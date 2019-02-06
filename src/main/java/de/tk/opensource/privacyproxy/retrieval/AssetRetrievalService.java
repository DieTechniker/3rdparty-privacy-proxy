/*--- (C) 1999-2017 Techniker Krankenkasse ---*/

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

public abstract class AssetRetrievalService implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssetRetrievalService.class);

	@Value("${assets.fileLocation}")
	private String location;

	@Autowired
	private Proxy proxy;

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
					URL url = new URL(endpoint + "?_=" + System.currentTimeMillis());
					URLConnection connection = url.openConnection(proxy);
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

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
