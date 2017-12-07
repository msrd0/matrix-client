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

import com.beust.klaxon.JsonBase

interface SessionWithId
{
	/**
	 * Return the session id.
	 */
	val sessionId : String
}

interface SessionWithKey
{
	/**
	 * Return the session key.
	 */
	val sessionKey : String
}

interface E2ESession : SessionWithId
{
	/**
	 * Encrypt the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun encrypt(message : String) : E2EMessage
	
	/**
	 * Encrypt the json [message].
	 */
	@Throws(MatrixE2EException::class)
	fun encrypt(message : JsonBase) : E2EMessage
			= encrypt(message.toJsonString(prettyPrint = false))
	
	/**
	 * Decrypt the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun decrypt(message : E2EMessage) : String
}

interface E2EOutboundGroupSession : SessionWithId, SessionWithKey
{
	/**
	 * Return true if this outbound session needs to be rotated.
	 *
	 * @param messages The maximum amount of messages that can be created by this session before it needs to be rotated.
	 * @param lifetime The maximum amount of milliseconds this session can life before it needs to be rotated.
	 */
	fun needsRotation(messages : Int, lifetime : Long) : Boolean
	
	/**
	 * Encrypt the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun encrypt(message : String) : String
	
	/**
	 * Encrypt the json [message].
	 */
	@Throws(MatrixE2EException::class)
	fun encrypt(message : JsonBase) : String
			= encrypt(message.toJsonString(prettyPrint = false))
}

data class E2EInboundGroupSessionExport(
		val export : String,
		val chainIndex : Long
)

interface E2EInboundGroupSession : SessionWithId
{
	/**
	 * Decrypt the [message].
	 */
	@Throws(MatrixE2EException::class)
	fun decrypt(message : String) : String
	
	/**
	 * Export the session.
	 */
	@Throws(MatrixE2EException::class)
	fun export() : E2EInboundGroupSessionExport
}
