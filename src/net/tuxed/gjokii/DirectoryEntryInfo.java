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

import java.util.GregorianCalendar;

import net.tuxed.misc.Utils;

/**
 * This class can be used to extract information about file and directories on
 * the phone file system.
 * 
 * @author F. Kooman <fkooman@tuxed.net>
 * 
 */
public class DirectoryEntryInfo {
	private String entryName;
	private final byte entryType;
	private int entrySize;
	private short entryYear;
	private short entryMonth;
	private short entryDay;
	private short entryHour;
	private short entryMinute;
	private short entrySecond;

	/**
	 * The directory entry data to request information about.
	 * 
	 * @param entryData the entry data to analyze
	 */
	public DirectoryEntryInfo(final byte[] entryData) throws GjokiiException {
		entryType = entryData[14];
		if (isFile() || isDirectory()) {
			entrySize = Utils.byteArrayToInt(entryData, 16);
			entryYear = Utils.byteArrayToShort(entryData, 20);
			entryMonth = entryData[22];
			entryDay = entryData[23];
			entryHour = entryData[24];
			entryMinute = entryData[25];
			entrySecond = entryData[26];
			final short entryNameLength = Utils.byteArrayToShort(entryData, 36);
			entryName = Utils.bytesToString(entryData, 38, entryNameLength * 2 - 2);
		}
	}

	/**
	 * Get the human readable date of the entry.
	 * 
	 * @return the date
	 */
	public String getEntryDate() {
		return Utils.shortToTwoDigitString(entryYear) + "-" + Utils.shortToTwoDigitString(entryMonth) + "-"
				+ Utils.shortToTwoDigitString(entryDay);
	}

	/**
	 * Get the human readable entry name of the entry.
	 */
	public String getEntryName() {
		return entryName;
	}

	/**
	 * Get the size of the entry.
	 * 
	 * @return the file size
	 */
	public int getEntrySize() {
		return entrySize;
	}

	/**
	 * Get the time of the entry.
	 * 
	 * @return the time
	 */
	public String getEntryTime() {
		return Utils.shortToTwoDigitString(entryHour) + ":" + Utils.shortToTwoDigitString(entryMinute) + ":"
				+ Utils.shortToTwoDigitString(entrySecond);
	}

	/**
	 * Get the UNIX time stamp of the entry (number of seconds since January 1st
	 * 1970).
	 * 
	 * @return the UNIX time stamp
	 */
	public long getEntryTimeStamp() {
		final GregorianCalendar c = new GregorianCalendar(entryYear, entryMonth, entryDay, entryHour, entryMinute,
				entrySecond);
		return c.getTime().getTime();
	}

	/**
	 * Whether or not the entry is a directory.
	 * 
	 * @return true if the entry is a directory, false if not.
	 */
	public boolean isDirectory() {
		return (entryType & 0x40) == 0x40;
	}

	/**
	 * Whether or not the entry is a file.
	 * 
	 * @return true if the entry is a file, false if not.
	 */
	public boolean isFile() {
		return (entryType & 0x80) == 0x80;
	}
}
