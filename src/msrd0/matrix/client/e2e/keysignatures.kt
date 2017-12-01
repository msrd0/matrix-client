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

package msrd0.matrix.client.e2e

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.MatrixId
import msrd0.matrix.client.util.JsonSerializable
import msrd0.matrix.client.util.toMutableMap

class KeySignatures() : HashMap<MatrixId, MutableMap<String, String>>(), JsonSerializable
{
	constructor(json : JsonObject) : this()
	{
		loadSignaturesFromJson(json)
	}
	
	fun loadSignaturesFromJson(json : JsonObject) : KeySignatures
	{
		for ((id, obj) in json.map { (id, obj) -> MatrixId.fromString(id) to (obj as JsonObject) })
			this[id] = obj.mapValues { it.value as String }.toMutableMap()
		return this
	}
	
	override val json : JsonObject get() = JsonObject(mapKeys { "${it.key}" })
}
