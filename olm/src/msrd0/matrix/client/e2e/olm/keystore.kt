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
	
	/** Store the OLM session associated with the identity key. */
	@Throws(OlmException::class)
	fun storeSession(idKey : String, session : OlmSession)
	
	/** Return all stored OLM sessions for the given identity key. */
	@Throws(OlmException::class)
	fun allSessions(idKey : String) : Collection<OlmSession>
	
	/** Store the outbound [session] with its [timestamp] for the [room]. */
	@Throws(OlmException::class)
	fun storeOutboundSession(room : RoomId, session : OlmOutboundGroupSession, timestamp : LocalDateTime)
	
	/** Find the stored outbound session for the [room] and its timestamp or return null. */
	@Throws(OlmException::class)
	fun findOutboundSession(room : RoomId) : Pair<OlmOutboundGroupSession, LocalDateTime>?
	
	/** Store the inbound [session]. */
	@Throws(OlmException::class)
	fun storeInboundSession(session : OlmInboundGroupSession)
	
	/** Find the stored inbound session uniquely identified by the [sessionId] or return null. */
	@Throws(OlmException::class)
	fun findInboundSession(sessionId : String) : OlmInboundGroupSession?
}


open class InMemoryKeyStore : KeyStore
{
	protected var _account : OlmAccount? = null
	
	override var account : OlmAccount
		get() = _account ?: throw IllegalStateException()
		set(value) { _account = value }
	
	override fun hasAccount() = _account != null
	
	
	protected var _sessions = HashMap<String, MutableSet<OlmSession>>()
	
	override fun storeSession(idKey : String, session : OlmSession)
	{
		_sessions[idKey] = (_sessions[idKey] ?: HashSet()).apply { add(session) }
	}
	
	override fun allSessions(idKey : String) : Collection<OlmSession>
			= _sessions[idKey] ?: emptyList()
	
	
	protected var _outboundSessions = HashMap<RoomId, Pair<OlmOutboundGroupSession, LocalDateTime>>()
	
	override fun storeOutboundSession(room : RoomId, session : OlmOutboundGroupSession, timestamp : LocalDateTime)
	{
		_outboundSessions[room] = session to timestamp
	}
	
	override fun findOutboundSession(room : RoomId) : Pair<OlmOutboundGroupSession, LocalDateTime>?
			= _outboundSessions[room]
	
	
	protected var _inboundSessions = HashMap<String, OlmInboundGroupSession>()
	
	override fun storeInboundSession(session : OlmInboundGroupSession)
	{
		_inboundSessions[session.sessionIdentifier()] = session
	}
	
	override fun findInboundSession(sessionId : String) : OlmInboundGroupSession?
			= _inboundSessions[sessionId]
}
