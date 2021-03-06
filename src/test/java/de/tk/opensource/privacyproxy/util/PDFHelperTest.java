/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PDFHelperTest {
	@Test
	public void testPdfWithBomMarker() throws IOException {
		ClassPathResource classPathResource =
			new ClassPathResource("testPdfs/pdfWithBomMarker.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertTrue(PDFHelper.isPdf(bytes));
	}

	@Test
	public void testStandardPdf() throws IOException {
		ClassPathResource classPathResource = new ClassPathResource("testPdfs/testPdf.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertTrue(PDFHelper.isPdf(bytes));
	}

	@Test
	public void testCorruptPdf() throws IOException {
		ClassPathResource classPathResource =
			new ClassPathResource("testPdfs/shellBinaryAsPdf.pdf");
		byte[] bytes = IOUtils.toByteArray(classPathResource.getInputStream());
		assertFalse(PDFHelper.isPdf(bytes));
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
