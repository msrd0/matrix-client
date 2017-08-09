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

package msrd0.matrix.client.event.state

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.event.MatrixEventTypes.*
import msrd0.matrix.client.util.emptyMutableMap

/**
 * A list of default power levels, as in the matrix specification for `m.room.power_levels`.
 */
object DefaultPowerLevels
{
	@JvmField val BAN = 50
	@JvmField val EVENTS_DEFAULT = 0
	@JvmField val INVITE = 0 // synapse says 0, the spec says 50
	@JvmField val KICK = 50
	@JvmField val REDACT = 50
	@JvmField val STATE_DEFAULT = 50
	@JvmField val USERS_DEFAULT = 0
	
	@JvmField val EVENTS = mapOf(
			// mods
			ROOM_AVATAR to 50,
			ROOM_CANONICAL_ALIAS to 50,
			ROOM_NAME to 50,
			// admins / owners
			ROOM_HISTORY_VISIBILITY to 100,
			ROOM_POWER_LEVELS to 100
	)
}

/**
 * The content of a room power levels event. To modify the power levels of a room, it is recommended to download the
 * current power levels, customize them, and upload them again, rather than creating the power levels from scratch.
 */
// TODO only make the maps mutable if someone wants them to be mutable
data class RoomPowerLevels(
		var ban : Int = DefaultPowerLevels.BAN,
		var events : MutableMap<String, Int> = DefaultPowerLevels.EVENTS.toMutableMap(),
		var eventsDefault : Int = DefaultPowerLevels.EVENTS_DEFAULT,
		var invite : Int = DefaultPowerLevels.INVITE,
		var kick : Int = DefaultPowerLevels.KICK,
		var redact : Int = DefaultPowerLevels.REDACT,
		var stateDefault : Int = DefaultPowerLevels.STATE_DEFAULT,
		var users : MutableMap<MatrixId, Int> = emptyMutableMap(),
		var usersDefault : Int = DefaultPowerLevels.USERS_DEFAULT
) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a room power levels event by parsing the supplied json.
		 */
		@JvmStatic
		fun fromJson(json : JsonObject) : RoomPowerLevels
				= RoomPowerLevels(
					ban = json.int("ban") ?: DefaultPowerLevels.BAN,
					events = json.obj("events")
							?.mapValues { it.value as Int }
							?.toMutableMap()
							?: DefaultPowerLevels.EVENTS.toMutableMap(),
					eventsDefault = json.int("events_default") ?: DefaultPowerLevels.EVENTS_DEFAULT,
					invite = json.int("invite") ?: DefaultPowerLevels.INVITE,
					kick = json.int("kick") ?: DefaultPowerLevels.KICK,
					redact = json.int("redact") ?: DefaultPowerLevels.REDACT,
					stateDefault = json.int("state_default") ?: DefaultPowerLevels.STATE_DEFAULT,
					users = json.obj("users")
							?.mapKeys { MatrixId.fromString(it.key) }
							?.mapValues { it.value as Int }
							?.toMutableMap()
							?: emptyMutableMap(),
					usersDefault = json.int("users_default") ?: DefaultPowerLevels.USERS_DEFAULT
				)
		
		/** The power level of a user. */
		@JvmField val USER = 0
		/** The power level of a moderator. */
		@JvmField val MODERATOR = 50
		/** The power level of an administrator. */
		@JvmField val ADMINISTRATOR = 100
		/** The power level of an owner. */
		@JvmField val OWNER = 100
	}
	
	// just for java interop
	constructor() : this(ban = DefaultPowerLevels.BAN) // hacky way to call the default constructor
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["ban"] = ban
		json["events"] = events
		json["events_default"] = eventsDefault
		json["invite"] = invite
		json["kick"] = kick
		json["redact"] = redact
		json["state_default"] = stateDefault
		json["users"] = users.mapKeys { "${it.key}" }
		json["users_default"] = usersDefault
		return json
	}
}

/**
 * A room power levels event.
 */
class RoomPowerLevelsEvent(
		room : Room,
		sender : MatrixId,
		content : RoomPowerLevels
) : MatrixRoomEvent<RoomPowerLevels>(room, sender, ROOM_POWER_LEVELS, content)
{
	companion object
	{
		/**
		 * Constructs a room power levels event by parsing the supplied json.
		 *
		 * @throws IllegalJsonException On errors in the json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(room : Room, json : JsonObject) : RoomPowerLevelsEvent
				= RoomPowerLevelsEvent(room, MatrixId.fromString(json.string("sender") ?: missing("sender")),
					RoomPowerLevels.fromJson(json.obj("content") ?: missing("content")))
	}
	
	override val json : JsonObject get() = abstractJson
}
