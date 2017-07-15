/*
 matrix-client
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client.event

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.EventTypes.ROOM_MESSAGE
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.*
import java.util.*

object MessageTypes
{
	@JvmField
	var TEXT = "m.text"
}

class MessageContent(
		val body : String,
		val msgtype : String
) : EventContent()
{
	companion object
	{
		/**
		 * Constructs a message content by parsing the supplied json. For a documentation of the json see the matrix
		 * specifications.
		 *
		 * @throws NullPointerException If one of the required json parameters were null (or not present).
		 */
		@JvmStatic
		fun fromJson(json : JsonObject) : MessageContent
				= MessageContent(json.string("body")!!, json.string("msgtype")!!)
	}
	
	override fun getJson() : JsonObject
	{
		val json = JsonObject()
		json["body"] = body
		json["msgtype"] = msgtype
		return json
	}
}

/**
 * This class represents a message in a room.
 */
class Message(
		val room : Room,
		sender : MatrixId,
		val age : LocalDateTime,
		content : MessageContent
) : Event(sender, ROOM_MESSAGE, content)
{
	constructor(room : Room, sender : MatrixId, age : LocalDateTime, body : String, msgtype : String)
		: this(room, sender, age, MessageContent(body, msgtype))
	
	companion object
	{
		/**
		 * Constructs a message by parsing the supplied json. For a documentation of the json see the matrix
		 * specifications.
		 *
		 * @throws NullPointerException If one of the required json parameters were null (or not present).
		 */
		@JvmStatic
		fun fromJson(room : Room, json : JsonObject) : Message
				= Message(room, MatrixId.fromString(json.string("sender")!!),
					LocalDateTime.now().minus(json.long("age")!!, MILLIS),
					MessageContent.fromJson(json.obj("content")!!))
	}
	
	val body get() = (content as MessageContent).body
	val msgtype get() = (content as MessageContent).msgtype
	
	override fun getJson() : JsonObject
	{
		val json = getAbstractJson()
		json["room_id"] = room.id.toString()
		return json
	}
}

class Messages(
		val start : String,
		val end : String,
		messages : Collection<Message> = Collections.emptyList()
) : ArrayList<Message>(messages)
