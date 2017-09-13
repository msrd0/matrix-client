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

package msrd0.matrix.client.event.call

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.Room
import msrd0.matrix.client.event.*

/**
 * The content of a call event. Every call event has a call id.
 */
abstract class CallEventContent(
		val callId : String
) : MatrixEventContent()
{
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["call_id"] = callId
		return json
	}
}

/**
 * A call event.
 */
abstract class CallEvent<out C : CallEventContent>(
		room : Room,
		data : MatrixEventData,
		content : C
) : MatrixRoomEvent<C>(room, data, content)
{
	constructor(room : Room, json : JsonObject, content : C)
			: this(room, MatrixEventData(json), content)
	
	val callId get() = content.callId
}
