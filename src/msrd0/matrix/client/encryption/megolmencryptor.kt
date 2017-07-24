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

package msrd0.matrix.client.encryption

import com.beust.klaxon.*
import org.matrix.olm.*
import kotlin.collections.set

class MegolmEncryptor(val olm : OlmEncryption, val roomId : String, val maxTime : String, val maxMessages : String) : RoomEncryptor
{
	private var outSession : OlmOutboundGroupSession = OlmOutboundGroupSession()
	private var inSessions : MutableMap<String, OlmInboundGroupSession> = mutableMapOf(outSession.sessionIdentifier() to OlmInboundGroupSession(outSession.sessionKey()))
	private var senderSessions : MutableMap<String, String> = mutableMapOf(olm.identityKey to outSession.sessionIdentifier())
	private var pendingSecrets = true
	
	override fun getEncryptedJson(event : JsonObject) : JsonObject
	{
		//TODO: test if time is up or max messages have been sent
		val formattedJson = event.toJsonString(false, true)
		val encryptedJson = outSession.encryptMessage(formattedJson)
		val resultJson = JsonObject()
		resultJson["type"] = "m.room.encrypted"
		resultJson["room_id"] = roomId
		resultJson.mapNested("content.sender_key", olm.identityKey)
		resultJson.mapNested("content.ciphertext", encryptedJson)
		resultJson.mapNested("content.algorithm", "m.megolm.v1.aes-sha2")
		resultJson.mapNested("content.device_id", olm.deviceId)
		return resultJson
	}
	
	override fun getDecryptedJson(event : JsonObject) : JsonObject
	{
		if (event["type"] == "m.room.encrypted" && event["room_id"] == roomId)
		{
			if (event.getNested("content.algorithm") == "m.megolm.v1.aes-sha2")
			{
				val senderKey = event.getNested("content.sender_key")
				val sessionId = event.getNested("content.session_id")
				if (senderSessions[senderKey] == sessionId)
				{
					val inSession = inSessions[sessionId]
					if (inSession != null)
					{
						val string : String = inSession.decryptMessage(event.getNested("content.ciphertext") as String).mDecryptedMessage
						return Parser().parse(StringBuilder(string)) as JsonObject
					}
				}
			}
		}
		return getErrorMessageJson()
	}
	
	override fun getSecrets() : JsonObject
	{
		val megolmJson = JsonObject()
		megolmJson["algorithm"] = "m.megolm.v1.aes-sha2"
		megolmJson["room_id"] = roomId
		megolmJson["session_id"] = outSession.sessionIdentifier()
		megolmJson["session_key"] = outSession.sessionKey()
		val eventJson = JsonObject()
		eventJson["type"] = "m.room_key"
		eventJson["content"] = megolmJson
		pendingSecrets = false
		return megolmJson
	}
	
	override fun hasPendingSecrets() : Boolean = pendingSecrets
	
	/**
	 * Add session IDs and session keys to this Megolm encryptor
	 *
	 * The map must contain ```session_id, session_key``` and ```curve25519``` which is the identity key of the sending
	 * device. Usually, this information is transmitted by the sending side as ```m.room_key``` event.
	 */
	override fun addSecrets(secrets : Map<String, String>)
	{
		val sessionKey = secrets["session_key"]
		val sessionId = secrets["session_id"]
		val idKey = secrets["curve25519"]
		if (sessionKey != null && sessionId != null && idKey != null)
		{
			inSessions[sessionId] = OlmInboundGroupSession(sessionKey)
			senderSessions[idKey] = sessionId
		}
	}
}