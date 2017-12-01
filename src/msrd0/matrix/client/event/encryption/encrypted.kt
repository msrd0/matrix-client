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
import msrd0.matrix.client.event.encryption.EncryptionAlgorithms.*
import org.matrix.olm.OlmException

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
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			algorithm = json.string("algorithm") ?: missing("algorithm"),
			ciphertext = json.string("ciphertext") ?: missing("ciphertext"),
			deviceId = json.string("device_id"),
			senderKey = json.string("sender_key"),
			sessionId = json.string("session_id")
	)
	
	@Throws(OlmException::class)
	fun decrypt(room : Room) : MessageContent
	{
		if (algorithm != MEGOLM_V1_RATCHET)
			throw IllegalStateException("Unknown algorithm '$algorithm'")
		
		val session = room.findOrCreateInboundSession()
		val decrypted = session.decryptMessage(ciphertext)?.decryptedMessage
				?: throw RuntimeException("Decryption failed for unknown reasons") // TODO do something here
		val json = Parser().parse(StringBuilder(decrypted)) as JsonObject
		return MessageContent.fromJson(json)
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
		data : MatrixEventData,
		content : EncryptedEventContent
) : MatrixToDeviceEvent<EncryptedEventContent>(data, content)
{
	constructor(json : JsonObject, content : EncryptedEventContent)
			: this(MatrixEventData(json), content)
}

/**
 * A room encrypted message event.
 */
class EncryptedRoomEvent(
		room : Room,
		data : MatrixEventData,
		content : MessageContent
) : MatrixRoomEvent<MessageContent>(room, data, content), Message
{
	@Throws(IllegalJsonException::class, OlmException::class)
	constructor(room : Room, json : JsonObject)
			: this(room, MatrixEventData(json), EncryptedEventContent(json.obj("content") ?: missing("content")).decrypt(room))
	
	override val body get() = content.body
	override val msgtype get() = content.msgtype
}
