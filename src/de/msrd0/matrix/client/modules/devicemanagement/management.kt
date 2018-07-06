/*
 matrix-client
 Copyright (C) 2018 Dominic Meiser
 
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
@file:JvmName("DeviceManagement")
package de.msrd0.matrix.client.modules.devicemanagement

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.MatrixClient.Companion.checkForError

/**
 * Return all devices from the current user.
 *
 * @throws MatrixAnswerException On errors in the matrix answer.
 */
@Throws(MatrixAnswerException::class)
fun MatrixClient.devices() : List<Device>
{
	val res = target.get("_matrix/client/r0/devices", token ?: throw NoTokenException(), id)
	MatrixClient.checkForError(res)
	return res.json.array<JsonObject>("devices")
			?.map { Device(it) }
			?: missing("devices")
}

/**
 * Return a particular device from the current user.
 *
 * @throws MatrixAnswerException On errors in the matrix answer.
 */
@Throws(MatrixAnswerException::class)
fun MatrixClient.device(deviceId : String) : Device?
{
	val res = target.get("_matrix/client/r0/devices/$deviceId", token ?: throw NoTokenException(), id)
	if (res.status.status == 404)
		return null
	checkForError(res)
	return Device(res.json)
}

/**
 * Update the display name of a certain device of the current user.
 *
 * @throws MatrixAnswerException On errors in the matrix answer.
 */
@Throws(MatrixAnswerException::class)
fun MatrixClient.updateDeviceDisplayName(deviceId : String, displayName : String)
{
	val res = target.put("_matrix/client/r0/devices/$deviceId", token ?: throw NoTokenException(), id,
			JsonObject(mapOf("display_name" to displayName)))
	MatrixClient.checkForError(res)
}

/**
 * Delete a certain device of the current user. Note that this might require authentication with the
 * matrix server, although a valid access token is present. It is recommended to use at least the
 * `DefaultFlowHelper` with the password of the user.
 *
 * @throws MatrixAnswerException On errors in the matrix answer.
 * @throws UnsupportedFlowsException If the flow helper can't authenticate.
 */
@Throws(MatrixAnswerException::class, UnsupportedFlowsException::class)
fun MatrixClient.deleteDevice(deviceId : String, helper : FlowHelper = DefaultFlowHelper())
{
	val json = JsonObject()
	var res = target.delete("_matrix/client/r0/devices/$deviceId", json)
	
	while (res.status.status == 401 && res.json.containsKey("flows"))
	{
		val flowResponse = helper.answer(FlowRequest.fromJson(res.json))
		json["auth"] = flowResponse.json
		res = target.delete("_matrix/client/r0/devices/$deviceId", json)
	}
	
	MatrixClient.checkForError(res)
}
