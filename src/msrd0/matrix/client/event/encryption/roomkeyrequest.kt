/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
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
import msrd0.matrix.client.RoomId
import msrd0.matrix.client.event.MatrixEventContent
import msrd0.matrix.client.missing

/**
 * The content of a room key request event.
 */
class RoomKeyRequestEventContent(
		val action : String,
		val requestingDeviceId : String,
		val requestId : String,
		val roomId : RoomId? = null,
		val algorithm : String? = null,
		val senderKey : String? = null,
		val sessionId : String? = null
) : MatrixEventContent()
{
	private constructor(action : String, requestingDeviceId : String, requestId : String, body : JsonObject?) : this(
			action, requestingDeviceId, requestId,
			body?.string("room_id")?.let { RoomId.fromString(it) },
			body?.string("algorithm"),
			body?.string("sender_key"),
			body?.string("session_id")
	)
	
	constructor(json : JsonObject) : this(
			json.string("action") ?: missing("action"),
			json.string("requesting_device_id") ?: missing("requesting_device_id"),
			json.string("request_id") ?: missing("request_id"),
			json.obj("body")
	)
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["action"] = action
		json["requesting_device_id"] = requestingDeviceId
		json["request_id"] = requestId
		val body = JsonObject()
		if (roomId != null)
			body["room_id"] = "$roomId"
		if (algorithm != null)
			body["algorithm"] = algorithm
		if (senderKey != null)
			body["sender_key"] = senderKey
		if (sessionId != null)
			body["session_id"] = sessionId
		if (body.isNotEmpty())
			json["body"] = body
		return json
	}
}
