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

package de.msrd0.matrix.client.event.state;

import static lombok.AccessLevel.*;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class RoomHistoryVisibility
{
	/**
	 * All events may be shared with anyone, no matter if they ever joined the room.
	 */
	public static final String WORLD_READABLE = "world_readable";
	
	/**
	 * All events may be shared with all who joined the room, no matter if they joined before or after
	 * the event was sent.
	 */
	public static final String SHARED = "shared";
	
	/**
	 * Those events sent after the invitation may be shared with the invitee.
	 */
	public static final String INVITED = "invited";
	
	/**
	 * Those events sent after a particular member joined may be shared with that member.
	 */
	public static final String JOINED = "joined";
}
