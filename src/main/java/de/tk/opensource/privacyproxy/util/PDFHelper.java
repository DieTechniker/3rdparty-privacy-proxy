package de.tk.opensource.privacyproxy.util;

public class PDFHelper {

    /**
     * Check if a byte array is an PDF file.
     * PDF files starts with magic numbers '%PDF-' and ends with '%%EOF'.
     * A File that does not contains PDF magic numbers is therefore not a PDF file.
     * @param data file byte array
     * @return true if byte array looks like a pdf, otherwise false
     */
    public static boolean isPdf(byte[] data) {
        if (data != null && data.length > 4 &&
                data[0] == 0x25 && // %
                data[1] == 0x50 && // P
                data[2] == 0x44 && // D
                data[3] == 0x46 && // F
                data[4] == 0x2D) { // -
            int count = 0;
            int offset = data.length - 8; // check last 8 bytes for %%EOF with optional white-space
            while (offset < data.length) {
                if (count == 0 && data[offset] == 0x25) count++; // %
                if (count == 1 && data[offset] == 0x25) count++; // %
                if (count == 2 && data[offset] == 0x45) count++; // E
                if (count == 3 && data[offset] == 0x4F) count++; // O
                if (count == 4 && data[offset] == 0x46) count++; // F
                offset++;
            }
            return count == 5;
        }

        return false;
    }
}