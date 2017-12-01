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

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.util.JsonSerializable

class OneTimeKey
@JvmOverloads constructor(
		val userId : MatrixId,
		val deviceId : String,
		val algorithm : String,
		val keyId : String,
		val key : String,
		val signatures : KeySignatures = KeySignatures()
) : JsonSerializable
{
	@Throws(IllegalJsonException::class)
	constructor(userId : MatrixId, deviceId : String, keyName : String, json : JsonObject) : this(
			userId, deviceId,
			keyName.substring(0, keyName.indexOf(':')),
			keyName.substring(keyName.indexOf(':') + 1),
			json.string("key") ?: missing("key"),
			KeySignatures(json.obj("signatures") ?: missing("signatures"))
	)
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["key"] = key
		json["signatures"] = signatures.json
		return json
	}
}
