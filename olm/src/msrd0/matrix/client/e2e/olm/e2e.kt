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

package msrd0.matrix.client.e2e.olm

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.RoomId
import msrd0.matrix.client.e2e.*
import msrd0.matrix.client.e2e.olm.EncryptionAlgorithms.*
import msrd0.matrix.client.event.encryption.ForwardedRoomKeyEventContent
import msrd0.matrix.client.event.encryption.RoomKeyEventContent
import org.matrix.olm.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

open class OlmE2E(val keyStore : KeyStore) : E2E
{
	companion object
	{
		private val logger : Logger = LoggerFactory.getLogger(OlmE2E::class.java)
		
		init { OlmManager() }
		
		val olmUtil by lazy { OlmUtility() }
	}
	
	override val encryptionAlgorithm : String
		get() = OLM_V1_RATCHET
	override val roomEncryptionAlgorithm : String
		get() = MEGOLM_V1_RATCHET
	override val supportedAlgorithms : List<String>
		get() = ALGORITHMS
	
	override val fingerprintKeyName : String
		get() = OlmAccount.JSON_KEY_FINGER_PRINT_KEY
	override val identityKeyName : String
		get() = OlmAccount.JSON_KEY_IDENTITY_KEY
	override val oneTimeKeyName : String
		get() = OlmAccount.JSON_KEY_ONE_TIME_KEY
	
	lateinit var account : OlmAccount
		private set
	
	@Throws(OlmException::class)
	protected fun newAccount() : OlmAccount
	{
		val account = OlmAccount()
		keyStore.account = account
		return account
	}
	
	@Throws(MatrixOlmException::class)
	override fun initialise()
	{
		if (this::account.isInitialized)
			return
		wrapOlmEx {
			account = if (keyStore.hasAccount()) keyStore.account else newAccount()
		}
	}
	
	override val identityKeys : JsonObject
		@Throws(MatrixOlmException::class) get() = wrapOlmEx { account.identityKeysJson() }
	override val fingerprintKey : String
		@Throws(MatrixOlmException::class) get() = wrapOlmEx { account.identityKeys().ed25519 }
	override val identityKey : String
		@Throws(MatrixOlmException::class) get() = wrapOlmEx { account.identityKeys().curve25519 }
	
	override val maxOneTimeKeyCount : Int
		get() = account.maxOneTimeKeys().toInt()
	
	@Throws(MatrixOlmException::class)
	override fun generateOneTimeKeys(number : Int) : JsonObject = wrapOlmEx {
		account.generateOneTimeKeys(number)
		val keys = account.oneTimeKeys()
		keyStore.account = account
		return keys
	}
	
	override fun markOneTimeKeysAsPublished()
	{
		try
		{
			account.markOneTimeKeysAsPublished()
			keyStore.account = account
		}
		catch (ex : OlmException)
		{
			logger.error("Error while marking one-time keys as published", ex)
		}
	}
	
	
	@Throws(MatrixOlmException::class)
	override fun sign(message : String) : String
			= wrapOlmEx { account.signMessage(message) }
	
	override fun verifySignature(signature : String, key : String, message : String) : Boolean
	{
		try
		{
			olmUtil.verifyEd25519Signature(signature, key, message)
		}
		catch (ex : OlmException)
		{
			return false
		}
		return true
	}
	
	
	@Throws(MatrixOlmException::class)
	override fun outboundSession(receiverIdentityKey : String, receiverOneTimeKey : String) : E2ESession
	{
		val olmSession : OlmSession = wrapOlmEx olmSession@ {
			val session = OlmSession()
			session.initOutboundSession(account, receiverIdentityKey, receiverOneTimeKey)
			return@olmSession session
		}
		return OlmSessionWrapper(this, olmSession)
	}
	
