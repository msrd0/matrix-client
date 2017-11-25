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
@file:JvmName("Util")
package msrd0.matrix.client.e2e

import com.beust.klaxon.JsonObject
import org.matrix.olm.*

/**
 * Map an object to the specified key using the dot syntax
 *
 * This method allows one to map an object nested in subobjects without creating an instance for each of the subobjects.
 * Therefore the key ```top.level1.level2``` is converted into ```"top" : { "level1" : { "level2" : <object>}}```
 */
fun JsonObject.mapNested(key : String, data : Any?)
{
	val subKey = key.substringBefore(".")
	if (subKey != key)
	{
		val newKey = key.substringAfter(".")
		val json = JsonObject()
		this[subKey] = json
		json.mapNested(newKey, data)
	}
	else
		this[key] = data
}

fun JsonObject.getNested(key : String) : Any?
{
	val subKey = key.substringBefore(".")
	if (subKey != key)
	{
		val newKey = key.substringAfter(".")
		val json : JsonObject = this[subKey] as JsonObject
		return json.getNested(newKey)
	}
	else
		return this[subKey]
}

val olm : OlmManager = OlmManager()
val utility : OlmUtility = OlmUtility()

fun verifySignature(signature : String, deviceKey : String, message : String) : Boolean
{
	try
	{
		utility.verifyEd25519Signature(signature, deviceKey, message)
	}
	catch (e : OlmException)
	{
		return false
	}
	return true
}

fun getErrorMessageJson() : JsonObject
{
	val json = JsonObject()
	json["type"] = "m.room.message"
	json.mapNested("content.msgtype","m.bad.encrypted")
	json.mapNested("content.body", "** Unable to decrypt: The sender's device has not sent us the keys for this message. **")
	return json
}