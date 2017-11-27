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

import msrd0.matrix.client.RoomId
import org.matrix.olm.*
import java.time.LocalDateTime

interface KeyStore
{
	/** Store/Retrieve the [OlmAccount] stored in this key store. */
	var account : OlmAccount
		@Throws(OlmException::class) get
		@Throws(OlmException::class) set
	
	/** Return true if this key store stores an [OlmAccount]. */
	fun hasAccount() : Boolean
	
	/** Store the outbound [session] with its [timestamp] for the [room]. */
	@Throws(OlmException::class)
	fun storeOutboundSession(room : RoomId, session : OlmOutboundGroupSession, timestamp : LocalDateTime)
	
	/** Find the stored outbound session for the [room] and its timestamp or return null. */
	@Throws(OlmException::class)
	fun findOutboundSession(room : RoomId) : Pair<OlmOutboundGroupSession, LocalDateTime>?
}


open class InMemoryKeyStore : KeyStore
{
	protected var _account : OlmAccount? = null
	
	override var account : OlmAccount
		get() = _account ?: throw IllegalStateException()
		set(value) { _account = value }
	
	override fun hasAccount() = _account != null
	
	
	protected var _outboundSessions = HashMap<RoomId, Pair<OlmOutboundGroupSession, LocalDateTime>>()
	
	override fun storeOutboundSession(room : RoomId, session : OlmOutboundGroupSession, timestamp : LocalDateTime)
	{
		_outboundSessions[room] = session to timestamp
	}
	
	override fun findOutboundSession(room : RoomId) : Pair<OlmOutboundGroupSession, LocalDateTime>?
			= _outboundSessions[room]
}
