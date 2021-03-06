/*
 * matrix-client
 * Copyright (C) 2017-2018 Dominic Meiser
 * Copyright (C) 2017 Julius Lehmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
 */

package de.msrd0.matrix.client.encryption

import com.beust.klaxon.JsonObject

/**
 * **DEPRECATED**. Don't use. This will be replaced with the new e2e api very soon.
 */
@Deprecated("Will be replaced with e2e api")
interface RoomEncryptor
{
	/**
	 * Add new secret to this RoomEncryptor
	 *
	 * This function adds secrets previously exchanged with Olm to this instance of RoomEncryptor. This may include
	 * shared secrets to decrypt messages or session ids to initialize an underlying encryption object.
	 *
	 * The map should contain all all key-value pairs from the decrypted Json.
	 *
	 * @param secrets the map containing all secrets transmitted by olm
	 */
	fun addSecrets(secrets : Map<String, String>)
	
	/**
	 * Enrypts a room event
	 *
	 * After encryption has finished, the result should be sent as is to the corresponding room. A client/server may add
	 * additional properties **outside** of ```content```. The properties ```type``` and ```room_id``` are set by this
	 * class.
	 */
	fun getEncryptedJson(event : JsonObject) : JsonObject
	
	/**
	 * Decrypts a room event
	 *
	 * If an event can not be decrypted correctly, this method will throw an exception. To be able to decrypt all events
	 * successfully all known secrets should be passed to [addSecrets] to ensure successful decryption of all events.
	 */
	fun getDecryptedJson(event : JsonObject) : JsonObject
	
	/**
	 * Returns the secrets that need to be transmitted to other devices
	 *
	 * Other devices might need additional information to be able to decrypt events that have been encrypted previously
	 * by this class.
	 */
	fun getSecrets() : JsonObject
	
	/**
	 * Indicates whether underlying encryption parameters have changed.
	 *
	 * If so, [getSecrets] should be called and transmitted to all participating devices.
	 */
	fun hasPendingSecrets() : Boolean
}
