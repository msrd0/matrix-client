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

import com.beust.klaxon.*
import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.event.*

/**
 * The content of a room aliases event.
 */
class RoomAliasesEventContent(val aliases : List<RoomAlias>) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room aliases event by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : RoomAliasesEventContent
				= RoomAliasesEventContent(json.array<String>("aliases")?.map { RoomAlias.fromString(it) }
					?: missing("aliases"))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("aliases" to JsonArray(aliases.map { "$it" })))
}

/**
 * A room aliases event.
 */
class RoomAliasesEvent
@Throws(IllegalJsonException::class)
constructor(room : Room, json : JsonObject)
	: MatrixRoomEvent<RoomAliasesEventContent>(room, json,
		RoomAliasesEventContent.fromJson(json.obj("content") ?: missing("content")))
