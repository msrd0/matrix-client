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

package msrd0.matrix.client.event.state;

import static lombok.AccessLevel.*;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class RoomJoinRules
{
	/** Anybody can join without prior action. */
	public static final String PUBLIC  = "public";
	
	/** Reserved. */
	public static final String KNOCK   = "knock";
	
	/** Anybody who wants to join need to receive an invitation. */
	public static final String INVITE  = "invite";
	
	/** Reserved. */
	public static final String PRIVATE = "private";
}
