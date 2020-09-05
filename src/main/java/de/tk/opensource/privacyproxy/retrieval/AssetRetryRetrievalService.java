/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.retrieval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.tk.opensource.privacyproxy.util.PDFCorruptedException;
import de.tk.opensource.privacyproxy.util.PDFHelper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import de.tk.opensource.privacyproxy.config.RetrievalEndpoint;
import de.tk.opensource.privacyproxy.util.ProxyHelper;

@Service
public class AssetRetryRetrievalService {

	/**
	 * Configure where the files will be stored on the file system.
	 */
	@Value("${assets.fileLocation}")
	private String location;

	private final ProxyHelper proxyHelper;

	public AssetRetryRetrievalService(ProxyHelper proxyHelper) {
		this.proxyHelper = proxyHelper;
	}

	@Retryable(
		value = { IOException.class, IllegalStateException.class },
		backoff = @Backoff(delay = 3000),
		maxAttempts = 4
	)
	void retrieveAsset(String provider, RetrievalEndpoint endpoint) throws IOException, PDFCorruptedException {
		final URL url = new URL(endpoint.getRemoteUrlWithCacheBuster());
		final URLConnection connection = url.openConnection(proxyHelper.selectProxy(url));
		connection.setRequestProperty("User-Agent", "3rd Party Privacy Proxy");

		// get content-length or -1 if header not present
		final long originalFileSize = connection.getContentLengthLong();
		if (endpoint.getFilename().endsWith(".zip")) {
			retrieveZip(provider, endpoint, connection);
		} else {

			if (endpoint.getFilename().endsWith(".pdf") &&
					!PDFHelper.isPdf(IOUtils.toByteArray(connection.getInputStream()))) {
				throw new PDFCorruptedException(String.format("The requested resource %s wasn't a valid pdf file. " +
						"Maybe the endpoint has an error?", endpoint.getRemoteUrl()));
			}
			// transfer files without loading it to the application memory - performance boost
			retrieveFileByChannel(provider, endpoint, connection, originalFileSize);
		}
	}

	void retrieveZip(
		final String			provider,
		final RetrievalEndpoint endpoint,
		final URLConnection		connection
	) throws IOException
	{
		try(final ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream())) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				final long originalEntrySize = zipEntry.getSize();
				final Path source = Paths.get(getPath(provider, zipEntry.getName() + ".tmp"));
				final long transferredSize =
					Files.copy(zipInputStream, source, StandardCopyOption.REPLACE_EXISTING);

				if (originalEntrySize != transferredSize) {
					throw new IllegalStateException(
						String.format(
							"Copied file and original zip entry '%s' are not the same size. End of file error detected!",
							endpoint.getRemoteUrl()
						)
					);
				}

				// Rename
				Files.move(
					source,
					source.resolveSibling(
						zipEntry.getName().startsWith("/") ? zipEntry.getName().substring(1)
						: zipEntry.getName()
					),
					StandardCopyOption.REPLACE_EXISTING
				);
			}
		}
	}

	void retrieveFileByChannel(
		final String			provider,
		final RetrievalEndpoint endpoint,
		final URLConnection		connection,
		final long				originalFileSize
	) throws IOException
	{
		try(
			final ReadableByteChannel readChannel = Channels.newChannel(connection.getInputStream())
		) {
			final String file = getPath(provider, endpoint.getFilename() + ".tmp");
			try(final FileOutputStream fileOS = new FileOutputStream(file)) {
				final FileChannel writeChannel = fileOS.getChannel();
				long transferredSize = writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);

				if (originalFileSize != -1 && originalFileSize != transferredSize) {
					throw new IllegalStateException(
						String.format(
							"Transferred file and file from url '%s' are not the same size. End of file error detected!",
							endpoint.getRemoteUrl()
						)
					);
				}
			}

			// Rename
			Path sourcePath = Paths.get(file);
			Files.move(
				sourcePath,
				sourcePath.resolveSibling(
					endpoint.getFilename().startsWith("/") ? endpoint.getFilename().substring(1)
					: endpoint.getFilename()
				),
				StandardCopyOption.REPLACE_EXISTING
			);
		}
	}

	private String getPath(String provider, String filename) {
		return location + File.separator + provider + File.separator + filename;
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