	@Throws(MatrixOlmException::class)
	override fun inboundSession(message : E2EMessage, senderIdentityKey : String) : E2ESession
	{
		val olmSession : OlmSession = wrapOlmEx olmSession@ {
			// try to find an existing session
			keyStore.allSessions().find {
				it.matchesInboundSession(message.ciphertext)
			}?.let {
				return@olmSession it
			}
			
			// create a new one
			val session = OlmSession()
			session.initInboundSessionFrom(account, senderIdentityKey, message.ciphertext)
			keyStore.storeSession(session)
			account.removeOneTimeKeys(session)
			keyStore.account = account
			return@olmSession session
		}
		return OlmSessionWrapper(this, olmSession)
	}
	
	
	@Throws(OlmException::class, RoomKeyMismatchException::class)
	protected open fun storeInboundSessionForRoomKey(roomId : RoomId, roomKey : String, sessionId : String? = null)
	{
		val inboundSession = OlmInboundGroupSession(roomKey)
		if (sessionId != null && inboundSession.sessionIdentifier() != sessionId)
			throw RoomKeyMismatchException("Received key session ${inboundSession.sessionIdentifier()} does not match received session $sessionId")
		keyStore.storeInboundSession(inboundSession)
	}
	
	@Throws(OlmException::class, RoomKeyMismatchException::class)
	protected open fun storeInboundSessionForSessionKey(roomId : RoomId, sessionKey : String, sessionId : String? = null)
	{
		val inboundSession = OlmInboundGroupSession.importSession(sessionKey)
		if (sessionId != null && inboundSession.sessionIdentifier() != sessionId)
			throw RoomKeyMismatchException("Received key session ${inboundSession.sessionIdentifier()} does not match received session $sessionId")
		keyStore.storeInboundSession(inboundSession)
	}
	
	override fun roomKeyReceived(roomKey : RoomKeyEventContent)
	{
		try
		{
			if (roomKey.algorithm != MEGOLM_V1_RATCHET)
				throw RoomKeyMismatchException("Received key algorithm is not $MEGOLM_V1_RATCHET")
			storeInboundSessionForRoomKey(roomKey.roomId, roomKey.sessionKey, roomKey.sessionId)
		}
		catch (ex : Exception) // TODO once kotlin supports multicatch, make this `OlmException | RoomKeyMismatchException`
		{
			logger.error("Error while handling received room key", ex)
		}
	}
	
	override fun roomKeyReceived(roomKey : ForwardedRoomKeyEventContent)
	{
		try
		{
			if (roomKey.algorithm != MEGOLM_V1_RATCHET)
				throw RoomKeyMismatchException("Received key algorithm is not $MEGOLM_V1_RATCHET")
			storeInboundSessionForSessionKey(roomKey.roomId, roomKey.sessionKey, roomKey.sessionId)
		}
		catch (ex : Exception) // TODO once kotlin supports multicatch, make this `OlmException | RoomKeyMismatchException`
		{
			logger.error("Error while handling received room key", ex)
		}
	}
	
	
	@Throws(MatrixOlmException::class)
	override fun findOutboundGroupSession(roomId : RoomId) : E2EOutboundGroupSession?
	{
		val (olmSession, timestamp) = wrapOlmEx { keyStore.findOutboundSession(roomId) } ?: return null
		return OlmOutboundGroupSessionWrapper(this, roomId, olmSession, timestamp)
	}
	
	@Throws(MatrixOlmException::class)
	override fun newOutboundGroupSession(roomId : RoomId) : E2EOutboundGroupSession = wrapOlmEx {
		val olmSession = OlmOutboundGroupSession()
		val timestamp = LocalDateTime.now()
		keyStore.storeOutboundSession(roomId, olmSession, timestamp)
		
		val inboundSession = OlmInboundGroupSession(olmSession.sessionKey())
		keyStore.storeInboundSession(inboundSession)
		
		return OlmOutboundGroupSessionWrapper(this, roomId, olmSession, timestamp)
	}
	
	@Throws(MatrixOlmException::class)
	override fun findInboundGroupSession(roomId : RoomId, sessionId : String) : E2EInboundGroupSession?
	{
		val olmSession = wrapOlmEx { keyStore.findInboundSession(sessionId) } ?: return null
		return OlmInboundGroupSessionWrapper(this, olmSession)
	}
}
