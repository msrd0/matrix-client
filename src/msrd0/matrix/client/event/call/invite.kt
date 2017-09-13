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
import msrd0.matrix.client.util.JsonSerializable

data class CallInviteOffer(
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
 * The content of a call invite event.
 */
class CallInviteEventContent(
		callId : String,
		val offer : CallInviteOffer,
		val lifetime : Long,
		val version : Int
) : CallEventContent(callId)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject)
				= CallInviteEventContent(
					json.string("call_id") ?: missing("call_id"),
					CallInviteOffer(json.obj("offer") ?: missing("offer")),
					json.long("lifetime") ?: missing("lifetime"),
					json.int("version") ?: missing("version")
				)
	}
	
	override val json : JsonObject get()
	{
		val json = super.json
		json["offer"] = offer.json
		json["lifetime"] = lifetime
		json["version"] = version
		return json
	}
}

/**
 * A call invite event.
 */
class CallInviteEvent
@Throws(IllegalJsonException::class)
constructor(room : Room, json : JsonObject)
	: CallEvent<CallInviteEventContent>(room, json,
		CallInviteEventContent.fromJson(json.obj("content") ?: missing("content")))
