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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.security.auth.x500.X500Principal;

import net.tuxed.gjokii.GjokiiException;
import net.tuxed.misc.Utils;

/**
 * This class parses a (DER encoded) X.509 certificate and has the ability to
 * construct a certificate list entry for use on Nokia phones.
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 */
public class CertParser {
	private final static byte[] APPS_SIGNING_BYTES = { (byte) 0x06, (byte) 0x08, (byte) 0x2b, (byte) 0x06, (byte) 0x01,
			(byte) 0x05, (byte) 0x05, (byte) 0x07, (byte) 0x03, (byte) 0x03 };
	private final static byte[] CROSS_CERTIFICATION_BYTES = { (byte) 0x06, (byte) 0x0a, (byte) 0x2b, (byte) 0x06,
			(byte) 0x01, (byte) 0x04, (byte) 0x01, (byte) 0x5e, (byte) 0x01, (byte) 0x31, (byte) 0x04, (byte) 0x01 };
	private final static byte[] SERVER_AUTHENTIC_BYTES = { (byte) 0x06, (byte) 0x08, (byte) 0x2b, (byte) 0x06,
			(byte) 0x01, (byte) 0x05, (byte) 0x05, (byte) 0x07, (byte) 0x03, (byte) 0x01 };

	private X509Certificate cert;
	private String subjectCountryCode;
	private String subjectOrganization;
	private String subjectDistinguishedName;
	private String subjectLocality;
	private String subjectCommonName;
	private String subjectState;
	private String subjectOrgUnit;
	private String issuerCountryCode;
	private String issuerOrganization;
	private String issuerCommonName;
	private String issuerDistinguishedName;

	/**
	 * Constructs the X.509 certificate object from byte array.
	 * 
	 * @param data the byte array containing the DER encoded X.509 certificate
	 */
	public CertParser(final byte[] data) throws GjokiiException {
		final InputStream inStream = new ByteArrayInputStream(data);
		parseCert(inStream);
	}

