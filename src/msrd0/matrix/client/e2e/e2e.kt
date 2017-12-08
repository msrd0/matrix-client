/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
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

package msrd0.matrix.client.e2e

import com.beust.klaxon.*
import msrd0.matrix.client.RoomId
import msrd0.matrix.client.event.encryption.ForwardedRoomKeyEventContent
import msrd0.matrix.client.event.encryption.RoomKeyEventContent

interface E2E
{
	// some static constants that need to be overridden
	
	/**
	 * The encryption algorithm used.
	 */
	val encryptionAlgorithm : String
	
	/**
	 * The algorithm used to encrypt rooms.
	 */
	val roomEncryptionAlgorithm : String
	
	/**
	 * A list of all supported algorithms.
	 */
	val supportedAlgorithms : List<String>
	
	/**
	 * Return the name of the fingerprint key.
	 */
	val fingerprintKeyName : String
	
	/**
	 * Return the name of the identity key the identity key.
	 */
	val identityKeyName : String
	
	/**
	 * Return the name of the one time keys.
	 */
	val oneTimeKeyName : String
	
	// init function
	
	/**
	 * Initialise this [E2E]. After calling this method, the identity keys should be populated.
	 */
	@Throws(MatrixE2EException::class)
	fun initialise()
	
	// key operations
	
	/**
	 * Return the identity keys (identity and fingerprint key) of the client as a json object.
	 */
	val identityKeys : JsonObject
		@Throws(MatrixE2EException::class) get
	
	/**
	 * Return the fingerprint key.
	 */
	val fingerprintKey : String
		@Throws(MatrixE2EException::class) get() = identityKeys.string(fingerprintKeyName)!!
	
	/**
	 * Return the identity key.
	 */
	val identityKey : String
		@Throws(MatrixE2EException::class) get() = identityKeys.string(identityKeyName)!!
	
	/**
	 * Return the maximum number of one-time keys supported by this [E2E] instance. The client will try to manage about
	 * half of that amount on the server.
	 */
	val maxOneTimeKeyCount : Int
	
	/**
	 * Generate the [number] of one time keys.
	 *
	 * @return The generated keys as a json object.
	 */
	@Throws(MatrixE2EException::class)
	fun generateOneTimeKeys(number : Int) : JsonObject
	
	/**
	 * Mark the newly generated one-time keys as published.
	 */
	fun markOneTimeKeysAsPublished()
	
	// signing
	
	/**
	 * Sign the given [message] using the clients identity keys.
	 *
	 * @return The signature of the message.
	 */
	@Throws(MatrixE2EException::class)
	fun sign(message : String) : String
	
	/**
	 * Sign the given json [message] using the clients identity keys.
	 *
	 * @return The signature of the message.
	 */
	@Throws(MatrixE2EException::class)
	fun sign(message : JsonBase) : String
			= sign(message.toJsonString(canonical = true))
	
	/**
	 * Verify the signature of the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun verifySignature(signature : String, key : String, message : String) : Boolean
	
	/**
	 * Verify the signature of the json [message].
	 */
	@Throws(MatrixE2EException::class)
	fun verifySignature(signature : String, key : String, message : JsonBase) : Boolean
			 = verifySignature(signature, key, message.toJsonString(canonical = true))
	
	// sessions with one-time keys
	
	/**
	 * Return an outbound session for the receivers identity and one time key.
	 */
	@Throws(MatrixE2EException::class)
	fun outboundSession(receiverIdentityKey : String, receiverOneTimeKey : String) : E2ESession
	
	/**
	 * Return an inbound session for the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun inboundSession(message : E2EMessage, senderIdentityKey : String) : E2ESession
	
	// room keys
	
	/**
	 * Receive and store the room key.
	 */
	fun roomKeyReceived(roomKey : RoomKeyEventContent)
	
	/**
	 * Receive and store the forwarded room key.
	 */
	fun roomKeyReceived(roomKey : ForwardedRoomKeyEventContent)
	
	// room outbound sessions
	
	/**
	 * Find an outbound group session for the given room id, or return null.
	 */
	fun findOutboundGroupSession(roomId : RoomId) : E2EOutboundGroupSession?
	
	/**
	 * Create a new outbound group session for the given room id. The corresponding inbound group session should
	 * automatically be created and stored as if [roomKeyReceived] was called.
	 */
	@Throws(MatrixE2EException::class)
	fun newOutboundGroupSession(roomId : RoomId) : E2EOutboundGroupSession
	
	// room inbound sessions
	
	/**
	 * Find an inbound group session for the given room and session id, or return null.
	 */
	fun findInboundGroupSession(roomId : RoomId, sessionId : String) : E2EInboundGroupSession?
}
