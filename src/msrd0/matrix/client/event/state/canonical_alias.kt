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
import msrd0.matrix.client.event.MatrixEventTypes.ROOM_CANONICAL_ALIAS

/**
 * The content of a room canonical alias event.
 */
class RoomCanonicalAliasEventContent(val alias : RoomAlias) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room canonical alias event content by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : RoomCanonicalAliasEventContent
				= RoomCanonicalAliasEventContent(RoomAlias.fromString(
					json.string("alias") ?: throw IllegalJsonException("Missing: 'alias'")))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("alias" to alias.toString()))
}

/**
 * A room canonical alias event.
 */
class RoomCanonicalAliasEvent(
		room : Room,
		sender : MatrixId,
		content : RoomCanonicalAliasEventContent
) : MatrixRoomEvent<RoomCanonicalAliasEventContent>(room, sender, ROOM_CANONICAL_ALIAS, content)
{
	companion object
	{
		/**
		 * Constructs a room canonical alias event py parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(room : Room, json : JsonObject) : RoomCanonicalAliasEvent
				= RoomCanonicalAliasEvent(room, MatrixId.fromString(json.string("sender") ?: throw IllegalJsonException("Missing: 'sender'")),
					RoomCanonicalAliasEventContent.fromJson(json.obj("content") ?: throw IllegalJsonException("Missing: 'content'")))
	}
	
	override val json : JsonObject get() = abstractJson
}
