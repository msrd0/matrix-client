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

package msrd0.matrix.client.e2e

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.util.*
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException

class DeviceKeySignatures() : HashMap<MatrixId, MutableMap<String, String>>(), JsonSerializable
{
	constructor(json : JsonObject) : this()
	{
		loadSignaturesFromJson(json)
	}
	
	fun loadSignaturesFromJson(json : JsonObject) : DeviceKeySignatures
	{
		for ((id, obj) in json.map { (id, obj) -> MatrixId.fromString(id) to (obj as JsonObject) })
			this[id] = obj.mapValues { it.value as String }.toMutableMap()
		return this
	}
	
	override val json : JsonObject get() = JsonObject(mapKeys { "${it.key}" })
}

class DeviceKeys
@JvmOverloads constructor(
		val userId : MatrixId,
		val deviceId : String,
		val algorithms : List<String>,
		val keys : Map<String, String>,
		val signatures : DeviceKeySignatures = DeviceKeySignatures()
) : JsonSerializable
{
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			userId = MatrixId.fromString(json.string("user_id") ?: missing("user_id")),
			deviceId = json.string("device_id") ?: missing("device_id"),
			algorithms = json.array("algorithms") ?: missing("algorithms"),
			keys = json.obj("keys")?.mapValues { it.value as String } ?: missing("keys"),
			signatures = DeviceKeySignatures(json.obj("signatures") ?: missing("signatures"))
	)
	
	@Throws(OlmException::class)
	fun sign(account : OlmAccount, id : MatrixId, deviceId : String)
	{
		val toSign = json
		toSign.remove("signatures")
		val signature = account.signMessage(toSign.toJsonString(canonical = true))
		if (!signatures.containsKey(id))
			signatures[id] = emptyMutableMap()
		signatures[id]!!["ed25519:$deviceId"] = signature
	}
	
	/**
	 * Check the self-signatures of this device key. Signatures from a third-party account cannot be verified
	 * because we don't know their keys.
	 *
	 * @return False if the signature is invalid or there is no signature and [forceSignature] is set to true.
	 * @throws OlmException On errors while checking the signatures.
	 */
	@JvmOverloads
	@Throws(OlmException::class)
	fun checkSignatures(forceSignature : Boolean = true) : Boolean
	{
		val userSignatures = signatures[userId]
		val deviceSignature = userSignatures?.filter { (device, _) -> device.endsWith(deviceId) }?.entries?.firstOrNull()
			?: return !forceSignature // there is no self-signature
		val device = deviceSignature.key
		val signature = deviceSignature.value
		val jsonToVerify = json
		jsonToVerify.remove("signatures")
		return verifySignature(signature, keys[device] ?: return false,
				jsonToVerify.toJsonString(canonical = true))
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
