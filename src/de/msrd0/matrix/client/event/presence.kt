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

package de.msrd0.matrix.client.event

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.*

/**
 * Represents a presence state.
 */
enum class PresenceState
{
	ONLINE,
	OFFLINE,
	UNAVAILABLE
}

/**
 * Holds a user's presence.
 */
data class Presence(
		val user : MatrixId,
		var presence : PresenceState,
		var lastActiveAgo : Long? = null,
		var status : String? = null,
		var currentlyActive : Boolean = false
)
{
	companion object
	{
		/**
		 * Parses a user's presence from json.
		 *
		 * @throws IllegalJsonException On errors in the supplied json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(user : MatrixId, json : JsonObject) : Presence
		{
			val presence = Presence(user,
					PresenceState.valueOf(json.string("presence")?.toUpperCase() ?: missing("presence")))
			presence.lastActiveAgo = json.long("last_active_ago")
			presence.status = json.string("status_msg")
			presence.currentlyActive = json.boolean("currently_active") ?: false
			return presence
		}
	}
}

// TODO implement presence events
