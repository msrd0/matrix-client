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
 * The content of a room join rules event.
 */
class RoomJoinRulesEventContent(val joinRule : String) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room join rules event content by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : RoomJoinRulesEventContent
				= RoomJoinRulesEventContent(json.string("join_rule") ?: missing("join_rule"))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("join_rule" to joinRule))
}

/**
 * A room join rules event.
 */
class RoomJoinRulesEvent
@Throws(IllegalJsonException::class)
constructor(room : MatrixRoom, json : JsonObject)
	: MatrixRoomEvent<RoomJoinRulesEventContent>(room, json,
		RoomJoinRulesEventContent.fromJson(json.obj("content") ?: missing("content")))
