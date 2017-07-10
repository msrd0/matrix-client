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

enum class EventType

interface JsonSerializable
{
	fun getJson() : JsonObject
}

abstract class Event(
		val timestamp : Long,
		val sender : String,
		val event_id : String,
		val age : Long,
		val unsigned : Unsigned,
		val type : EventType
) : JsonSerializable
{
	//information of events from other servers
	companion object
	{
		class Unsigned (
			val age : Long
		)
		{
			var prev_content : EventContent? = null
			var transaction_id : String? = null
			
			fun addEventContent(eventContent : EventContent) : Unsigned
			{
				prev_content = eventContent
				return this
			}
			
			fun addTransactionID(id : String) : Unsigned
			{
				transaction_id = id
				return this
			}
		}
	}
}
