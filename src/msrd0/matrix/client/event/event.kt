/*
 * matrix-client
 * Copyright (C) 2017  Julius Lehmann
 *  
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package msrd0.matrix.client.event

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.MatrixId

object EventTypes
{
	@JvmField
	val ROOM_MESSAGE = "m.room.message"
}

interface JsonSerializable
{
	fun getJson() : JsonObject
}

abstract class Event(
		val sender : MatrixId,
		val type : String,
		val content : EventContent
) : JsonSerializable
{
	var timestamp : Long? = null
	
	var event_id : String? = null
	
	protected fun getAbstractJson() : JsonObject
	{
		val json = JsonObject()
		json["sender"] = sender.toString()
		json["type"] = type
		json["content"] = content.getJson()
		return json
	}
}
