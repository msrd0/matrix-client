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

import msrd0.matrix.client.e2e.*
import org.matrix.olm.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MILLIS

open class OlmSessionWrapper(val olmSession : OlmSession) : E2ESession
{
	override val sessionId : String
		get() = olmSession.sessionIdentifier()
	
	@Throws(MatrixOlmException::class)
	override fun encrypt(message : String) : E2EMessage
			= wrapOlmEx { olmSession.encryptMessage(message) }.e2eMessage
	
	@Throws(MatrixOlmException::class)
	override fun decrypt(message : E2EMessage) : String
			= wrapOlmEx { olmSession.decryptMessage(message.olmMessage) }
}


open class OlmOutboundGroupSessionWrapper(
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
	override fun encrypt(message : String) : String
			= wrapOlmEx { olmSession.encryptMessage(message) } ?: throw OlmEncryptionException()
}


open class OlmInboundGroupSessionWrapper(val olmSession : OlmInboundGroupSession) : E2EInboundGroupSession
{
	override val sessionId : String
		get() = olmSession.sessionIdentifier()
	
	@Throws(MatrixE2EException::class)
	override fun decrypt(message : String) : String
			= wrapOlmEx { olmSession.decryptMessage(message)?.decryptedMessage } ?: throw OlmDecryptionException()
	
	@Throws(MatrixOlmException::class)
	override fun export() : E2EInboundGroupSessionExport = wrapOlmEx {
		E2EInboundGroupSessionExport(
				olmSession.export(olmSession.firstKnownIndex),
				olmSession.firstKnownIndex
		)
	}
}
