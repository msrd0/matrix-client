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

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.util.JsonSerializable

data class CallEventDescriptor(
		val type : String,
		val sdp : String
) : JsonSerializable
{
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject)
			: this(json.string("type") ?: missing("type"), json.string("sdp") ?: missing("sdp"))
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["type"] = type
		json["sdp"] = sdp
		return json
	}
}

/**
 * The content of a call event. Every call event has a call id and a version.
 */
abstract class CallEventContent(
		val callId : String,
		val version : Int
) : MatrixEventContent()
{
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["call_id"] = callId
		json["version"] = version
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
	val version get() = content.version
}
