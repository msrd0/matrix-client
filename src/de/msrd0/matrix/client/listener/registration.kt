/*
 * matrix-client
 * Copyright (C) 2017-2018 Dominic Meiser
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

package de.msrd0.matrix.client.listener

import de.msrd0.matrix.client.listener.EventTypes.ROOM_INVITATION
import de.msrd0.matrix.client.listener.EventTypes.ROOM_JOIN
import de.msrd0.matrix.client.listener.EventTypes.ROOM_MESSAGE_RECEIVED

interface ListenerRegistration
{
	fun on(type : EventType, l : Listener<*>)
}

fun ListenerRegistration.onRoomJoin(callback : (event : RoomJoinEvent) -> Boolean)
		= on(ROOM_JOIN, object : RoomJoinListener {
			override fun call(event : RoomJoinEvent) : Boolean
					= callback(event)
		})

fun ListenerRegistration.onRoomInvitation(callback : (event : RoomInvitationEvent) -> Boolean)
		= on(ROOM_INVITATION, object : RoomInvitationListener {
			override fun call(event : RoomInvitationEvent) : Boolean
					= callback(event)
		})

fun ListenerRegistration.onRoomMessage(callback : (event : RoomMessageReceivedEvent) -> Boolean)
		= on(ROOM_MESSAGE_RECEIVED, object : RoomMessageReceivedListener {
			override fun call(event : RoomMessageReceivedEvent) : Boolean
					= callback(event)
		})
