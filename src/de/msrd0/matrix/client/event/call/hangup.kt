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

package de.msrd0.matrix.client.event.call

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*

/**
 * The content of a call hangup event.
 */
class CallHangupEventContent(
		callId : String,
		version : Int
) : CallEventContent(callId, version)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject)
				= CallHangupEventContent(
					json.string("call_id") ?: missing("call_id"),
					json.int("version") ?: missing("version")
				)
	}
}

/**
 * A call hangup event.
 */
class CallHangupEvent
@Throws(IllegalJsonException::class)
constructor(room : Room, json : JsonObject)
	: CallEvent<CallHangupEventContent>(room, json,
		CallHangupEventContent.fromJson(json.obj("content") ?: missing("content")))
