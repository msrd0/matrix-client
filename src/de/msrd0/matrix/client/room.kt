/*
 * matrix-client
 * Copyright (C) 2017-2018 Dominic Meiser
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

package de.msrd0.matrix.client

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.MatrixClient.Companion.checkForError
import de.msrd0.matrix.client.event.*
import de.msrd0.matrix.client.event.MatrixEventTypes.*
import de.msrd0.matrix.client.event.state.*
import org.slf4j.*

/**
 * This class represents a matrix room.
 */
open class Room(
		val client : MatrixClient,
		val id : RoomId
) : RoomCache()
{
	override fun toString() = "Room(name=$name, id=$id)"
	
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(Room::class.java)
	}
	
	/**
	 * Retrieve a state event. If the event was found, its json is returned. Otherwise, null is returned.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun retrieveStateEvent(eventType : String, stateKey : String = "", canBeNotFound : Boolean = true) : JsonObject?
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$eventType/$stateKey",
				client.token ?: throw NoTokenException(), client.id)
		if (canBeNotFound && res.status.status == 404 && res.json.string("errcode") == "M_NOT_FOUND")
			return null
		checkForError(res)
		return res.json
	}
	
	/**
	 * Send a state event. This method should not be used to send messages.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun sendStateEvent(eventType : String, content : MatrixEventContent, stateKey : String = "")
	{
		val res = client.target.put("_matrix/client/r0/rooms/$id/state/$eventType/$stateKey",
				client.token ?: throw NoTokenException(), client.id, content.json)
		checkForError(res)
	}
	
	/** The name of this room or it's id. */
	var name : String by RoomEventDelegate(
			{ RoomNameEventContent.fromJson(retrieveStateEvent(ROOM_NAME) ?: return@RoomEventDelegate id.id).name },
			{ sendStateEvent(ROOM_NAME, RoomNameEventContent(it))}
	)
	
	/** The topic of this room or an empty string. */
	var topic : String by RoomEventDelegate(
			{ RoomTopicEventContent.fromJson(retrieveStateEvent(ROOM_TOPIC) ?: return@RoomEventDelegate "").topic },
			{ sendStateEvent(ROOM_TOPIC, RoomTopicEventContent(it)) }
	)
	
	/** The avatar of this room or null. */
	var avatar : Avatar? by RoomEventDelegate(
			{ Avatar.fromJson(retrieveStateEvent(ROOM_AVATAR) ?: return@RoomEventDelegate null) },
			{ sendStateEvent(ROOM_AVATAR, it!!) }
	)
	
	/** All room aliases sorted by their homeserver. */
	val aliases by RoomEventStateKeyDelegate<List<RoomAlias>>(
			{ hs -> RoomAliasesEventContent.fromJson(retrieveStateEvent(ROOM_ALIASES, hs) ?: return@RoomEventStateKeyDelegate emptyList()).aliases },
			{ hs, aliasList -> sendStateEvent(ROOM_ALIASES, RoomAliasesEventContent(aliasList), hs) }
	)
	
	/** The canonical alias of this room or null. */
	var canonicalAlias : RoomAlias? by RoomEventDelegate(
			{ RoomCanonicalAliasEventContent.fromJson(retrieveStateEvent(ROOM_CANONICAL_ALIAS) ?: return@RoomEventDelegate null).alias },
			{ sendStateEvent(ROOM_CANONICAL_ALIAS, RoomCanonicalAliasEventContent(it!!)) }
	)
	
	/** The power levels of this room. */
	// TODO the power levels should be updated when we change the respective properties, not only through assignment
	var powerLevels : RoomPowerLevels by RoomEventDelegate(
			{ RoomPowerLevels.fromJson(retrieveStateEvent(ROOM_POWER_LEVELS) ?: return@RoomEventDelegate RoomPowerLevels()) },
			{ sendStateEvent(ROOM_POWER_LEVELS, it) }
	)
	
	/** The join rule of this room. */
	var joinRule : String by RoomEventDelegate(
			{ RoomJoinRulesEventContent.fromJson(retrieveStateEvent(ROOM_JOIN_RULES, canBeNotFound = false)!!).joinRule },
			{ sendStateEvent(ROOM_JOIN_RULES, RoomJoinRulesEventContent(it)) }
	)
	
	/** The history visibility of this room. */
	var historyVisibility : String by RoomEventDelegate(
			{ RoomHistoryVisibilityEventContent.fromJson(retrieveStateEvent(ROOM_HISTORY_VISIBILITY, canBeNotFound = false)!!).historyVisibility },
			{ sendStateEvent(ROOM_HISTORY_VISIBILITY, RoomHistoryVisibilityEventContent(it)) }
	)
	
	/** The members of this room. */
	val members : List<MatrixId> by lazy { retrieveMembers() }
	
	/**
	 * Retrieves the room's members.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveMembers() : List<MatrixId>
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/members", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		val members = ArrayList<MatrixId>()
		
		val chunk = res.json.array<JsonObject>("chunk") ?: missing("chunk")
		for (member in chunk)
		{
			val content = member.obj("content") ?: missing("chunk.[].content")
			val membership = content.string("membership") ?: throw IllegalJsonException("Missing: 'chunk.[].content.membership")
			if (membership != "join")
				continue
			
			val userId = member.string("state_key") ?: throw IllegalJsonException("Missing: 'chunk.[].state_key")
			members.add(MatrixId.fromString(userId))
		}
		
		return members
	}
	
	/** The previous batch of the last retrieveMessage call. */
	var prev_batch : String? = null
		private set
	
	/**
	 * Retrieves the last `limit` messages, starting now or, if supplied, at `start`.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun retrieveMessages(limit : Int = 10, start : String? = null) : Messages
	{
		val params = HashMap<String, String>()
		params["access_token"] = client.token ?: throw NoTokenException()
		params["dir"] = "b"
		if (start != null)
			params["from"] = start
		params["limit"] = "$limit"
		val res = client.target.get("_matrix/client/r0/rooms/$id/messages", params)
		checkForError(res)
		
		val json = res.json
		prev_batch = json.string("end")
		val chunk = json.array<JsonObject>("chunk")!!
		return Messages(json.string("start")!!, json.string("end")!!, this, chunk)
	}
	
	/**
	 * Send the message to this room. This message is sent as plain text. There is no encryption going on.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun sendMessage(msg : MessageContent)
	{
		val res = client.target.put("_matrix/client/r0/rooms/$id/send/m.room.message/${client.nextTxnId}",
				client.token ?: throw NoTokenException(), client.id, msg.json)
		checkForError(res)
	}
	
	
	/**
	 * Add a new alias for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun addAlias(alias : RoomAlias)
	{
		val res = client.target.put("_matrix/client/r0/directory/room/$alias", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf("room_id" to "$id")))
		checkForError(res)
	}
	
	/**
	 * Invite a user with known matrix id to this room. Please note that the user will not participate in the room until
	 * he hasn't accepted the invitation.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun invite(user : MatrixId)
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/invite", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf("user_id" to "$user")))
		checkForError(res)
	}
	
	/**
	 * Invite a user with a third-party address to this room. Please note that the user will not participate in the room
	 * until he hasn't accepted the invitation.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun invite(address : String, medium : String = "email", idServer : String = "vector.im")
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/invite", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf(
					"address" to address,
					"medium" to medium,
					"id_server" to idServer
				)))
		checkForError(res)
	}
	
	/**
	 * Promote (or demote) a user in this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun promote(user : MatrixId, powerLevel : Int)
			= promote(mapOf(user to powerLevel))
	
	/**
	 * Promote (or demote) a list of users in this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun promote(promotions : Map<MatrixId, Int>)
	{
		val pl = powerLevels
		promotions.forEach { user, level -> pl.users[user] = level }
		powerLevels = pl
	}
	
	/**
	 * Kick a user from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun kick(user : MatrixId, reason : String = "")
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/kick", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf(
					"user_id" to "$user",
					"reason" to reason
				)))
		checkForError(res)
	}
	/**
	 * Kick a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun kick(users : Iterable<MatrixId>, reason : String = "")
			= users.forEach { kick(it, reason) }
	/**
	 * Kick a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun kick(vararg users : MatrixId, reason : String = "")
			= users.forEach { kick(it, reason) }
	
	/**
	 * Ban a user from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun ban(user : MatrixId, reason : String = "")
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/ban", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf(
					"user_id" to "$user",
					"reason" to reason
				)))
		checkForError(res)
	}
	/**
	 * Ban a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun ban(users : Iterable<MatrixId>, reason : String = "")
			= users.forEach { ban(it, reason) }
	/**
	 * Ban a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun ban(vararg users : MatrixId, reason : String = "")
			= users.forEach { ban(it, reason) }
	
	/**
	 * Unban a user from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun unban(user : MatrixId)
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/unban", client.token ?: throw NoTokenException(),
				client.id, JsonObject(mapOf(
					"user_id" to "$user"
				)))
		checkForError(res)
	}
	/**
	 * Unban a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun unban(users : Iterable<MatrixId>)
			= users.forEach { unban(it) }
	/**
	 * Unban a list of users from this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun unban(vararg users : MatrixId)
			= users.forEach { unban(it) }
	
	/**
	 * Leave this room. You will not be part of the room anymore, but you can still read the history of the room before
	 * you left.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun leave()
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/leave", client.token ?: throw NoTokenException(),
				client.id, JsonObject())
		checkForError(res)
	}
	
	/**
	 * Forget this room. If you are currently part of the room, you will leave the room. After forgetting this room,
	 * you will not be able to read the history of this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun forget()
	{
		val res = client.target.post("_matrix/client/r0/rooms/$id/forget", client.token ?: throw NoTokenException(),
				client.id, JsonObject())
		checkForError(res)
	}
}
