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

package msrd0.matrix.client.event

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.MatrixEventTypes.DEVICE_NEW

class NewDeviceEventContent(
		val device_id : String,
		val rooms : Collection<RoomId>
) : MatrixEventContent()
{
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["device_id"] = device_id
		json["rooms"] = JsonArray(rooms.map { it.toString() })
		return json
	}
}

/*
class NewDeviceEvent(
		sender : MatrixId,
		content : NewDeviceEventContent
) : MatrixEvent<NewDeviceEventContent>(sender, DEVICE_NEW, content)
{
	override val json : JsonObject
		get() = TODO("not implemented")
	
}
*/
