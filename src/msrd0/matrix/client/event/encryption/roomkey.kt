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
 * The content of a room key event.
 */
class RoomKeyEventContent(
		val algorithm : String,
		val roomId : RoomId,
		val sessionId : String,
		val sessionKey : String
) : MatrixEventContent()
{
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			json.string("algorithm") ?: missing("algorithm"),
			json.string("room_id")?.let { RoomId.fromString(it) } ?: missing("room_id"),
			json.string("session_id") ?: missing("session_id"),
			json.string("session_key") ?: missing("session_key")
	)
	
	override val json : JsonObject get() = JsonObject(mapOf(
			"algorithm" to algorithm,
			"room_id" to "$roomId",
			"session_id" to sessionId,
			"session_key" to sessionKey
	))
}

/**
 * A room key event. This is NOT a room event. It is sent directly to a device, usually encapsulated in an encrypted
 * event.
 */
class RoomKeyEvent(
		data : MatrixEventData,
		content : RoomKeyEventContent
) : MatrixToDeviceEvent<RoomKeyEventContent>(data, content)
{
	constructor(json : JsonObject, content : RoomKeyEventContent)
			: this(MatrixEventData(json), content)
}
