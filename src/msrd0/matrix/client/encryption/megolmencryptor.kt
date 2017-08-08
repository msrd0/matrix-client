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

package msrd0.matrix.client.encryption

import msrd0.matrix.client.encryption.EncryptionAlgorithms.*
import com.beust.klaxon.*
import msrd0.matrix.client.event.MatrixEventTypes.*
import org.matrix.olm.*
import java.time.LocalDateTime
import kotlin.collections.set

class MegolmEncryptor(val olm : OlmEncryption, val roomId : String, val maxMessages : Int = 0, val maxTime : Long = 0) : RoomEncryptor
{
	private var outSession : OlmOutboundGroupSession = OlmOutboundGroupSession()
	private var inSessions : MutableMap<String, OlmInboundGroupSession> = mutableMapOf(outSession.sessionIdentifier() to OlmInboundGroupSession(outSession.sessionKey()))
	private var senderSessions : MutableMap<String, String> = mutableMapOf(olm.identityKey to outSession.sessionIdentifier())
	private var pendingSecrets = true
	private val startTime : LocalDateTime = LocalDateTime.now()
	
	override fun getEncryptedJson(event : JsonObject) : JsonObject
	{
		val formattedJson = event.toJsonString(false, true)
		val nextMessageIndex = outSession.messageIndex()
		val encryptedJson = outSession.encryptMessage(formattedJson)
		
		if (maxMessages != 0)
		{
			if (nextMessageIndex >= maxMessages)
				renew()
			
		}
		if (maxTime != 0L)
		{
			if (LocalDateTime.now().minusSeconds(maxTime) <= startTime)
				renew()
		}
		val resultJson = JsonObject()
		resultJson["type"] = ROOM_ENCRYPTED
		resultJson["room_id"] = roomId
		resultJson.mapNested("content.sender_key", olm.identityKey)
		resultJson.mapNested("content.ciphertext", encryptedJson)
		resultJson.mapNested("content.algorithm", MEGOLM_AES_SHA2)
		resultJson.mapNested("content.device_id", olm.deviceId)
		return resultJson
	}
	
	override fun getDecryptedJson(event : JsonObject) : JsonObject
	{
		if (event["type"] == ROOM_ENCRYPTED && event["room_id"] == roomId)
		{
			if (event.getNested("content.algorithm") == MEGOLM_AES_SHA2)
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
	
	fun renew()
	{
		outSession = OlmOutboundGroupSession()
		inSessions[outSession.sessionIdentifier()] = OlmInboundGroupSession(outSession.sessionKey())
		senderSessions[olm.identityKey] = outSession.sessionIdentifier()
		pendingSecrets = true
	}
	
	override fun getSecrets() : JsonObject
	{
		pendingSecrets = false
		val megolmJson = JsonObject()
		megolmJson["algorithm"] = MEGOLM_AES_SHA2
		megolmJson["room_id"] = roomId
		megolmJson["session_id"] = outSession.sessionIdentifier()
		megolmJson["session_key"] = outSession.sessionKey()
		val eventJson = JsonObject()
		eventJson["type"] = ROOM_KEY
		eventJson["content"] = megolmJson
		pendingSecrets = false
		return megolmJson
	}
	
	override fun hasPendingSecrets() : Boolean = pendingSecrets
	
	/**
	 * Add session IDs and session keys to this Megolm encryptor
	 *
	 * The map must contain ```session_id, session_key``` and ```curve25519``` which is the identity key of the sending
	 * device. Usually, this information is transmitted by the sending side as ```m.room_key``` event. The default behavior
	 * of transmitting all json variables applies to this method as well.
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
