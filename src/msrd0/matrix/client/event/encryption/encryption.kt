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

package msrd0.matrix.client.event.encryption

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.event.MatrixEventTypes.*

/**
 * The content of a room encryption event.
 */
class RoomEncryptionEventContent(val algorithm : String) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room encryption event content by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : RoomEncryptionEventContent
				= RoomEncryptionEventContent(json.string("algorithm") ?: missing("algorithm"))
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf("algorithm" to algorithm))
}

/**
 * A room encryption event.
 */
class RoomEncryptionEvent(
		room : Room,
		data : MatrixEventData,
		content : RoomEncryptionEventContent
) : MatrixRoomEvent<RoomEncryptionEventContent>(room, data, content)
{
	constructor(room : Room, json : JsonObject, content : RoomEncryptionEventContent)
			: this(room, MatrixEventData(json), content)
}
