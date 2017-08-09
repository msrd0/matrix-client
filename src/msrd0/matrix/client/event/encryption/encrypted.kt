/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
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

package msrd0.matrix.client.event.encryption

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.event.MatrixEventTypes.*

/**
 * The content of a room encrypted event.
 */
class EncryptedEventContent(
		val algorithm : String,
		val ciphertext : String,
		val deviceId : String? = null,
		val senderKey : String? = null,
		val sessionId : String? = null
) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room encrypted event content by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : EncryptedEventContent
				= EncryptedEventContent(
					algorithm = json.string("algorithm") ?: missing("algorithm"),
					ciphertext = json.string("ciphertext") ?: missing("ciphertext"),
					deviceId = json.string("device_id"),
					senderKey = json.string("sender_key"),
					sessionId = json.string("session_id")
				)
	}
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["algorithm"] = algorithm
		json["ciphertext"] = ciphertext
		if (deviceId != null)
			json["device_id"] = deviceId
		if (senderKey != null)
			json["sender_key"] = senderKey
		if (sessionId != null)
			json["session_id"] = sessionId
		return json
	}
}

/**
 * A to-device encrypted event.
 */
class EncryptedEvent(
		sender : MatrixId,
		content : EncryptedEventContent
) : MatrixToDeviceEvent<EncryptedEventContent>(sender, ROOM_ENCRYPTED, content)
{
	companion object
	{
		/**
		 * Constructs a new encrypted event by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : EncryptedEvent
				= EncryptedEvent(MatrixId.fromString(json.string("sender") ?: missing("sender")),
					EncryptedEventContent.fromJson(json.obj("content") ?: missing("content")))
	}
	
	override val json : JsonObject get() = abstractJson
}

/**
 * A room encrypted event.
 */
class EncryptedRoomEvent(
		room : Room,
		sender : MatrixId,
		content : EncryptedEventContent
) : MatrixRoomEvent<EncryptedEventContent>(room, sender, ROOM_ENCRYPTED, content)
{
	companion object
	{
		/**
		 * Constructs a new encrypted event by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(room : Room, json : JsonObject) : EncryptedRoomEvent
				= EncryptedRoomEvent(room, MatrixId.fromString(json.string("sender") ?: missing("sender")),
					EncryptedEventContent.fromJson(json.obj("content") ?: missing("content")))
	}
	
	override val json : JsonObject get() = abstractJson
}
