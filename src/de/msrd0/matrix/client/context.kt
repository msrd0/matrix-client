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

import de.msrd0.matrix.client.MatrixClient.Companion.checkForError
import de.msrd0.matrix.client.MatrixClient.Companion.publicTarget
import java.net.URI

/**
 * A data class representing a matrix HomeServer.
 */
data class HomeServer(
		val domain : String,
		val base : URI
)

/**
 * A data class representing a matrix user id.
 */
data class MatrixId(
		val localpart : String,
		val domain : String
)
{
	companion object
	{
		/**
		 * Parses a string like `@localpart:example.tld` and returns a MatrixId.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		fun fromString(str : String) : MatrixId
		{
			val s = str.split(':')
			if (s.size < 2)
				throw IllegalArgumentException(str)
			if (!s[0].startsWith("@"))
				throw IllegalArgumentException(str)
			return MatrixId(s[0].substring(1), s.subList(1, s.size).joinToString(":"))
		}
	}
	
	/**
	 * The displayname of this matrix user.
	 */
	@get:Throws(MatrixAnswerException::class)
	val displayname : String? get()
	{
		val res = publicTarget.get("_matrix/client/r0/profile/$this/displayname")
		if (res.status.status == 404)
			return null
		checkForError(res)
		return res.json.string("displayname")
	}
	
	/**
	 * The avatar of this matrix user.
	 */
	@get:Throws(MatrixAnswerException::class)
	val avatar : Avatar? get()
	{
		val res = publicTarget.get("_matrix/client/r0/profile/$this/avatar_url")
		if (res.status.status == 404)
			return null
		checkForError(res)
		return Avatar(res.json.string("avatar_url") ?: return null)
	}
	
	override fun toString() : String = "@$localpart:$domain"
}

/**
 * A data class representing a room id.
 */
data class RoomId(
		val id : String,
		val domain : String
)
{
	companion object
	{
		/**
		 * Parses a string like `!id:example.tld` and returns a RoomId.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		fun fromString(str : String) : RoomId
		{
			val s = str.split(':')
			if (s.size < 2)
				throw IllegalArgumentException(str)
			if (!s[0].startsWith("!"))
				throw IllegalArgumentException(str)
			return RoomId(s[0].substring(1), s.subList(1, s.size).joinToString(":"))
		}
	}
	
	override fun toString() : String = "!$id:$domain"
}

/**
 * A data class representing a room alias.
 */
data class RoomAlias(
		val alias : String,
		val domain : String
)
{
	companion object
	{
		/**
		 * Parses a string like `#alias:example.tld` and returns a RoomAlias.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		fun fromString(str : String) : RoomAlias
		{
			val s = str.split(':')
			if (s.size < 2)
				throw IllegalArgumentException(str)
			if (!s[0].startsWith("#"))
				throw IllegalArgumentException(str)
			return RoomAlias(s[0].substring(1), s.subList(1, s.size).joinToString(":"))
		}
	}
	
	override fun toString() : String = "#$alias:$domain"
}

/**
 * A data class that stores user data, e.g. access token and device id.
 */
data class MatrixUserData(
		var token : String,
		var deviceId : String
)
