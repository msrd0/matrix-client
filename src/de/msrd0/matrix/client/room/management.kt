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

package de.msrd0.matrix.client.room

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.MatrixClient.Companion.checkForError

/**
 * Create a new room.
 *
 * @param name If not null, set the `m.room.name` event.
 * @param topic If not null, set the `m.room.topic` event.
 * @param public If true, this room will be published to the room list.
 * @return The created room.
 * @throws MatrixAnswerException On errors in the matrix answer.
 */
@Throws(MatrixAnswerException::class)
@JvmOverloads
fun MatrixClient.createRoom(name : String? = null, topic : String? = null, public : Boolean = false) : MatrixRoom
{
	val json = JsonObject()
	if (name != null)
		json["name"] = name
	if (topic != null)
		json["topic"] = topic
	json["preset"] = if (public) "public_chat" else "private_chat"
	val res = target.post("_matrix/client/r0/createRoom", token ?: throw NoTokenException(), id, json)
	checkForError(res)
	return MatrixRoom(this, MatrixRoomId(res.json.string("room_id") ?: missing("room_id")))
}