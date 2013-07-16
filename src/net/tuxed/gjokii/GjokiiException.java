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

import java.io.IOException;

public class GjokiiException extends IOException {

	private static final long serialVersionUID = 5336560319846071336L;

	public static final int UNKNOWN_ERROR = 0;
	public static final int CONNECTION_PROBLEM = 1;
	public static final int INVALID_CERT_FILE = 2;

	private final int errorCode;

	public GjokiiException() {
		this(UNKNOWN_ERROR);
	}

	public GjokiiException(final int errorCode) {
		this(errorCode, null, null);
	}

	public GjokiiException(final int errorCode, final String string) {
		this(errorCode, string, null);
	}

	public GjokiiException(final int errorCode, final String message, final Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public GjokiiException(final int errorCode, final Throwable cause) {
		this(errorCode, null, cause);
	}

	public GjokiiException(final String detailMessage) {
		this(UNKNOWN_ERROR, detailMessage);
	}

	public GjokiiException(final String message, final Throwable cause) {
		this(UNKNOWN_ERROR, message, cause);
	}

	public GjokiiException(final Throwable cause) {
		this(UNKNOWN_ERROR, cause);
	}

	public int getErrorCode() {
		return errorCode;
	}
}
