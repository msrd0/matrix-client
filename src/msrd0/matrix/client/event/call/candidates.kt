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

package msrd0.matrix.client.event.call

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.util.JsonSerializable

data class CallCandidate(
		val candidate : String,
		val sdpMid : String,
		val sdpMLineIndex : Int
) : JsonSerializable
{
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject)
		: this(json.string("candidate") ?: missing("candidate"), json.string("sdpMid") ?: missing("sdpMid"),
			json.int("sdpMLineIndex") ?: missing("sdpMLineIndex"))
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["candidate"] = candidate
		// matrix inconsistencies - these are really uppercased
		json["sdpMid"] = sdpMid
		json["sdpMLineIndex"] = sdpMLineIndex
		return json
	}
}

/**
 * The content of a call candidates event.
 */
class CallCandidatesEventContent(
		callId : String,
		version : Int,
		val candidates : List<CallCandidate>
) : CallEventContent(callId, version)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject)
				= CallCandidatesEventContent(
					json.string("call_id") ?: missing("call_id"),
					json.int("version") ?: missing("version"),
					json.array<JsonObject>("candidates")?.map { CallCandidate(it) } ?: missing("candidates")
				)
	}
	
	override val json : JsonObject get()
	{
		val json = super.json
		json["candidates"] = JsonArray(candidates.map { it.json })
		return json
	}
}

/**
 * A call candidates event.
 */
class CallCandidatesEvent
@Throws(IllegalJsonException::class)
constructor(room : Room, json : JsonObject)
	: CallEvent<CallCandidatesEventContent>(room, json,
		CallCandidatesEventContent.fromJson(json.obj("content") ?: missing("content")))
