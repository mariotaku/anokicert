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
package net.tuxed.gjokii;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import net.tuxed.misc.Utils;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Low level class to access Nokia S40 functionality.
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class Gjokii implements Closeable {

	private static final String COMMON_SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	/**
	 * The header for sending data over Bluetooth looks like this:
	 * 
	 * <pre>
	 * 0x19 0x00 0x10 (type [1]) (size [2]) (data [size])
	 * </pre>
	 * 
	 * The header for receiving data over Bluetooth looks like this:
	 * 
	 * <pre>
	 * 0x19 0x10 0x00 (type [1]) (size [2]) (data [size])
	 * </pre>
	 * 
	 * 0xff is replaced with the message type, 0xee 0xee is replaced with the
	 * total length of the message following the header
	 */
	private static final byte[] BT_HEADER = { (byte) 0x19, (byte) 0x00, (byte) 0x10, (byte) 0xff, (byte) 0xee,
			(byte) 0xee };

	private static final byte[] PHONE_INIT = { (byte) 0x04 };

	private static final byte[] PHONE_INFO = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x07, (byte) 0x01,
			(byte) 0x00 };

	private static final byte[] PHONE_IMEI = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x41 };

	private static final byte[] PHONE_RESET = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x80,
			(byte) 0x00 };

	/**
	 * The header for requesting a file list of a given directory.
	 * 
	 * 0xff is replaced with the number of bytes making up the directory path
	 */
	private static final byte[] FILE_LIST = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x68, (byte) 0x00,
			(byte) 0xff, (byte) 0x00 };

	private static final byte[] FILE_INFO = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x68, (byte) 0x00,
			(byte) 0x68, (byte) 0x00 };

	private static final byte[] GET_FILE_ID = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x72, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x68, (byte) 0x00 };

	/**
	 * The header for requesting a file from the phone.
	 * 
	 * 0xff 0xff is replaced with the file descriptor (fileDesc)
	 * 
	 * 0xee 0xee is replaced with the number of bytes to request for this block.
	 * The idea implemented by Gnokii is that blocks of size 256 (0x01 0x00) are
	 * requested until the last block which can be less than 256 bytes.
	 * 
	 * 0xdd 0xdd is replaced with the current block number starting from 0x00
	 * 0x00, at every request this is increased by 1. This would thus result in
	 * a maximum file size of 16MB. It should be investigated whether or not
	 * these fields can contain integer values instead of just shorts.
	 */
	private static final byte[] GET_FILE = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x5e, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0xdd, (byte) 0xdd,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xee,
			(byte) 0xee };

	/**
	 * Get a file ID on the phone for writing a file
	 * 
	 * 0xff 0xff contains the length of the file path
	 */
	private static final byte[] PUT_FILE_ID = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x72, (byte) 0x11,
			(byte) 0x00, (byte) 0xff, (byte) 0xff };

	/**
	 * Close an open file
	 * 
	 * 0xff 0xff contains the file Id
	 */
	private static final byte[] CLOSE_FILE = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x74, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff };
	/**
	 * Write a file to the phone
	 * 
	 * 0xff 0xff contains the fileId, 0xee 0xee contains the number of bytes to
	 * write to the phone with this command
	 */
	private static final byte[] PUT_FILE = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x58, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0xee,
			(byte) 0xee };

	/**
	 * Delete a file from the phone
	 * 
	 * 0xff is replaced with the number of bytes making up the directory path
	 */
	private static final byte[] DELETE_FILE = { (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x62, (byte) 0x00,
			(byte) 0xff };

	/**
	 * The block size used for getting and putting files
	 */
	private static final short BLOCK_SIZE = 256;
	private BluetoothSocket con;

	private final InputStream is;
	private final OutputStream os;
	private final boolean verbose;

	private String firmwareVersion;
	private String firmwareDate;
	private String phoneModel;

	/**
	 * Open the phone connection and initialize it
	 * 
	 * @param deviceAddress the Bluetooth hardware address to connect to (e.g.:
	 *            001122334455)
	 * @param channel the channel on the phone to connect to (e.g.: 15)
	 * @param verbose whether or not to print debugging information
	 * @throws GjokiiException if the Bluetooth hardware address is invalid
	 */
	public Gjokii(final BluetoothDevice device, final boolean verbose) throws GjokiiException {
		this.verbose = verbose;
		if (device == null) throw new GjokiiException("No device specified");
		try {
			con = device.createRfcommSocketToServiceRecord(UUID.fromString(COMMON_SPP_UUID));
			con.connect();
			is = con.getInputStream();
			os = con.getOutputStream();
			phoneInit();
		} catch (final IOException e) {
			throw new GjokiiException(GjokiiException.CONNECTION_PROBLEM, "unable to connect", e);
		}
	}

	/**
	 * Close the connection to the phone
	 * 
	 * @throws GjokiiException if closing the connection fails
	 */
	@Override
	public void close() throws GjokiiException {
		try {
			con.close();
		} catch (final IOException e) {
			throw new GjokiiException("unable to close connection: " + e.getMessage());
		}
	}

	/**
	 * Delete a file from the phone with the specified path
	 * 
	 * @param pathFileName the file to delete
	 */
	public void deleteFile(final String pathFileName) throws GjokiiException {
		final DirectoryEntryInfo d = getEntryInfo(pathFileName);
		if (!d.isFile()) throw new GjokiiException("not a file or does not exist");
		final byte[] fileNameBytes = Utils.stringToBytes(pathFileName, true);
		final byte[] deleteFile = Utils.appendToByteArray(DELETE_FILE, fileNameBytes);
		deleteFile[5] = (byte) fileNameBytes.length;
		send((byte) 0x6d, deleteFile);
		/* we assume that if the file exists, deleting succeeds */
		receive();
	}

	/**
	 * Dump the file system of the phone starting from a certain directory.
	 * 
	 * @param directoryPath the directory to start from
	 * @param recursive whether or not to recursively get the files and
	 *            directories below the provided directory
	 * @throws GjokiiException if the path is invalid
	 */
	public void dumpFileSystem(final String directoryPath, final boolean recursive) throws GjokiiException {
		final File outputDir = new File("output");
		outputDir.mkdir();
		dumpFileSystem("output", directoryPath, recursive);
	}

	/**
	 * Get a directory list.
	 * 
	 * @param directoryPath the directory to list
	 * @return a list of directories and files
	 * @throws GjokiiException if getting the file list fails
	 */
	public ArrayList<DirectoryEntryInfo> getDirectoryList(String directoryPath) throws GjokiiException {

		/* make sure the directory exists */
		if (!directoryPath.equals("/")) {
			final DirectoryEntryInfo d = getEntryInfo(directoryPath.substring(0, directoryPath.length() - 1));
			if (!d.isDirectory()) throw new GjokiiException("not a directory or does not exist");
		}
		directoryPath += "*";
		byte[] fileList = FILE_LIST;
		final byte[] filePathBytes = Utils.stringToBytes(directoryPath, true);

		final ArrayList<DirectoryEntryInfo> directoryListing = new ArrayList<DirectoryEntryInfo>();

		/* the length of the path in bytes should be set in the request */
		fileList[5] = (byte) filePathBytes.length;

		fileList = Utils.appendToByteArray(fileList, filePathBytes);
		send((byte) 0x6d, fileList);

		final byte[] result = receive();
		/*
		 * we receive the whole file list in one data block, we need to parse
		 * this file in order to retrieve all the entries in there
		 */
		int offset = 0;
		while (offset < result.length) {
			/* the short at offset 4 contains the length of the current block */
			final short length = Utils.byteArrayToShort(result, offset + 4);
			final byte[] entryData = new byte[length + 6];
			System.arraycopy(result, offset, entryData, 0, length + 6);
			directoryListing.add(new DirectoryEntryInfo(entryData));
			offset += length + 6;
		}
		return directoryListing;
	}

	/**
	 * Gets a file from the phone located at the specified path.
	 * 
	 * The file is written to the current directory (PWD) with the same name as
	 * the file being fetched.
	 * 
	 * @param fileName the file with full path to get
	 * @throws GjokiiException if no file was specified, if a directory was
	 *             specified, if a non existing file was specified, or if
	 *             writing the file to the local file system failed.
	 */
	public void getFile(final String fileName) throws GjokiiException {
		final String fileNameParts[] = fileName.split("/");
		getFile(fileName, new File(fileNameParts[fileNameParts.length - 1]));
	}

	/**
	 * Gets a file from the phone located at the specified path.
	 * 
	 * The file is written to name (and path) specified by targetFileName
	 * 
	 * @param fileName the file with full path to get
	 * @param targetFile the file to write to
	 * @throws GjokiiException if no file was specified, if a directory was
	 *             specified, if a non existing file was specified, or if
	 *             writing the file to the local file system failed.
	 */
	public void getFile(final String fileName, final File targetFile) throws GjokiiException {
		if (fileName == null) throw new GjokiiException("no file name to get specified");
		if (fileName.endsWith("/")) throw new GjokiiException("cannot fetch a directory");

		final DirectoryEntryInfo fi = getEntryInfo(fileName);

		if (fi.isDirectory()) throw new GjokiiException("cannot fetch a directory");
		if (!fi.isFile()) throw new GjokiiException("file does not exist");

		if (verbose) {
			log(fi);
		}
		final int fileSize = fi.getEntrySize();
		final int numberOfBlocks = fileSize % BLOCK_SIZE != 0 ? fileSize / BLOCK_SIZE + 1 : fileSize / BLOCK_SIZE;
		final short fileDesc = getFileDescriptor(fileName);
		final byte[] getFile = GET_FILE;
		/* add the fileId to the request byte array */
		getFile[8] = Utils.shortToByteArray(fileDesc)[0];
		getFile[9] = Utils.shortToByteArray(fileDesc)[1];

		try {
			final FileOutputStream fos = new FileOutputStream(targetFile);
			final DataOutputStream fileStream = new DataOutputStream(fos);
			for (int i = 0; i < numberOfBlocks; i++) {
				/* add the the current block number to the request byte array */
				getFile[11] = Utils.shortToByteArray((short) i)[0];
				getFile[12] = Utils.shortToByteArray((short) i)[1];
				/* add the requested number of bytes to the request byte array */
				final short bytesWanted = (short) (i < numberOfBlocks - 1 ? BLOCK_SIZE : fileSize % BLOCK_SIZE);
				getFile[20] = Utils.shortToByteArray(bytesWanted)[0];
				getFile[21] = Utils.shortToByteArray(bytesWanted)[1];
				send((byte) 0x6d, getFile);
				final byte[] tmp = receive();
				/*
				 * maybe we should look at the number receive in tmp buffer
				 * instead of just assuming we get what we want!
				 */
				assert bytesWanted == tmp[15];
				fileStream.write(tmp, 16, bytesWanted);
			}
			fileStream.close();

			/* retail file/date of file */
			targetFile.setLastModified(fi.getEntryTimeStamp());

			/* close the file */
			final byte[] closeFile = CLOSE_FILE;
			closeFile[8] = Utils.shortToByteArray(fileDesc)[0];
			closeFile[9] = Utils.shortToByteArray(fileDesc)[1];
			send((byte) 0x6d, closeFile);
			receive();
		} catch (final FileNotFoundException e) {
			throw new GjokiiException("target file cannot be created: " + e.getMessage());
		} catch (final IOException e) {
			throw new GjokiiException("error writing to file: " + e.getMessage());
		}
	}

	/**
	 * Get the phone IMEI number
	 * 
	 * @return the IMEI number
	 */
	public String getIMEI() throws GjokiiException {
		send((byte) 0x1b, PHONE_IMEI);
		final byte[] result = receive();
		return new String(result, 16, 15);
	}

	/**
	 * Returns human readable information about the phone
	 * 
	 * @return the information
	 */
	public DeviceInfo getInfo() {
		return new DeviceInfo(firmwareVersion, firmwareDate, phoneModel);
	}

	/**
	 * Puts a file on the phone, we assume that the last part of the
	 * targetPathFileName is a file that exists in the current directory
	 * 
	 * @param targetPathFileName the file name of the file on the phone we want
	 *            to write to
	 */
	public void putFile(final String targetPathFileName) throws GjokiiException {
		final String[] pathComponents = targetPathFileName.split("/");
		final String sourcePathFileName = pathComponents[pathComponents.length - 1];
		putFile(targetPathFileName, new File(sourcePathFileName));
	}

	/**
	 * Puts a file on the phone
	 * 
	 * @param targetPathFileName the file name of the file on the phone we want
	 *            to write to
	 * @param sourceFile the local file name
	 */
	public void putFile(final String targetPathFileName, final File sourceFile) throws GjokiiException {
		final byte[] fileNameBytes = Utils.stringToBytes(targetPathFileName, true);

		final byte[] putFileId = Utils.appendToByteArray(PUT_FILE_ID, fileNameBytes);
		putFileId[6] = Utils.shortToByteArray((short) fileNameBytes.length)[0];
		putFileId[7] = Utils.shortToByteArray((short) fileNameBytes.length)[1];
		send((byte) 0x6d, putFileId);
		byte[] result = receive();
		final short fileId = Utils.byteArrayToShort(result, 14);

		final byte[] putFile = PUT_FILE;
		putFile[8] = Utils.shortToByteArray(fileId)[0];
		putFile[9] = Utils.shortToByteArray(fileId)[1];
		FileInputStream fis = null;
		try {
			/* open the source file */
			fis = new FileInputStream(sourceFile);
			final byte[] buffer = new byte[BLOCK_SIZE];
			short bytesRead;

			while ((bytesRead = (short) fis.read(buffer)) >= 0) {
				byte[] blockPutFile = putFile;
				/* set the number of bytes in the byte array */
				blockPutFile[12] = Utils.shortToByteArray(bytesRead)[0];
				blockPutFile[13] = Utils.shortToByteArray(bytesRead)[1];
				/* add the byte array data */
				blockPutFile = Utils.appendToByteArray(blockPutFile, buffer, 0, bytesRead);
				send((byte) 0x6d, blockPutFile);
				result = receive();
			}
		} catch (final IOException e) {
			throw new GjokiiException("unable to read from source file: " + e.getMessage());
		} finally {
			Utils.closeSliently(fis);
		}
		/* close the file */
		final byte[] closeFile = CLOSE_FILE;
		closeFile[8] = Utils.shortToByteArray(fileId)[0];
		closeFile[9] = Utils.shortToByteArray(fileId)[1];
		send((byte) 0x6d, closeFile);
		receive();
	}

	/**
	 * Reboot the phone
	 * 
	 * @throws GjokiiException if rebooting fails
	 */
	public void reboot() throws GjokiiException {
		send((byte) 0x15, PHONE_RESET);
		receive();
		close();
	}

	protected void log(final Object message) {
		if (!verbose) return;
		System.out.println(message);
	}

	/**
	 * Dump the file system of the phone starting from a certain directory.
	 * 
	 * @param hostDirPathName the file system directory to write the dump to
	 * @param phoneDirPathName the directory to start from
	 * @param recursive whether or not to recursively get the files and
	 *            directories below the provided directory
	 * @throws GjokiiException if the path is invalid
	 */
	private void dumpFileSystem(final String hostDirPathName, final String phoneDirPathName, final boolean recursive)
			throws GjokiiException {
		final ArrayList<DirectoryEntryInfo> fileList = getDirectoryList(phoneDirPathName + "*");
		for (final DirectoryEntryInfo d : fileList) {
			if (d.isDirectory()) {
				/*
				 * directory: when in recursive mode create it, do nothing
				 * otherwise because then we are only interested in the files
				 */
				if (recursive) {
					final String newDirPathName = hostDirPathName + File.separator + d.getEntryName();
					final File newDir = new File(newDirPathName);
					newDir.mkdir();
					dumpFileSystem(newDirPathName, phoneDirPathName + d.getEntryName() + "/", recursive);

					/* retail file/date of file */
					newDir.setLastModified(d.getEntryTimeStamp());
				}
			} else if (d.isFile()) {
				final String newFilePathName = hostDirPathName + File.separator + d.getEntryName();
				getFile(phoneDirPathName + d.getEntryName(), new File(newFilePathName));
			} else {
				/* probably empty directory, ignore */
			}
		}
	}

	/**
	 * Get information about a directory entry.
	 * 
	 * @param filePathName the entry to get information about
	 * @return the object containing information about the entry
	 * @throws GjokiiException
	 */
	private DirectoryEntryInfo getEntryInfo(final String filePathName) throws GjokiiException {
		final byte[] fileNameBytes = Utils.stringToBytes(filePathName, true);
		final byte[] getFileInfo = Utils.appendToByteArray(FILE_INFO, fileNameBytes);
		send((byte) 0x6d, getFileInfo);
		return new DirectoryEntryInfo(receive());
	}

	/**
	 * Get a file descriptor.
	 * 
	 * @param filePathName the file to get, make sure the entry exists and is a
	 *            file by using getFileInfo first
	 * @return the file descriptor
	 */
	private short getFileDescriptor(final String filePathName) throws GjokiiException {
		final byte[] fileNameBytes = Utils.stringToBytes(filePathName, true);
		final byte[] getFileID = Utils.appendToByteArray(GET_FILE_ID, fileNameBytes);
		send((byte) 0x6d, getFileID);
		final byte[] result = receive();
		final byte[] fileDescriptor = Utils.subByteArray(result, 14, 2);
		final short fileDesc = Utils.byteArrayToShort(fileDescriptor, 0);
		return fileDesc;
	}

	/**
	 * Initialize the phone connection
	 * 
	 * @throws GjokiiException if the phone returns unexpected data in response
	 *             to the initialization
	 */
	private void phoneInit() throws GjokiiException {
		send((byte) 0xd0, PHONE_INIT);
		byte[] result = receive();
		if (!Arrays.equals(result, new byte[] { (byte) 0x19, (byte) 0x10, (byte) 0x00, (byte) 0xd0, (byte) 0x00,
				(byte) 0x01, (byte) 0x05 })) throw new GjokiiException("unexpected response to initiatialization");
		send((byte) 0x1b, PHONE_INFO);
		result = receive();
		try {
			final BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result)));
			br.skip(18);
			firmwareVersion = br.readLine();
			firmwareDate = br.readLine();
			phoneModel = br.readLine();
		} catch (final IOException e) {
			throw new GjokiiException("unable to retrieve phone firmware information");
		}
	}

	/**
	 * Receive data from the phone
	 * 
	 * @return the data
	 * @throws GjokiiException if there was a problem receiving the data
	 */
	private byte[] receive() throws GjokiiException {
		byte[] received = null;
		final byte[] buffer = new byte[64];
		try {
			do {
				final int bytesRead = is.read(buffer);
				if (bytesRead == -1) throw new GjokiiException("end of stream reached");
				final byte[] result = new byte[bytesRead];
				System.arraycopy(buffer, 0, result, 0, bytesRead);
				received = Utils.appendToByteArray(received, result);
				if (is.available() == 0) {
					/*
					 * although there is no data available right now, we wait a
					 * bit in hopes of still getting more data in a short time
					 * which belongs to this transfer, otherwise with the next
					 * command we will be in trouble as we still receive data
					 * belonging to this command. 100 ms seems to be adequate,
					 * but I guess it may need to be more if the phone is slower
					 * in dealing with all the data
					 */
					try {
						Thread.sleep(100);
					} catch (final InterruptedException e) {
					}
				}
			} while (is.available() > 0);
			if (verbose) {
				log("RECEIVED " + received.length + " bytes:\n" + Utils.hexDump(received));
			}
			return received;
		} catch (final IOException e) {
			throw new GjokiiException("problem receiving data: " + e.getMessage());
		}
	}

	/**
	 * Send data to the phone
	 * 
	 * @param msgType the message type
	 * @param data the data to send
	 * 
	 * @throws GjokiiException if there was a problem sending the data
	 */
	private void send(final byte msgType, final byte[] data) throws GjokiiException {
		byte[] message = BT_HEADER;
		message[3] = msgType;
		message[4] = Utils.shortToByteArray((short) data.length)[0];
		message[5] = Utils.shortToByteArray((short) data.length)[1];
		message = Utils.appendToByteArray(message, data);
		try {
			if (verbose) {
				log("SENT:\n" + Utils.hexDump(message));
			}
			os.write(message);
			os.flush();
		} catch (final IOException e) {
			throw new GjokiiException("problem sending data: " + e.getMessage());
		}
	}

	public static final class DeviceInfo {
		private final String firmwareVersion, firmwareDate, phoneModel;

		private DeviceInfo(final String firmwareVersion, final String firmwareDate, final String phoneModel) {
			this.firmwareVersion = firmwareVersion;
			this.firmwareDate = firmwareDate;
			this.phoneModel = phoneModel;
		}

		public String getFirmwareDate() {
			return firmwareDate;
		}

		public String getFirmwareVersion() {
			return firmwareVersion;
		}

		public String getPhoneModel() {
			return phoneModel;
		}

		@Override
		public String toString() {
			return "DeviceInfo [firmwareVersion=" + firmwareVersion + ", firmwareDate=" + firmwareDate
					+ ", phoneModel=" + phoneModel + "]";
		}

	}
}
