/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class PDFHelper {

	private PDFHelper() {
	}

	private static final int[] UTF8_BYTE_ORDER_MARK = { 239, 187, 191 };

	/**
	 * Check if a byte array is an PDF file. PDF files starts with magic numbers '%PDF-' and ends
	 * with '%%EOF'. A File that does not contains PDF magic numbers is therefore not a PDF file.
	 *
	 * @param   data  file byte array
	 *
	 * @return  true if byte array looks like a pdf, otherwise false
	 */
	public static boolean isPdf(byte[] data) {
		data = removeBomMarker(data);
		if (
			data != null
			&& data.length > 4
			&& data[0] == 0x25 // %
			&& data[1] == 0x50 // P
			&& data[2] == 0x44 // D
			&& data[3] == 0x46 // F
			&& data[4] == 0x2D
		) {
			int count = 0;
			int offset = data.length - 8; // check last 8 bytes for %%EOF with optional white-space
			while (offset < data.length) {
				if (count == 0 && data[offset] == 0x25) {
					count++; // %
				}
				if (count == 1 && data[offset] == 0x25) {
					count++; // %
				}
				if (count == 2 && data[offset] == 0x45) {
					count++; // E
				}
				if (count == 3 && data[offset] == 0x4F) {
					count++; // O
				}
				if (count == 4 && data[offset] == 0x46) {
					count++; // F
				}
				offset++;
			}
			return count == 5;
		}
		return false;
	}

	/**
	 * Check if a bom marker is at the beginning of the file. If there is a bom marker, the bom
	 * marker is removed from the byte array.
	 *
	 * @param   data  file byte array
	 *
	 * @return  file byte array free of any bom markers
	 */
	private static byte[] removeBomMarker(byte[] data) {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
		int[] bomTestArr = new int[UTF8_BYTE_ORDER_MARK.length];

		for (int index = 0; index < UTF8_BYTE_ORDER_MARK.length; ++index) {
			bomTestArr[index] = byteArrayInputStream.read();
		}
		boolean isBomMarked = Arrays.equals(bomTestArr, UTF8_BYTE_ORDER_MARK);

		if (isBomMarked) {
			data = Arrays.copyOfRange(data, UTF8_BYTE_ORDER_MARK.length, data.length);
		}
		return data;
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/
