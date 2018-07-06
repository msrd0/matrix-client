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

package de.msrd0.matrix.client.filter

import com.beust.klaxon.*
import de.msrd0.matrix.client.MatrixId
import de.msrd0.matrix.client.util.JsonSerializable

open class EventFilter : JsonSerializable
{
	/** A list of event types to exclude. A matching type will be excluded even if in `types`. Wildcards can be used. */
	var notTypes = ArrayList<String>()
	/** A list of event types to include. Wildcards can be used. */
	var types = ArrayList<String>()
	/** A list of sender IDs to exclude. A matching sender will be excluded even if in `senders`. */
	var notSenders = ArrayList<MatrixId>()
	/** A list of sender IDs to include. */
	var senders = ArrayList<MatrixId>()
	/** The maximum number of events to return. */
	var limit = -1
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		if (notTypes.isNotEmpty())
			json["not_types"] = JsonArray(notTypes)
		if (types.isNotEmpty())
			json["types"] = JsonArray(types)
		if (notSenders.isNotEmpty())
			json["not_senders"] = JsonArray(notSenders.map { it.toString() })
		if (senders.isNotEmpty())
			json["senders"] = JsonArray(senders.map { it.toString() })
		if (limit != -1)
			json["limit"] = limit
		return json
	}
}
