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

package msrd0.matrix.client.filter

import com.beust.klaxon.*
import msrd0.matrix.client.*

class RoomFilter : JsonSerializable
{
	/** Include rooms that the user has left in the sync. */
	var includeLeave : Boolean = false
	/** The per user account data to include for rooms. */
	var accountData = RoomEventFilter()
	/** The messages and state update events to include for rooms. */
	var timeline = RoomEventFilter()
	/** The events that aren't recorded in the room history, e.g. typing and receipts, to include for rooms. */
	var ephemeral = RoomEventFilter()
	/** The state events to include for rooms. */
	var state = RoomEventFilter()
	/** A list of room IDs to exclude. A matching room will be excluded even if listed in `rooms`. */
	var notRooms = ArrayList<RoomId>()
	/** A list of room IDs to include. */
	var rooms = ArrayList<RoomId>()
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["include_leave"] = includeLeave
		json["account_data"] = accountData.json
		json["timeline"] = timeline.json
		json["ephemeral"] = ephemeral.json
		json["state"] = state.json
		if (notRooms.isNotEmpty())
			json["not_rooms"] = JsonArray(notRooms.map { it.toString() })
		if (rooms.isNotEmpty())
			json["rooms"] = JsonArray(rooms.map { it.toString() })
		return json
	}
}
