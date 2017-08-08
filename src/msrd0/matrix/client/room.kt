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
import msrd0.matrix.client.event.MatrixEventTypes.*
import msrd0.matrix.client.event.state.*
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
	/** The topic of this room or an empty string. */
	var topic : String = ""
		protected set
	/** The members of this room. */
	val members = ArrayList<MatrixId>()
	
	init // TODO rather than pulling some properties always and some never a cache would be cool
	{
		try { retrieveName() }
		catch (ex : MatrixErrorResponseException)
		{
			if (ex.errcode == "M_NOT_FOUND")
				/* The room does not have a name */
			else
				logger.warn("Failed to retrieve room name", ex)
		}
		
		try { retrieveTopic() }
		catch (ex : MatrixErrorResponseException)
		{
			if (ex.errcode == "M_NOT_FOUND")
			/* The room does not have a topic */
			else
				logger.warn("Failed to retrieve room topic", ex)
		}
		
		retrieveMembers()
	}
	
	/**
	 * Retrieves the room's name.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveName()
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_NAME", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		val content = RoomNameEventContent.fromJson(res.json)
		name = content.name
	}
	
	/**
	 * Update the name of this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateName(name : String)
	{
		sendStateEvent(ROOM_NAME, RoomNameEventContent(name))
		this.name = name
	}
	
	/**
	 * Retrieves the room's topic.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveTopic()
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_TOPIC", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		val content = RoomTopicEventContent.fromJson(res.json)
		topic = content.topic
	}
	
	/**
	 * Update the topic of this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateTopic(topic : String)
	{
		sendStateEvent(ROOM_TOPIC, RoomTopicEventContent(topic))
		this.topic = topic
	}
	
	/**
	 * Retrieves the room's members.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveMembers()
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/members", client.token ?: throw NoTokenException(), client.id)
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
		return Messages(json.string("start")!!, json.string("end")!!,
				chunk.filter{ it.string("type") == "m.room.message" }.map { Message.fromJson(this, it) })
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
	 * Send a state update event. This method should not be used to send messages.
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
	
	/**
	 * Retrieve the alias list for this room of the given domain. Right now the matrix api doesn't allow to query
	 * all aliases of all domains.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrieveAliases(domain : String) : List<RoomAlias>
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_ALIASES/$domain",
				client.token ?: throw NoTokenException(), client.id)
		if (res.status.status == 404 && res.json.string("errcode") == "M_NOT_FOUND")
			return emptyList()
		checkForError(res)
		
		val content = RoomAliasesEventContent.fromJson(res.json)
		return content.aliases
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
	 * Retrieve the canonical alias for this room, or null if it doesn't exist.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrieveCanonicalAlias() : RoomAlias?
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_CANONICAL_ALIAS",
				client.token ?: throw NoTokenException(), client.id)
		if (res.status.status == 404 && res.json.string("errcode") == "M_NOT_FOUND")
			return null
		
		val content = RoomCanonicalAliasEventContent.fromJson(res.json)
		return content.alias
	}
	
	/**
	 * Update the canonical alias for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateCanonicalAlias(alias : RoomAlias)
			= sendStateEvent(ROOM_CANONICAL_ALIAS, RoomCanonicalAliasEventContent(alias))
	
	/**
	 * Retrieve the power levels for this room. If no such event was found, the default values are returned.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrievePowerLevels() : RoomPowerLevels
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_POWER_LEVELS",
				client.token ?: throw NoTokenException(), client.id)
		if (res.status.status == 404 && res.json.string("errcode") == "M_NOT_FOUND")
			return RoomPowerLevels()
		
		return RoomPowerLevels.fromJson(res.json)
	}
	
	/**
	 * Update the power levels for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updatePowerLevels(powerLevels : RoomPowerLevels)
			= sendStateEvent(ROOM_POWER_LEVELS, powerLevels)
	
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
		val powerLevels = retrievePowerLevels()
		promotions.forEach { user, level -> powerLevels.users[user] = level }
		updatePowerLevels(powerLevels)
	}
	
	/**
	 * Retrieve the join rule for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrieveJoinRule() : String
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_JOIN_RULES",
				client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		val content = RoomJoinRulesEventContent.fromJson(res.json)
		return content.joinRule
	}
	
	/**
	 * Update the join rule for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateJoinRule(joinRule : String)
			= sendStateEvent(ROOM_JOIN_RULES, RoomJoinRulesEventContent(joinRule))
	
	/**
	 * Retrieve the history visibility for this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrieveHistoryVisibility() : String
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_HISTORY_VISIBILITY",
				client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
		
		val content = RoomHistoryVisibilityEventContent.fromJson(res.json)
		return content.historyVisibility
	}
	
	/**
	 * Update the history visibility of this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateHistoryVisibility(historyVisibility : String)
			= sendStateEvent(ROOM_HISTORY_VISIBILITY, RoomHistoryVisibilityEventContent(historyVisibility))
	
	
	/**
	 * Retrieve the avatar of this room. Returns null if there is no avatar.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun retrieveAvatar() : Avatar?
	{
		val res = client.target.get("_matrix/client/r0/rooms/$id/state/$ROOM_AVATAR",
				client.token ?: throw NoTokenException(), client.id)
		if (res.status.status == 404 && res.json.string("errcode") == "M_NOT_FOUND")
			return null
		checkForError(res)
		
		return Avatar.fromJson(res.json)
	}
	
	/**
	 * Update the avatar of this room.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateAvatar(avatar : Avatar)
			= sendStateEvent(ROOM_AVATAR, avatar)
}
