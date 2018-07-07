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

package de.msrd0.matrix.client.event.state

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.event.*
import de.msrd0.matrix.client.room.MatrixRoom

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
				= RoomNameEventContent(json.string("name") ?: missing("name"))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("name" to name))
}

/**
 * A room name event.
 */
class RoomNameEvent
@Throws(IllegalJsonException::class)
constructor(room : MatrixRoom, json : JsonObject)
	: MatrixRoomEvent<RoomNameEventContent>(room, json,
		RoomNameEventContent.fromJson(json.obj("content") ?: missing("content")))
