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

import msrd0.matrix.client.RoomId
import msrd0.matrix.client.e2e.*
import org.matrix.olm.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MILLIS

open class OlmSessionWrapper(
		val e2e : OlmE2E,
		val olmSession : OlmSession,
		val otherIdentityKey : String
) : E2ESession
{
	override val sessionId : String
		get() = olmSession.sessionIdentifier()
	
	@Throws(MatrixOlmException::class)
	override fun encrypt(message : String) : E2EMessage = wrapOlmEx encrypted@ {
		val encrypted = olmSession.encryptMessage(message)
		e2e.keyStore.storeSession(otherIdentityKey, olmSession)
		return@encrypted encrypted
	}.e2eMessage
	
	@Throws(MatrixOlmException::class)
	override fun decrypt(message : E2EMessage) : String = wrapOlmEx decrypted@ {
		val decrypted = olmSession.decryptMessage(message.olmMessage)
		e2e.keyStore.storeSession(otherIdentityKey, olmSession)
		return@decrypted decrypted
	}
}


open class OlmOutboundGroupSessionWrapper(
		val e2e : OlmE2E,
		val roomId : RoomId,
		val olmSession : OlmOutboundGroupSession,
		val olmSessionTimestamp : LocalDateTime
) : E2EOutboundGroupSession
{
	override val sessionId : String
		get() = olmSession.sessionIdentifier()
	
	override val sessionKey : String
		get() = olmSession.sessionKey()
	
	override fun needsRotation(messages : Int, lifetime : Long) : Boolean
			= (olmSession.messageIndex() >= messages
					|| olmSessionTimestamp.until(LocalDateTime.now(), MILLIS) >= lifetime)
	
	@Throws(MatrixE2EException::class)
	override fun encrypt(message : String) : String = wrapOlmEx encrypted@ {
		val encrypted = olmSession.encryptMessage(message)
		e2e.keyStore.storeOutboundSession(roomId, olmSession, olmSessionTimestamp)
		return@encrypted encrypted
	} ?: throw OlmEncryptionException()
}


open class OlmInboundGroupSessionWrapper(
		val e2e : OlmE2E,
		val olmSession : OlmInboundGroupSession
) : E2EInboundGroupSession
{
	override val sessionId : String
		get() = olmSession.sessionIdentifier()
	
	@Throws(MatrixE2EException::class)
	override fun decrypt(message : String) : String = wrapOlmEx decrypted@ {
		val decrypted = olmSession.decryptMessage(message)
		e2e.keyStore.storeInboundSession(olmSession)
		return@decrypted decrypted
	}?.decryptedMessage ?: throw OlmDecryptionException()
	
	@Throws(MatrixOlmException::class)
	override fun export() : E2EInboundGroupSessionExport = wrapOlmEx {
		E2EInboundGroupSessionExport(
				olmSession.export(olmSession.firstKnownIndex),
				olmSession.firstKnownIndex
		)
	}
}
