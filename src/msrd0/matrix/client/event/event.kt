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

package msrd0.matrix.client.event

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.*
import msrd0.matrix.client.util.JsonSerializable

abstract class MatrixEventContent : JsonSerializable

abstract class MatrixEvent<out C : MatrixEventContent>
@JvmOverloads
constructor(
		val sender : MatrixId,
		val type : String,
		val content : C,
		val timestamp : Long? = null,
		val eventId : String? = null
) : JsonSerializable
{
	val abstractJson : JsonObject get()
	{
		val json = JsonObject()
		json["sender"] = sender.toString()
		json["type"] = type
		json["content"] = content.json
		if (eventId != null)
			json["event_id"] = eventId
		return json
	}
}

abstract class MatrixRoomEvent<out C : MatrixEventContent>(
		val room : Room,
		sender : MatrixId,
		type : String,
		content : C
) : MatrixEvent<C>(sender, type, content)
