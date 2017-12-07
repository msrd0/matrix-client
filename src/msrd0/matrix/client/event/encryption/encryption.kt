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
class RoomEncryptionEventContent(
		val algorithm : String,
		val rotationPeriodMsgs : Int = DEFAULT_ROTATION_PERIOD_MSGS,
		val rotationPeriodMs : Long = DEFAULT_ROTATION_PERIOD_MS
) : MatrixEventContent()
{
	companion object
	{
		// see https://matrix.org/docs/guides/e2e_implementation.html#rotating-megolm-sessions
		@JvmField val DEFAULT_ROTATION_PERIOD_MSGS = 100
		@JvmField val DEFAULT_ROTATION_PERIOD_MS = 604800000L /* 1w */
	}
	
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			json.string("algorithm") ?: missing("algorithm"),
			json.int("rotation_period_msgs") ?: DEFAULT_ROTATION_PERIOD_MSGS,
			json.long("rotation_period_ms") ?: DEFAULT_ROTATION_PERIOD_MS
	)
	
	override val json : JsonObject get() = JsonObject(mapOf(
			"algorithm" to algorithm,
			"rotation_period_msgs" to rotationPeriodMsgs,
			"rotation_period_ms" to rotationPeriodMs
	))
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
