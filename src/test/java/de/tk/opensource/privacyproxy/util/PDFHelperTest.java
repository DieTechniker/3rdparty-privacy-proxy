package de.tk.opensource.privacyproxy.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFHelperTest {
	@Test
	void testPdfWithBomMarker() throws IOException {
		ClassPathResource classPathResource =
			new ClassPathResource("testPdfs/pdfWithBomMarker.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertTrue(PDFHelper.isPdf(bytes));
	}

	@Test
	void testStandardPdf() throws IOException {
		ClassPathResource classPathResource = new ClassPathResource("testPdfs/testPdf.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertTrue(PDFHelper.isPdf(bytes));
	}

	@Test
	void testCorruptPdf() throws IOException {
		ClassPathResource classPathResource =
			new ClassPathResource("testPdfs/shellBinaryAsPdf.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertFalse(PDFHelper.isPdf(bytes));
	}
}