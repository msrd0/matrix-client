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

package msrd0.matrix.client.event.state

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.event.MatrixEventTypes.*

/**
 * The content of a room name event.
 */
class RoomNameEventContent(val name : String) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room name event content by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : RoomNameEventContent
				= RoomNameEventContent(json.string("name") ?: throw IllegalJsonException("Missing: 'name'"))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("name" to name))
}

/**
 * A room name event.
 */
class RoomNameEvent(
		room : Room,
		sender : MatrixId,
		content : RoomNameEventContent
) : MatrixRoomEvent<RoomNameEventContent>(room, sender, ROOM_NAME, content)
{
	companion object
	{
		/**
		 * Constructs a room name event py parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(room : Room, json : JsonObject) : RoomNameEvent
				= RoomNameEvent(room, MatrixId.fromString(json.string("sender") ?: throw IllegalJsonException("Missing: 'sender'")),
					RoomNameEventContent.fromJson(json.obj("content") ?: throw IllegalJsonException("Missing: 'content'")))
	}
	
	override val json : JsonObject get() = abstractJson
}
