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

package de.msrd0.matrix.client

import com.beust.klaxon.*
import java.time.*

/**
 * A data class that holds informations about a specific device.
 */
data class Device(
		val deviceId : String,
		val displayName : String?,
		val lastSeenIp : String,
		val lastSeen : ZonedDateTime
)
{
	companion object
	{
		/**
		 * Parses the supplied json and returns a device.
		 *
		 * @throws IllegalJsonException On errors in the supplied json.
		 */
		@JvmStatic
		@Deprecated(message = "Use constructor instead", replaceWith = ReplaceWith("Device(json)"))
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) = Device(json)
	}
	
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			json.string("device_id") ?: missing("device_id"),
			json.string("display_name"),
			json.string("last_seen_ip") ?: missing("last_seen_ip"),
			Instant.ofEpochMilli(json.long("last_seen_ts") ?: missing("last_seen_ts")).atZone(ZoneId.systemDefault())
	)
}
