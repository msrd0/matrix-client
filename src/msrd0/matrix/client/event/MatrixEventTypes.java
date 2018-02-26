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

package msrd0.matrix.client.event;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class MatrixEventTypes
{
	// ### room state events ###########################################################################################
	
	/**
	 * Event to update the list of known aliases for a certain room.
	 */
	public static final String ROOM_ALIASES = "m.room.aliases";
	
	/**
	 * This event sets the room's avatar.
	 */
	public static final String ROOM_AVATAR = "m.room.avatar";
	
	/**
	 * Event to update the canonical alias of the room.
	 */
	public static final String ROOM_CANONICAL_ALIAS = "m.room.canonical_alias";
	
	/**
	 * The first event in a room. It acts as the root event.
	 */
	public static final String ROOM_CREATE = "m.room.create";
	
	/**
	 * This event defines how this room should be encrypted.
	 */
	public static final String ROOM_ENCRYPTION = "m.room.encryption";
	
	/**
	 * This event controls whether users can view an event before they joined the room.
	 */
	public static final String ROOM_HISTORY_VISIBILITY = "m.room.history_visibility";
	
	/**
	 * This event updates the join rules of the room. A room may be public which means everyone can join, or private
	 * which means that one need an invitation to join.
	 */
	public static final String ROOM_JOIN_RULES = "m.room.join_rules";
	
	/**
	 * This event is used to exchange keys for e2e-encryption. It should be encrypted as a `ROOM_ENCRYPTED` event.
	 */
	public static final String ROOM_KEY = "m.room_key";
	
	/**
	 * This event adjusts the membership of a user in a room.
	 */
	public static final String ROOM_MEMBER = "m.room.member";
	
	/**
	 * This event sets the room's name.
	 */
	public static final String ROOM_NAME = "m.room.name";
	
	/**
	 * This event updates the power levels that members need to perform certain actions.
	 */
	public static final String ROOM_POWER_LEVELS = "m.room.power_levels";
	
	/**
	 * This event sets the room's topic.
	 */
	public static final String ROOM_TOPIC = "m.room.topic";
	
	
	// ### message events ##############################################################################################
	
	/**
	 * A new, encrypted message in a room.
	 */
	public static final String ROOM_ENCRYPTED = "m.room.encrypted";
	
	/**
	 * A new, unencrypted message in a room.
	 */
	public static final String ROOM_MESSAGE = "m.room.message";
	
	/**
	 * A redacted message in a room.
	 */
	public static final String ROOM_REDACTION = "m.room.redaction";
	
	// ### call message events #########################################################################################
	
	public static final String CALL_INVITE = "m.call.invite";
	
	public static final String CALL_CANDIDATES = "m.call.candidates";
	
	public static final String CALL_ANSWER = "m.call.answer";
	
	public static final String CALL_HANGUP = "m.call.hangup";
	
	// ### receipt events ##############################################################################################
	
	/**
	 * A receipt.
	 */
	public static final String RECEIPT = "m.receipt";
	
	
	// ### other events ################################################################################################
	
	public static final String DEVICE_NEW = "m.new_device";
}
