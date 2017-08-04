/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
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

package msrd0.matrix.client.event

object MatrixEventTypes
{
	// ### room state events ###########################################################################################
	
	/**
	 * Event to update the list of known aliases for a certain room.
	 */
	@JvmField
	val ROOM_ALIASES = "m.room.aliases"
	
	/**
	 * This event sets the room's avatar.
	 */
	@JvmField
	val ROOM_AVATAR = "m.room.avatar"
	
	/**
	 * Event to update the canonical alias of the room.
	 */
	@JvmField
	val ROOM_CANONICAL_ALIAS = "m.room.canonical_alias"
	
	/**
	 * The first event in a room. It acts as the root event.
	 */
	@JvmField
	val ROOM_CREATE = "m.room.create"
	
	/**
	 * This event defines how this room should be encrypted.
	 */
	@JvmField
	val ROOM_ENCRYPTION = "m.room.encryption"
	
	/**
	 * This event updates the join rules of the room. A room may be public which means everyone can join, or private
	 * which means that one need an invitation to join.
	 */
	@JvmField
	val ROOM_JOIN_RULES = "m.room.join_rules"
	
	/**
	 * This event is used to exchange keys for e2e-encryption. It should be encrypted as a `ROOM_ENCRYPTED` event.
	 */
	@JvmField
	val ROOM_KEY = "m.room_key"
	
	/**
	 * This event adjusts the membership of a user in a room.
	 */
	@JvmField
	val ROOM_MEMBER = "m.room.member"
	
	/**
	 * This event sets the room's name.
	 */
	@JvmField
	val ROOM_NAME = "m.room.name"
	
	/**
	 * This event updates the power levels that members need to perform certain actions.
	 */
	@JvmField
	val ROOM_POWER_LEVELS = "m.room.power_levels"
	
	/**
	 * This event sets the room's topic.
	 */
	@JvmField
	val ROOM_TOPIC = "m.room.topic"
	
	// ### message events ##############################################################################################
	
	/**
	 * A new, encrypted message in a room.
	 */
	@JvmField
	val ROOM_ENCRYPTED = "m.room.encrypted"
	
	/**
	 * A new, unencrypted message in a room.
	 */
	@JvmField
	val ROOM_MESSAGE = "m.room.message"
	
	/**
	 * A redacted message in a room.
	 */
	@JvmField
	val ROOM_REDACTION = "m.room.redaction"
	
	// ### other events ################################################################################################
	
	@JvmField
	val DEVICE_NEW = "m.new_device"
}
