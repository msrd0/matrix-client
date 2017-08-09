/*
 matrix-client
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client.encryption

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.util.JsonSerializable

class DeviceKeySignatures() : HashMap<MatrixId, Map<String, String>>(), JsonSerializable
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : DeviceKeySignatures
		{
			val signatures = DeviceKeySignatures()
			for ((id, obj) in json.mapKeys { MatrixId.fromString(it.key) }.mapValues { it.value as JsonObject })
				signatures[id] = obj.mapValues { it.value as String }
			return signatures
		}
	}
	
	override val json : JsonObject get() = JsonObject(mapKeys { "${it.key}" })
}

class DeviceKeys(
		val userId : MatrixId,
		val deviceId : String,
		val algorithms : List<String>,
		val keys : Map<String, String>,
		val signatures : DeviceKeySignatures
) : JsonSerializable
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : DeviceKeys
				= DeviceKeys(
					userId = MatrixId.fromString(json.string("user_id") ?: throw IllegalJsonException("Missing: 'user_id'")),
					deviceId = json.string("device_id") ?: throw IllegalJsonException("Missing: 'device_id'"),
					algorithms = json.array("algorithms") ?: throw IllegalJsonException("Missing: 'algorithms'"),
					keys = json.obj("keys")?.mapValues { it.value as String } ?: throw IllegalJsonException("Missing: 'keys'"),
					signatures = DeviceKeySignatures.fromJson(json.obj("signatures") ?: throw IllegalJsonException("Missing: 'signatures'"))
				)
	}
	
	override val json : JsonObject get()
			= JsonObject(mapOf(
				"user_id" to "$userId",
				"device_id" to deviceId,
				"algorithms" to JsonArray(algorithms),
				"keys" to keys,
				"signatures" to signatures.json
			))
}
