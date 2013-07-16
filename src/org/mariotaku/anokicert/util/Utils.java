package org.mariotaku.anokicert.util;

public final class Utils {
	/** Hexadecimal digits. */
	private static final char[] hc = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Converts a byte array into a corresponding string of hexadecimal digits.
	 * This is equivalent to hexEncode(b, 0, b.length).
	 * <P />
	 * 
	 * @param b byte array to be converted
	 * @return corresponding hexadecimal string
	 */
	public static String hexEncode(final byte[] b) {
		if (b == null)
			return "";
		else
			return hexEncode(b, 0, b.length);
	}

	/**
	 * Converts a subsequence of bytes in a byte array into a corresponding
	 * string of hexadecimal digits, each separated by a ":".
	 * 
	 * @param b byte array containing the bytes to be converted
	 * @param off starting offset of the byte subsequence inside b
	 * @param len number of bytes to be converted
	 * @return a string of corresponding hexadecimal digits or an error string
	 */
	public static String hexEncode(final byte[] b, final int off, final int len) {
		return new String(hexEncodeToChars(b, off, len));
	}

	/**
	 * Converts a subsequence of bytes in a byte array into a corresponding
	 * string of hexadecimal digits, each separated by a ":".
	 * 
	 * @param b byte array containing the bytes to be converted
	 * @param off starting offset of the byte subsequence inside b
	 * @param len number of bytes to be converted
	 * @return a string of corresponding hexadecimal digits or an error string
	 */
	public static char[] hexEncodeToChars(final byte[] b, final int off, final int len) {
		char[] r;
		int v;
		int i;
		int j;

		if (b == null || len == 0) return new char[0];

		if (off < 0 || len < 0) throw new ArrayIndexOutOfBoundsException();

		if (len == 1) {
			r = new char[len * 2];
		} else {
			r = new char[len * 3 - 1];
		}

		for (i = 0, j = 0;;) {
			v = b[off + i] & 0xff;
			r[j++] = hc[v >>> 4];
			r[j++] = hc[v & 0x0f];

			i++;
			if (i >= len) {
				break;
			}

			r[j++] = ':';
		}

		return r;
	}
}
