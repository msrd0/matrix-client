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

package msrd0.matrix.client.listener

import msrd0.matrix.client.Room

/**
 * This is the base class for every event handled by the EventQueue. It's id must match
 * the one of the listeners registered for it.
 */
open class Event(val type : EventType)
{
	constructor(id : String, listener : Class<*>) : this(EventType(id, listener))
	
	val id : String
		get() = type.id
	val listener : Class<*>
		get() = type.listener
}

open class RoomJoinEvent(val room : Room) : Event("matrix.room.join", RoomJoinListener::class.java)

open class RoomInvitationEvent(val room : Room) : Event("matrix.room.invitation", RoomInvitationListener::class.java)