	/**
	 * Constructs the X.509 certificate object from file.
	 * 
	 * @param f the file containing the DER encoded X.509 certificate
	 */
	public CertParser(final File f) throws GjokiiException {
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(f);
			parseCert(inStream);
		} catch (final FileNotFoundException e) {
			throw new GjokiiException("unable to find certificate: " + e.getMessage());
		}
	}

	/**
	 * Get a certificate directory meta file entry for this certificate
	 * 
	 * @param littleEndian The CDF uses little endian coding for the size of the
	 *            entries
	 * @param certUsage the usage flags for this certificate (APPS_SIGNING,
	 *            CROSS_CERTIFICATION, SERVER_AUTHENTIC). Use the OR operator
	 *            for specifying more than one.
	 * @return the CDF entry
	 * @throws GjokiiException
	 */
	public byte[] getCDFEntry(final boolean littleEndian, final int certUsage) throws GjokiiException {
		byte[] output;
		final byte[] header = new byte[] { 0x01, 0x41, 0x02, 0x10 };
		final byte[] fields = new byte[] { 0x14, 0x00, 0x14, 0x14 };
		output = Utils.appendToByteArray(header, fields);
		output = Utils.appendToByteArray(output, getFingerprint());
		output = Utils.appendToByteArray(output, getModulusHash());
		output = Utils.appendToByteArray(output, new byte[20]);
		output = Utils.appendToByteArray(output, getSubjectHash());
		output = Utils.appendToByteArray(output, getIssuerHash());
		output = Utils.appendToByteArray(output, new byte[] { (byte) ((byte) getSubjectCommonName().length() + 1) });
		output = Utils.appendToByteArray(output, getSubjectCommonName().getBytes());
		output = Utils.appendToByteArray(output, new byte[2]); /* separator */

		byte[] keyUsage = new byte[1]; /* first byte contains length */
		if ((certUsage & NokiCertUtils.APPS_SIGNING) == NokiCertUtils.APPS_SIGNING) {
			keyUsage = Utils.appendToByteArray(keyUsage, APPS_SIGNING_BYTES);
		}
		if ((certUsage & NokiCertUtils.CROSS_CERTIFICATION) == NokiCertUtils.CROSS_CERTIFICATION) {
			keyUsage = Utils.appendToByteArray(keyUsage, CROSS_CERTIFICATION_BYTES);
		}
		if ((certUsage & NokiCertUtils.SERVER_AUTHENTIC) == NokiCertUtils.SERVER_AUTHENTIC) {
			keyUsage = Utils.appendToByteArray(keyUsage, SERVER_AUTHENTIC_BYTES);
		}
		keyUsage[0] = (byte) (keyUsage.length - 1); /* set length */

		output = Utils.appendToByteArray(output, keyUsage);

		/* now make the total length a divisor of 4 */
		int padding = 4 - output.length % 4;

		/* for some reason we need extra space in some situations?! */
		if (padding != 4) {
			padding += 4;
		}

		output = Utils.appendToByteArray(output, new byte[padding]); /* padding */

		/* prepend with total length, which is 4 bytes, we add that as well */
		final short outputLength = (short) (output.length + 4);
		byte[] sizeBytes = null;

		if (littleEndian) {
			// if (Config.hasLittleEndianCDFSize(platform)) {
			/* the two? size bytes are little endian */
			sizeBytes = Utils.shortToByteArrayLE(outputLength);
		} else {
			/* the two? size bytes are big endian */
			sizeBytes = Utils.shortToByteArray(outputLength);
		}
		sizeBytes = Utils.appendToByteArray(sizeBytes, new byte[2]);
		output = Utils.appendToByteArray(sizeBytes, output);
		return output;
	}

	/**
	 * Get the SHA1 hash of the certificate
	 * 
	 * @return the SHA1 hash of the certificate
	 */
	public byte[] getFingerprint() {
		byte[] data = null;
		MessageDigest hash = null;
		try {
			data = cert.getEncoded();
			hash = MessageDigest.getInstance("SHA1");
		} catch (final CertificateEncodingException e) {
		} catch (final NoSuchAlgorithmException e) {
		}
		hash.update(data);
		return hash.digest();
	}

	public String getIssuerCommonName() {
		return issuerCommonName;
	}

	/**
	 * Get the country code of the issuer
	 * 
	 * @return the country code of the issuer
	 */
	public String getIssuerCountryCode() {
		return issuerCountryCode;
	}

	/**
	 * Get the distinguished name of the issuer
	 * 
	 * @return the distinguished name of the issuer
	 */
	public String getIssuerDN() {
		return issuerDistinguishedName;
	}

	/**
	 * Get the SHA1 hash of the certificate issuer
	 * 
	 * @return the SHA1 hash of the certificate issuer
	 */
	public byte[] getIssuerHash() {
		byte[] data = null;
		MessageDigest hash = null;
		try {
			data = cert.getIssuerX500Principal().getEncoded();
			hash = MessageDigest.getInstance("SHA1");
		} catch (final NoSuchAlgorithmException e) {
		}
		hash.update(data);
		return hash.digest();
	}

	/**
	 * Get the organization of the issuer
	 * 
	 * @return the organization of the issuer
	 */
	public String getIssuerOrganization() {
		return issuerOrganization;
	}

	/**
	 * Get the SHA1 hash of the certificate modulus
	 * 
	 * @return the SHA1 hash
	 */
	public byte[] getModulusHash() {
		final RSAPublicKey k = (RSAPublicKey) cert.getPublicKey();
		final BigInteger m = k.getModulus();
		MessageDigest hash = null;
		try {
			hash = MessageDigest.getInstance("SHA1");
		} catch (final NoSuchAlgorithmException e) {
		}
		/*
		 * the modulus always seems to start with a 0x00 which we don't want
		 * when computing the hash.
		 */
		hash.update(m.toByteArray(), 1, m.toByteArray().length - 1);
		return hash.digest();
	}

	/**
	 * Returns the size of the public modulus in bits
	 * 
	 * @return the bit size of the public modulus
	 */
	public int getModulusSize() {
		final RSAPublicKey k = (RSAPublicKey) cert.getPublicKey();
		final BigInteger m = k.getModulus();
		return m.bitLength();
	}

	/**
	 * Nokia way of encoding the distinguished name of the subject required for
	 * the JMR entries.
	 * 
	 * It consists of only the fields C,ST,L,O,OU,CN (in that order) and only
	 * these. If a field does not exist in a certificate it is omitted. This can
	 * result in an empty DN.
	 * 
	 * @return the Nokia way of encoding a distinguished name of the subject
	 */
	public String getNokiaSubjectDN() {
		String output = "";
		if (subjectCountryCode != null) {
			output += "C=" + subjectCountryCode + ";";
		}
		if (subjectState != null) {
			output += "ST=" + subjectState + ";";
		}
		if (subjectLocality != null) {
			output += "L=" + subjectLocality + ";";
		}
		if (subjectOrganization != null) {
			output += "O=" + subjectOrganization + ";";
		}
		if (subjectOrgUnit != null) {
			output += "OU=" + subjectOrgUnit + ";";
		}
		if (subjectCommonName != null) {
			output += "CN=" + subjectCommonName;
		}

		/*
		 * if not all fields exist it is possible that the output ends with a
		 * semicolon, get rid of it here
		 */
		if (output.length() != 0 && output.endsWith(";")) {
			output.substring(0, output.length() - 1);
		}
		return output;
	}

	/**
	 * Get the common name (CN) of the subject
	 * 
	 * @return the common name
	 */
	public String getSubjectCommonName() {
		return subjectCommonName;
	}

	/**
	 * Get the country code of the subject
	 * 
	 * @return the country code of the subject
	 */
	public String getSubjectCountryCode() {
		return subjectCountryCode;
	}

	/**
	 * Get the distinguished name of the subject
	 * 
	 * @return the distinguished name of the subject
	 */
	public String getSubjectDN() {
		return subjectDistinguishedName;
	}

	/**
	 * Get the SHA1 hash of the certificate subject
	 * 
	 * @return the SHA1 hash of the certificate subject
	 */
	public byte[] getSubjectHash() {
		byte[] data = null;
		MessageDigest hash = null;
		try {
			data = cert.getSubjectX500Principal().getEncoded();
			hash = MessageDigest.getInstance("SHA1");
		} catch (final NoSuchAlgorithmException e) {
		}
		hash.update(data);
		return hash.digest();
	}

	/**
	 * Get the organization of the subject
	 * 
	 * @return the organization of the subject
	 */
	public String getSubjectOrganization() {
		return subjectOrganization;
	}

	@Override
	public String toString() {
		String output = "";
		output += "Issuer: " + getIssuerCommonName() + "\n";
		output += "Subject: " + getSubjectCommonName() + "\n";
		output += "Fingerprint: " + Utils.byteArrayToString(getFingerprint()) + "\n";
		return output;
	}

	/**
	 * Parse the certificate contained by the InputStream
	 * 
	 * @param is the certificate InputStream
	 * @throws CertificateException unable to parse the certificate stream
	 * @throws IOException unable to close the stream
	 */
	private void parseCert(final InputStream is) throws GjokiiException {
		try {
			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
			cert = (X509Certificate) cf.generateCertificate(is);
			Utils.closeSliently(is);
		} catch (final CertificateException e) {
			throw new GjokiiException(GjokiiException.INVALID_CERT_FILE, "unable to parse certificate: "
					+ e.getMessage());
		}
		final X500Principal sxp = cert.getSubjectX500Principal();
		subjectDistinguishedName = sxp.getName("RFC1779");
		final String[] Sdns = subjectDistinguishedName.split(",");
		for (final String sdn : Sdns) {
			final String s = sdn.trim();
			if (s.startsWith("C=")) {
				subjectCountryCode = s.substring(2);
			} else if (s.startsWith("O=")) {
				subjectOrganization = s.substring(2);
			} else if (s.startsWith("L=")) {
				subjectLocality = s.substring(2);
			} else if (s.startsWith("ST=")) {
				subjectState = s.substring(3);
			} else if (s.startsWith("OU=")) {
				subjectOrgUnit = s.substring(3);
			} else if (s.startsWith("CN=")) {
				subjectCommonName = s.substring(3);
			}
		}

		final X500Principal ixp = cert.getIssuerX500Principal();
		issuerDistinguishedName = ixp.getName();
		final String[] Idns = issuerDistinguishedName.split(",");
		for (final String idn : Idns) {
			final String s = idn.trim();
			if (s.startsWith("C=")) {
				issuerCountryCode = s.substring(2);
			} else if (s.startsWith("O=")) {
				issuerOrganization = s.substring(2);
			} else if (s.startsWith("CN=")) {
				issuerCommonName = s.substring(3);
			}
		}
	}
}