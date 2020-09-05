package de.tk.opensource.privacyproxy.util;

public class PDFHelper {

    /**
     * Checks if a byte array is an PDF file.
     * PDF files starts witch magic numbers.
     * An File that does not contains PDF magic numbers is therefore not a PDF file.
     * @param data file byte array
     * @return isPDF = true, otherwise false
     */
    public static boolean isPdf(byte[] data) {
        if (data != null && data.length > 4 &&
                data[0] == 0x25 && // %
                data[1] == 0x50 && // P
                data[2] == 0x44 && // D
                data[3] == 0x46 && // F
                data[4] == 0x2D) { // -

            // version 1.3 file terminator
            if (data[5] == 0x31 && data[6] == 0x2E && data[7] == 0x33 &&
                    data[data.length - 7] == 0x25 && // %
                    data[data.length - 6] == 0x25 && // %
                    data[data.length - 5] == 0x45 && // E
                    data[data.length - 4] == 0x4F && // O
                    data[data.length - 3] == 0x46 && // F
                    data[data.length - 2] == 0x20 && // SPACE
                    data[data.length - 1] == 0x0A) { // EOL
                return true;
            }

            // version 1.3 file terminator
            // EOL
            return data[5] == 0x31 && data[6] == 0x2E && data[7] == 0x34 &&
                    data[data.length - 6] == 0x25 && // %
                    data[data.length - 5] == 0x25 && // %
                    data[data.length - 4] == 0x45 && // E
                    data[data.length - 3] == 0x4F && // O
                    data[data.length - 2] == 0x46 && // F
                    data[data.length - 1] == 0x0A;
        }
        return false;
    }
}
