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

package msrd0.matrix.event

enum class EventType
{

}

open abstract class Event(timestamp: Long, sender: String, event_id: String, age: Long, unsigned: Unsigned, type: EventType)
{
	//information of events from other servers
	class Unsigned(age: Long)
	{
		var prev_content: EventContent? = null
		var transaction_id: String? = null
		
		fun addEventContent(eventContent: EventContent): Unsigned
		{
			prev_content = eventContent
			return this
		}
		fun addTransactionID(id: String): Unsigned
		{
			transaction_id = id
			return this
		}
	}
	
	//depends on eventtype
	open abstract class EventContent()
	
	abstract fun getJSON(): String
}
