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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.tuxed.gjokii.Gjokii;
import net.tuxed.gjokii.GjokiiException;
import net.tuxed.nokicert.CertListParser.CertListItem;
import android.bluetooth.BluetoothDevice;

/**
 * This class deals with installing and listing certificates on Nokia phones. It
 * uses the Gjokii library for file handling.
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class NokiCert extends Gjokii {
	private static final String CERT_DIR_FILE_PATH = "/predefhiddenfolder/certificates/auth/ext_info.sys";

	/**
	 * Construct the NokiCert object
	 * 
	 * @param gjokii the (open) Gjokii connection to the phone
	 * @param printStream the stream to write output to (can be System.out)
	 * @throws GjokiiException
	 */
	public NokiCert(final BluetoothDevice device, final boolean verbose) throws GjokiiException {
		super(device, verbose);
	}

	/**
	 * Retrieve the certificate list file (CDF) from the phone.
	 * 
	 * @return file handle to certificate list file (CDF)
	 * @throws GjokiiException
	 */
	public File getCertificateListFile() throws GjokiiException {
		log("(I) downloading CDF from the phone...");
		File f = null;
		/* get the current certificate directory file */
		try {
			f = File.createTempFile("CDF", null);
		} catch (final IOException e) {
			throw new GjokiiException("unable to create temporary file");
		}
		log("(I) using temporary file: " + f.getAbsolutePath());
		getFile(CERT_DIR_FILE_PATH, f);
		return f;
	}

	/**
	 * Install a X.509 certificate on the phone.
	 * 
	 * @param certFilePathName the full path name of the certificate file
	 * @param certUsage the certificate usage bits for this certificate
	 * @throws GjokiiException if an error occurs
	 */
	public void installCertificate(final String certFilePathName, final int certUsage) throws GjokiiException {
		final File f = getCertificateListFile();
		final File derFile = NokiCertUtils.convertToDER(new File(certFilePathName));
		getFile(CERT_DIR_FILE_PATH, f);
		final CertListParser c = new CertListParser(f);
		c.parse();
		String subjectCN;

		/* add the new certificate to the certificate directory file */
		try {
			final CertParser x = new CertParser(derFile);
			subjectCN = x.getSubjectCommonName();
			final byte[] certEntry = x.getCDFEntry(c.hasLittleEndianSizeBytes(), certUsage);
			final FileOutputStream fos = new FileOutputStream(f, true);
			fos.write(certEntry);
			fos.flush();
			fos.close();
		} catch (final FileNotFoundException e) {
			throw new GjokiiException("cannot find certificate directory file");
		} catch (final IOException e) {
			throw new GjokiiException(GjokiiException.INVALID_CERT_FILE, "not a cert file!");
		}
		/* upload the certificate */
		final String certPath = "/predefhiddenfolder/certificates/auth/" + subjectCN;

		log("(I) uploading certificate to the phone...");
		putFile(certPath, derFile);

		log("(I) uploading CDF to the phone...");
		/* upload the new certificate directory file (CDF) */
		putFile(CERT_DIR_FILE_PATH, f);
	}

	/**
	 * Retrieve a (formatted) list of installed certificates and their SHA-1
	 * hash.
	 * 
	 * @return the list
	 * @throws GjokiiException
	 */
	public ArrayList<CertListItem> listCertificates() throws GjokiiException {
		final CertListParser c = new CertListParser(getCertificateListFile());
		return c.parse();
	}

}
