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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import net.tuxed.gjokii.GjokiiException;
import net.tuxed.misc.Utils;

/**
 * This class analyzes the certificate file list from the Nokia phones
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class CertListParser {

	private boolean littleEndian;
	private final File file;

	public CertListParser(final File file) {
		this.file = file;
	}

	public boolean hasLittleEndianSizeBytes() {
		return littleEndian;
	}

	public ArrayList<CertListItem> parse() throws GjokiiException {
		final ArrayList<CertListItem> list = new ArrayList<CertListItem>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			while (fis.available() > 4) {
				int bytesRead = 0;
				final byte[] lengthBytes = new byte[2];
				fis.read(lengthBytes);
				int size = Utils.byteArrayToShort(lengthBytes, 0) - 2;
				if (size >= file.length() || size < 0) {
					/* we seem to have a little endian length indicator */
					littleEndian = true;
					size = Utils.byteArrayToShortLE(lengthBytes, 0) - 2;
				}
				// System.out.println(Utils.byteArrayToString(lengthBytes));
				// System.out.println("Size of block: " + size);
				bytesRead += fis.skip(10);
				final byte[] fingerprint = new byte[20];
				bytesRead += fis.read(fingerprint);
				final byte[] hashOfModulus = new byte[20];
				bytesRead += fis.read(hashOfModulus);
				final byte[] unknownField = new byte[20];
				bytesRead += fis.read(unknownField);
				final byte[] hashOfSubject = new byte[20];
				bytesRead += fis.read(hashOfSubject);
				final byte[] hashOfIssuer = new byte[20];
				bytesRead += fis.read(hashOfIssuer);
				final int sizeOfFileName = fis.read();
				bytesRead++;
				final byte[] fileName = new byte[sizeOfFileName - 1];
				bytesRead += fis.read(fileName);
				// System.out.println(new String(fileName));
				bytesRead += fis.skip(2);
				int keyUsage = 0;
				final int keyUsageLength = fis.read();
				// System.out.println(keyUsageLength);
				bytesRead++;
				final byte[] keyUsageBytes = new byte[keyUsageLength];
				bytesRead += fis.read(keyUsageBytes);
				// System.out.println(Utils.byteArrayToString(keyUsageBytes));
				/* read all usages */
				int offset = 1;
				while (offset < keyUsageBytes.length) {
					final int curKeyUsageLength = keyUsageBytes[offset];
					if (curKeyUsageLength + offset > keyUsageBytes.length || curKeyUsageLength < 0) {
						/*
						 * something seems wrong in key usage byte array, skip
						 * it
						 */
						break;
					}
					final byte[] t = new byte[curKeyUsageLength];
					offset++;
					System.arraycopy(keyUsageBytes, offset, t, 0, curKeyUsageLength);
					keyUsage |= NokiCertUtils.keyUsageBytesToType(t);
					offset += curKeyUsageLength + 1;
				}
				// System.out.println("Skipping " + (size - bytesRead)
				// + " bytes...");
				fis.skip(size - bytesRead);
				list.add(new CertListItem(new String(fileName), fingerprint, hashOfModulus, unknownField,
						hashOfSubject, hashOfIssuer, keyUsage));
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			Utils.closeSliently(fis);
		}
		return list;
	}

	public static class CertListItem implements Serializable {
		private static final long serialVersionUID = 4925312087598482273L;
		public final String fileName;
		public final byte[] fingerprint, hashOfModulus, unknownField, hashOfSubject, hashOfIssuer;
		public final int keyUsage;

		CertListItem(final String fileName, final byte[] fingerprint, final byte[] hashOfModulus,
				final byte[] unknownField, final byte[] hashOfSubject, final byte[] hashOfIssuer, final int keyUsage) {
			this.fileName = fileName;
			this.fingerprint = fingerprint;
			this.hashOfModulus = hashOfModulus;
			this.unknownField = unknownField;
			this.hashOfSubject = hashOfSubject;
			this.hashOfIssuer = hashOfIssuer;
			this.keyUsage = keyUsage;
		}

		public String getFileName() {
			return fileName;
		}

		public byte[] getFingerprint() {
			return fingerprint;
		}

		public byte[] getHashOfIssuer() {
			return hashOfIssuer;
		}

		public byte[] getHashOfModulus() {
			return hashOfModulus;
		}

		public byte[] getHashOfSubject() {
			return hashOfSubject;
		}

		public int getKeyUsage() {
			return keyUsage;
		}

		public byte[] getUnknownField() {
			return unknownField;
		}

		@Override
		public String toString() {
			return "CertListItem [fileName=" + fileName + ", fingerprint=" + Utils.byteArrayToString(fingerprint)
					+ ", hashOfModulus=" + Utils.byteArrayToString(hashOfModulus) + ", unknownField="
					+ Utils.byteArrayToString(unknownField) + ", hashOfSubject="
					+ Utils.byteArrayToString(hashOfSubject) + ", hashOfIssuer="
					+ Utils.byteArrayToString(hashOfIssuer) + ", keyUsage=" + keyUsage + "]";
		}
	}

}
