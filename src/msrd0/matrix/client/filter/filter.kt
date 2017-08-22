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

package msrd0.matrix.client.filter

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.MatrixClient.Companion.checkForError
import msrd0.matrix.client.util.JsonSerializable

enum class EventFormat
{
	CLIENT,
	FEDERATION
}

class Filter : JsonSerializable
{
	/** A list of event fields to include. Entries may include '.' to indicate sub-fields. */
	val eventFields = emptyArray<String>()
	/** The format to use for events. */
	val eventFormat = EventFormat.CLIENT
	/** The user account data that isn't associated with rooms to include. */
	val accountData = EventFilter()
	/** Filters to be applied to room data. */
	val room = RoomFilter()
	/** The presence updates to include. */
	val presence = EventFilter()
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		if (eventFields.isNotEmpty())
			json["event_fields"] = JsonArray(eventFields)
		json["event_format"] = eventFormat.name.toLowerCase()
		json["account_data"] = accountData.json
		json["room"] = room.json
		json["presence"] = presence.json
		return json
	}
	
	/**
	 * Upload the filter to the server.
	 *
	 * @return The filter id assigned by the server.
	 * @throws MatrixAnswerException On errors in the answer from the server.
	 */
	@Throws(MatrixAnswerException::class)
	fun upload(client : MatrixClient) : String
	{
		val res = client.target.post("_matrix/client/r0/user/${client.id}/filter",
				client.token ?: throw NoTokenException(), client.id, json)
		checkForError(res)
		return res.json.string("filter_id") ?: throw IllegalJsonException("Missing: 'filter_id'")
	}
}

@Throws(MatrixAnswerException::class)
fun MatrixClient.uploadFilter(filter : Filter) : String
		 = filter.upload(this)
