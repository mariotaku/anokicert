/*
 *  This file is part of Gjokii.
 *
 *  Gjokii is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Gjokii is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Gjokii.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.tuxed.misc;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * General purpose class with all kinds of useful methods
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class Utils {

	private Utils() {
	}

	/**
	 * Append a byte array to another byte array
	 * 
	 * @param first the byte array to append to
	 * @param second the byte array to append
	 * @return the appended array
	 */
	public static byte[] appendToByteArray(final byte[] first, final byte[] second) {
		final int secondLength = second != null ? second.length : 0;
		return appendToByteArray(first, second, 0, secondLength);
	}

	/**
	 * Append a byte array to another byte array specifying which part of the
	 * second byte array should be appended to the first
	 * 
	 * @param first the byte array to append to
	 * @param second the byte array to append
	 * @param offset offset in second array to start appending from
	 * @param length number of bytes to append from second to first array
	 * @return the appended array
	 */
	public static byte[] appendToByteArray(final byte[] first, final byte[] second, final int offset, final int length) {
		if (second == null || second.length == 0) // if (first == null)
			// return new byte[0];
			return first;
		final int firstLength = first != null ? first.length : 0;

		if (length < 0 || offset < 0 || second.length < length + offset) throw new ArrayIndexOutOfBoundsException();
		final byte[] result = new byte[firstLength + length];
		if (firstLength > 0) {
			System.arraycopy(first, 0, result, 0, firstLength);
		}
		System.arraycopy(second, offset, result, firstLength, length);
		return result;
	}

	/**
	 * Convert a (part of) a byte array to integer
	 * 
	 * @param data the byte array
	 * @param offset the offset in the byte array to start from
	 * @return the integer value represented by the byte array
	 */
	public static int byteArrayToInt(final byte[] data, final int offset) {
		return data[offset] << 24 & 0xff000000 | data[offset + 1] << 16 & 0x00ff0000 | data[offset + 2] << 8
				& 0x0000ff00 | data[offset + 3] & 0x000000ff;
	}

	/**
	 * Convert a (part of) a byte array to integer assuming the byte array is
	 * Little Endian encoded
	 * 
	 * @param data the byte array
	 * @param offset the offset in the byte array to start from
	 * @return the integer value represented by the byte array
	 */
	public static int byteArrayToIntLE(final byte[] data, final int offset) {
		return data[offset] & 0x000000ff | data[offset + 1] << 8 & 0x0000ff00 | data[offset + 2] << 16 & 0x00ff0000
				| data[offset + 3] << 24 & 0xff000000;
	}

	/**
	 * Convert a (part of) a byte array to short
	 * 
	 * @param data the byte array
	 * @param offset the offset in the byte array to start from
	 * @return the short value represented by the byte array
	 */
	public static short byteArrayToShort(final byte[] data, final int offset) {
		return (short) (data[offset] << 8 | data[offset + 1] & 0xff);
	}

	/**
	 * Convert a (part of) a byte array to short assuming the byte array is
	 * Little Endian encoded
	 * 
	 * @param data the byte array
	 * @param offset the offset in the byte array to start from
	 * @return the short value represented by the byte array
	 */
	public static short byteArrayToShortLE(final byte[] data, final int offset) {
		return (short) (data[offset + 1] << 8 | data[offset] & 0xff);
	}

	/**
	 * Converts a byte array to readable string
	 * 
	 * @param a array to print
	 * @return readable byte array string
	 */
	public static String byteArrayToString(final byte[] a) {
		return byteArrayToString(a, 0, a != null ? a.length : 0);
	}

	public static String byteArrayToString(final byte[] a, final int offset, final int length) {
		if (a == null) return "[null]";
		if (a.length == 0) return "[empty]";
		if (offset < 0 || length < 0 || length + offset > a.length) throw new IndexOutOfBoundsException();
		String result = "";
		for (int i = offset; i < offset + length; i++) {
			result += byteToString(a[i]);
		}
		return result;
	}

	/**
	 * Decode a byte array which contains UTF-16 bytes to string.
	 * 
	 * @param data the byte array containing the UTF-16 bytes NOT terminated by
	 *            0x00 0x00
	 * @param offset the offset in the array
	 * @param length the length of the array
	 * @return the decoded bytes
	 */
	public static String bytesToString(final byte[] data, final int offset, final int length) {
		if (data == null) return null;
		String s = null;
		try {
			s = new String(data, offset, length, "UTF-16");
		} catch (final UnsupportedEncodingException e) {
		}
		return s;
	}

	/**
	 * Decode a byte array which contains UTF-16 Little Endian bytes to string.
	 * We assume the byte array terminates with 0x00 0x00.
	 * 
	 * @param data the byte array containing the UTF-16 LE bytes terminated by
	 *            0x00 0x00
	 * @return the decoded string
	 */
	public static String bytesToStringLE(final byte[] data) {
		if (data != null && data.length >= 2)
			return bytesToStringLE(data, 0, data.length - 2);
		else
			return "";
	}

	/**
	 * Decode a byte array which contains UTF-16 Little Endian bytes to string.
	 * 
	 * @param data the byte array containing the UTF-16 LE bytes NOT terminated
	 *            by 0x00 0x00
	 * @param offset the offset in the array
	 * @param length the length of the array
	 * @return the decoded bytes
	 */
	public static String bytesToStringLE(final byte[] data, final int offset, final int length) {
		if (data == null) return null;
		String s = null;
		try {
			s = new String(data, offset, length, "UTF-16LE");
		} catch (final UnsupportedEncodingException e) {
		}
		return s;
	}

	/**
	 * Convert a byte to a human readable representation
	 * 
	 * @param b the byte
	 * @return the human readable representation
	 */
	public static String byteToString(final int b) {
		String s = Integer.toHexString(b);
		if (s.length() == 1) {
			s = "0" + s;
		} else {
			s = s.substring(s.length() - 2);
		}
		return s;
	}

	public static void closeSliently(final Closeable c) {
		if (c == null) return;
		try {
			c.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the SHA1 hash of the specified file
	 * 
	 * @param f the file
	 * @return the SHA1 hash
	 * @throws IOException
	 */
	public static byte[] getFileHash(final File f) throws IOException {
		final FileInputStream fis = new FileInputStream(f);
		try {
			final byte[] data = new byte[1024];
			int bytesRead;
			MessageDigest hash = null;
			try {
				hash = MessageDigest.getInstance("SHA1");
			} catch (final NoSuchAlgorithmException e) {
				/* we assume SHA1 always exists... */
			}
			while ((bytesRead = fis.read(data)) > 0) {
				hash.update(data, 0, bytesRead);
			}
			return hash.digest();
		} finally {
			closeSliently(fis);
		}
	}

	/**
	 * Look through data (hay stack) looking for a pattern that results in a
	 * certain SHA1 hash (needle).
	 * 
	 * This is a brute force search. For every possible (continuous) sub array
	 * of hayStack we calculate the SHA1 hash and match it with the hash we were
	 * looking for (needle).
	 * 
	 * @param hayStack the data to analyze
	 * @param needle the hash being looked for
	 * @return the {offset, length} of some sub array in hay stack resulting in
	 *         the hash specified by needle, or null if no match was found
	 */
	public static int[] hashCalculator(final byte[] hayStack, final byte[] needle) {
		MessageDigest hash = null;
		try {
			hash = MessageDigest.getInstance("SHA1");
		} catch (final NoSuchAlgorithmException e) {
		}
		/* for every possible size */
		for (int i = 0; i <= hayStack.length; i++) {
			/* every possible offset */
			for (int j = 0; j <= hayStack.length - i; j++) {
				hash.update(hayStack, j, i);
				final byte[] dig = hash.digest();
				if (Arrays.equals(dig, needle)) return new int[] { j, i };
			}
		}
		return null;
	}

	public static String hexDump(final byte[] a) {
		return hexDump(a, 0, a != null ? a.length : 0);
	}

	public static String hexDump(final byte[] a, final int offset, final int length) {
		final int WIDTH = 16;
		if (a == null) return "[null]";
		if (a.length == 0) return "[empty]";
		String result = "";
		if (offset < 0 || length < 0 || length + offset > a.length) throw new IndexOutOfBoundsException();
		int rows = length / WIDTH;
		final int lastRow = length % WIDTH;
		if (lastRow != 0) {
			rows++;
		}

		for (int i = 0; i < rows; i++) {
			final int m = lastRow != 0 && i == rows - 1 ? lastRow : WIDTH;

			for (int j = 0; j < m; j++) {
				result += byteToString(a[offset + i * WIDTH + j]) + " ";
			}
			for (int z = 0; z < WIDTH - m; z++) {
				result += "   ";
			}
			result += " |";
			for (int j = 0; j < m; j++) {
				if (a[offset + i * WIDTH + j] >= 0x20 && a[offset + i * WIDTH + j] < 0x7f) {
					result += (char) a[offset + i * WIDTH + j];
				} else {
					result += ".";
				}
			}
			result += "|\n";
		}
		return result;
	}

	/**
	 * Convert an integer to byte array
	 * 
	 * @param v the integer
	 * @return the byte array representing the integer
	 */
	public static byte[] intToByteArray(final int v) {
		return new byte[] { (byte) ((v & 0xFF000000) >> 24), (byte) ((v & 0x00FF0000) >> 16),
				(byte) ((v & 0x0000FF00) >> 8), (byte) (v & 0x000000FF) };
	}

	/**
	 * Convert an integer to byte array using Little Endian representation
	 * 
	 * @param v the integer
	 * @return the byte array representing the integer
	 */
	public static byte[] intToByteArrayLE(final int v) {
		return new byte[] { (byte) (v & 0x000000FF), (byte) ((v & 0x0000FF00) >> 8), (byte) ((v & 0x00FF0000) >> 16),
				(byte) ((v & 0xFF000000) >> 24) };
	}

	/**
	 * Convert a short to byte array
	 * 
	 * @param s the short
	 * @return the byte array representing the short
	 */
	public static byte[] shortToByteArray(final short s) {
		return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
	}

	/**
	 * Convert a short to byte array using Little Endian representation
	 * 
	 * @param s the short
	 * @return the byte array representing the short
	 */
	public static byte[] shortToByteArrayLE(final short s) {
		return new byte[] { (byte) (s & 0x00FF), (byte) ((s & 0xFF00) >> 8) };
	}

	/**
	 * Convert a short to string and pad it so it is always at least of length
	 * two.
	 * 
	 * @param s the short to convert
	 * @return the (padded) string
	 */
	public static String shortToTwoDigitString(final short s) {
		return s < 10 ? "0" + s : "" + s;
	}

	/**
	 * Convert a string to a UTF-16 Little Endian byte array terminated with
	 * 0x00 0x00
	 * 
	 * @param s the string to convert
	 * @param terminator whether or not to add String terminator bytes
	 * @return the UTF-16 LE byte array terminated with 0x00 0x00
	 */
	public static byte[] stringToBytes(final String s, final boolean terminator) {
		if (s == null && terminator)
			return new byte[] { 0x00, 0x00 };
		else if (s == null) return null;

		/*
		 * we may want to reserve two bytes at the end to contain 0x00 0x00 to
		 * indicate end of string (depending on terminator bool),
		 * String.getBytes does not do this
		 */
		final byte[] stringBytes = new byte[s.length() * 2 + (terminator ? 2 : 0)];
		try {
			final byte[] bA = s.getBytes("UTF-16LE");
			System.arraycopy(bA, 0, stringBytes, 0, bA.length);
		} catch (final UnsupportedEncodingException e) {
		}
		return stringBytes;
	}

	/**
	 * Return a specific part of a byte array starting from <code>offset</code>
	 * with <code>length</code>
	 * 
	 * @param array the byte array
	 * @param offset the offset in the array from where to start in bytes
	 * @param length the number of bytes to get
	 * @return the sub byte array
	 */
	public static byte[] subByteArray(final byte[] array, final int offset, final int length) {
		return appendToByteArray(null, array, offset, length);
	}
}
