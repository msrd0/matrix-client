/*
 * matrix-client
 * Copyright (C) 2017 Julius Lehmann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package org.matrix.olm;

/**
 * Message class used in Olm sessions to contain the encrypted data.<br>
 * See {@link OlmSession#decryptMessage(OlmMessage)} and {@link OlmSession#encryptMessage(String)}.
 * <br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmMessage
{
	/** PRE KEY message type (used to establish new Olm session) **/
	public final static int MESSAGE_TYPE_PRE_KEY = 0;
	/** normal message type **/
	public final static int MESSAGE_TYPE_MESSAGE = 1;
	
	/** the encrypted message **/
	public String mCipherText;
	
	/** defined by {@link #MESSAGE_TYPE_MESSAGE} or {@link #MESSAGE_TYPE_PRE_KEY} **/
	public long mType;
}
