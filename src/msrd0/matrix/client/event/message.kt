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

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.*
import msrd0.matrix.client.event.EventTypes.ROOM_MESSAGE
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
	override fun getJson() : JsonObject
	{
		val json = JsonObject()
		json["body"] = body
		json["msgtype"] = msgtype
		return json
	}
}

class Message(
		val room : Room,
		sender : MatrixId,
		content : MessageContent
) : Event(sender, ROOM_MESSAGE, content)
{
	constructor(room : Room, sender : MatrixId, body : String, msgtype : String)
		: this(room, sender, MessageContent(body, msgtype))
	
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
{

}
