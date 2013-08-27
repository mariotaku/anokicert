/*
 *  This file is part of NokiCert.
 *
 *  NokiCert is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  NokiCert is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with NokiCert.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.tuxed.nokicert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import net.tuxed.gjokii.GjokiiException;
import net.tuxed.misc.Utils;
import android.util.Base64;

/**
 * NokiCert Utils.
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class NokiCertUtils {
	public final static int APPS_SIGNING = 1;
	public final static int CROSS_CERTIFICATION = 2;
	public final static int SERVER_AUTHENTIC = 4;

	private NokiCertUtils() {
	}

	/**
	 * This method converts a PEM certificate to DER format, if the input is an
	 * actual PEM certificate. If it is not it just returns the file it was
	 * given as a parameter.
	 * 
	 * @param f PEM certificate file
	 * @return a pointer to a DER encoded file if the input was a PEM
	 *         certificate, or the input file in case it was not a PEM file.
	 * @throws GjokiiException
	 */
	public static File convertToDER(final File f) {
		InputStream is = null;
		BufferedReader br = null;
		try {
			is = new FileInputStream(f);
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			do {
				line = br.readLine();
				if (line == null) return f;
			} while (!line.matches("-----BEGIN CERTIFICATE-----"));
			/* next line(s) should contain certificate base64 encoded data */
			String base64cert = new String();
			boolean endOfLoop = false;
			do {
				line = br.readLine();
				endOfLoop = line.matches("-----END CERTIFICATE-----");
				if (!endOfLoop) {
					base64cert += line;
				}
			} while (!endOfLoop);
			/* convert Base64 encoded string to binary */
			final byte[] binaryCert = Base64.decode(base64cert, Base64.DEFAULT);
			final File derFile = File.createTempFile("DER", null);
			final FileOutputStream fos = new FileOutputStream(derFile);
			fos.write(binaryCert);
			fos.flush();
			fos.close();
			return derFile;
		} catch (final IOException e) {
			return f;
		} finally {
			Utils.closeSliently(br);
			Utils.closeSliently(is);
		}
	}

	/**
	 * Converts a specific key usage byte array from the S40 phone to a keyUsage
	 * type
	 * 
	 * @param keyUsage the key usage pattern
	 * @return the keyUsage type
	 */
	public static int keyUsageBytesToType(final byte[] keyUsage) {
		if (Arrays.equals(new byte[] { (byte) 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x03, 0x03 }, keyUsage))
			return APPS_SIGNING;
		else if (Arrays.equals(new byte[] { (byte) 0x2b, 0x06, 0x01, 0x04, 0x01, 0x5e, 0x01, 0x31, 0x04, 0x01 },
				keyUsage))
			return CROSS_CERTIFICATION;
		else if (Arrays.equals(new byte[] { (byte) 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x03, 0x01 }, keyUsage))
			return SERVER_AUTHENTIC;
		else
			return -1;
	}

}
