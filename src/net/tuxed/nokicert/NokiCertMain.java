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
import java.io.PrintStream;

import net.tuxed.gjokii.GjokiiException;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class NokiCertMain {

	private static final int NO_MODE = -1;
	private static final int CERT_INFO = 7;
	private static final int LIST_CERT = 0;
	private static final int INST_CERT = 1;
	private static final int IDENTIFY = 5;
	private static final int REBOOT = 6;

	private static PrintStream ps = null;

	public static void main(final String args[]) {
		ps = new PrintStream(System.out);

		int channelNumber = -1;
		String deviceAddress = null;
		String phoneFilePathName = null;

		int mode = -1;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--device") || args[i].equals("-d")) {
				deviceAddress = args[++i];
			}

			if (args[i].equals("--channel") || args[i].equals("-c")) {
				try {
					channelNumber = Integer.parseInt(args[++i]);
				} catch (final NumberFormatException e) {
					/*
					 * the channelNumber will remain -1, we'll find this out
					 * later anyway
					 */
				}
			}

			if (args[i].equals("--cert-info") || args[i].equals("-C")) {
				mode = CERT_INFO;
				phoneFilePathName = args[++i];
			}

			if (args[i].equals("--list-certs") || args[i].equals("-l")) {
				mode = LIST_CERT;
			}

			if (args[i].equals("--install-cert") || args[i].equals("-I")) {
				mode = INST_CERT;
				phoneFilePathName = args[++i];
			}

			if (args[i].equals("--identify") || args[i].equals("-i")) {
				mode = IDENTIFY;
			}

			if (args[i].equals("--reboot") || args[i].equals("-r")) {
				mode = REBOOT;
			}

			if (args[i].equals("--help") || args[i].equals("-h")) {
				showHelp();
				System.exit(0);
			}
		}
		if (mode == NO_MODE) {
			System.err.println("(E) no operation specified, see --help:\n");
			System.exit(1);
		}
		if (deviceAddress == null || channelNumber == -1) {
			System.err.println("(E) no device and/or channel specified, see --help:\n");
			System.exit(1);
		}
		if (mode == INST_CERT && phoneFilePathName.length() == 0) {
			System.err.println("(E) no certificate specified, see --help:\n");
			System.exit(1);
		}
		if (mode == CERT_INFO && phoneFilePathName.length() == 0) {
			System.err.println("(E) no certificate specified, see --help:\n");
			System.exit(1);
		}
		try {
			final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
			final NokiCert n = new NokiCert(device, false);
			switch (mode) {
				case IDENTIFY:
					ps.println("(I) Phone Information:\n\t" + n.getInfo() + "\n\t" + n.getIMEI());
					break;
				case REBOOT:
					ps.println("(I) Rebooting Phone...");
					n.reboot();
					break;
				case LIST_CERT:
					ps.println("(I) List Certificates...");
					ps.println(n.listCertificates());
					break;
				case INST_CERT:
					ps.println("(I) Installing Certificate...");
					n.installCertificate(phoneFilePathName, NokiCertUtils.APPS_SIGNING);
					break;
				case CERT_INFO:
					ps.println("(I) Showing Certificate Info...");
					final CertParser cp = new CertParser(new File(phoneFilePathName));
					ps.println(cp);
					break;
			}
			n.close();
		} catch (final GjokiiException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void showHelp() {
		String output = "NokiCert Help\n-----------\n";
		output += "Basic:\n";
		output += "  -d, --device <hwaddr>      Specify the Bluetooth device (e.g.: 001122334455)\n";
		output += "  -c, --channel <channel>    Specify the Bluetooth channel (e.g.: 15)\n";
		output += "Operations:\n";
		output += "  -i, --identify             Print phone identification\n";
		output += "  -r, --reboot               Reboot the phone\n";
		output += "  -C, --cert-info <cert>     Show information about certificate\n";
		output += "  -l, --list-cert            List the certificates installed on the phone\n";
		output += "  -I, --install-cert <cert>  Install an X.509 certificate on the phone\n";
		output += "  -v, --verbose              Increase verbosity\n";
		output += "  -h, --help                 Show this help message\n";
		ps.println(output);
	}

}
