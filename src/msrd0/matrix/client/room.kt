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

package msrd0.matrix.client

import com.beust.klaxon.*
import msrd0.matrix.client.Client.Companion.checkForError
import msrd0.matrix.client.event.*
import org.slf4j.*

/**
 * This class represents a matrix room.
 */
open class Room(
		val client : Client,
		val id : RoomId
) {
	override fun toString() = "Room(name=$name, id=$id)"
	
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(Room::class.java)
	}
	
	/** The name of this room or it's id. */
	var name : String = id.id
		protected set
	val members = ArrayList<MatrixId>()
	
	init
	{
		try { retrieveName() }
		catch (ex : MatrixErrorResponseException)
		{
			if (ex.errcode == "M_NOT_FOUND")
				/* The room does not have a name */
			else
				logger.warn("Failed to retrieve room name", ex)
		}
		
		retrieveMembers()
	}
	
	/**
	 * Retrieves the room's name.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveName()
	{
		val res = client.target.get("/_matrix/client/r0/rooms/$id/state/m.room.name", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		name = res.json.string("name") ?: throw IllegalJsonException("Missing: 'name'")
	}
	
	/**
	 * Retrieves the room's members.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveMembers()
	{
		val res = client.target.get("/_matrix/client/r0/rooms/$id/members", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		members.clear()
		
		val chunk = res.json.array<JsonObject>("chunk") ?: throw IllegalJsonException("Missing: 'chunk'")
		for (member in chunk)
		{
			val content = member.obj("content") ?: throw IllegalJsonException("Missing: 'chunk.[].content'")
			val membership = content.string("membership") ?: throw IllegalJsonException("Missing: 'chunk.[].content.membership")
			if (membership != "join")
				continue
			
			val userid = member.string("state_key") ?: throw IllegalJsonException("Missing: 'chunk.[].state_key")
			members.add(MatrixId.fromString(userid))
		}
	}
	
	/** The previous batch of the last retrieveMessage call. */
	var prev_batch : String? = null
		private set
	
	/**
	 * Retrieves the last `limit` messages, starting now or, if supplied, at `start`.
	 */
	@Throws(MatrixAnswerException::class)
	@JvmOverloads
	fun retrieveMessages(limit : Int = 10, start : String? = null) : Messages
	{
		val params = HashMap<String, String>()
		params["access_token"] = client.token ?: throw NoTokenException()
		params["dir"] = "b"
		if (start != null)
			params["from"] = start
		params["limit"] = "$limit"
		val res = client.target.get("/_matrix/client/r0/rooms/$id/messages", params)
		checkForError(res)
		
		val json = res.json
		prev_batch = json.string("end")
		val chunk = json.array<JsonObject>("chunk")!!
		return Messages(json.string("start")!!, json.string("end")!!,
				chunk.filter{ it.string("type") == "m.room.message" }.map { Message.fromJson(this, it) })
	}
	
	/**
	 * Send the message to this room. This message is sent as plain text. There is no encryption going on.
	 */
	@Throws(MatrixAnswerException::class)
	fun sendMessage(msg : MessageContent)
	{
		val res = client.target.put("_matrix/client/r0/rooms/$id/send/m.room.message/${client.nextTxnId}",
				client.token ?: throw NoTokenException(), client.id, msg.json)
		checkForError(res)
	}
}
