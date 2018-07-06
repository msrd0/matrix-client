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

package de.msrd0.matrix.client.event

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.util.JsonSerializable
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.*

/**
 * The superclass of every event content.
 */
abstract class MatrixEventContent : JsonSerializable

data class MatrixEventData(
		val eventId : String,
		val sender : MatrixId,
		val type : String,
		val timestamp : LocalDateTime
)
{
	constructor(json : JsonObject) : this(
			json.string("event_id") ?: missing("event_id"),
			MatrixId.fromString(json.string("sender") ?: missing("sender")),
			json.string("type") ?: missing("type"),
			LocalDateTime.now().minus(json.long("age") ?: json.obj("unsigned")?.long("age") ?: missing("age"), MILLIS)
	)
}

/**
 * The superclass of all matrix events. For room events, use [MatrixRoomEvent].
 */
abstract class MatrixEvent<out C : MatrixEventContent>
constructor(
		val data : MatrixEventData,
		val content : C
) : JsonSerializable
{
	constructor(json : JsonObject, content : C)
			: this(MatrixEventData(json), content)
	
	val eventId get() = data.eventId
	val sender get() = data.sender
	val type get() = data.type
	val timestamp get() = data.timestamp
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["event_id"] = eventId
		json["sender"] = sender.toString()
		json["type"] = type
		json["content"] = content.json
		return json
	}
}

abstract class MatrixRoomEvent<out C : MatrixEventContent>(
		val room : Room,
		data : MatrixEventData,
		content : C
) : MatrixEvent<C>(data, content)
{
	constructor(room : Room, json : JsonObject, content : C)
			: this(room, MatrixEventData(json), content)
	
	val roomId get() = room.id
	
	override val json : JsonObject get()
	{
		val json = super.json
		json["room_id"] = "$roomId"
		return json
	}
}
