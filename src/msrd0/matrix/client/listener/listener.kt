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

import msrd0.matrix.client.listener.EventType.*

import msrd0.matrix.client.Room


/**
 * This class must be inherited by every Listener supplied to the event queue.
 */
abstract class Listener<in E>(val name : String)
{
	/**
	 * This method will be called as soon as an event of the type of this listener is fired. The
	 * listener last added to the event queue will be called first, and if he returns true, no further
	 * event listeners will be called.
	 *
	 * @returns true to stop further listeners for this event to be called.
	 */
	abstract fun call(event : E) : Boolean
}

abstract class RoomJoinListener : Listener<Room>(ROOM_JOIN.id)

/**
 * This listeners is called as soon as a new room invitation arrives.
 */
abstract class RoomInvitationListener : Listener<Room>(ROOM_INVITATION.id)
