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

package msrd0.matrix.client

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
			if (s.size != 2)
				throw IllegalArgumentException()
			if (!s[0].startsWith("@"))
				throw IllegalArgumentException()
			return MatrixId(s[0].substring(1), s[1])
		}
	}
	
	override fun toString() : String = "@$localpart:$domain"
}

/**
 * A data class representing a client context.
 */
data class ClientContext(
		val hs : HomeServer,
		val id : MatrixId
)

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
		 * Parses a string like `id:example.tld` and returns a RoomId.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		fun fromString(str : String) : RoomId
		{
			val s = str.split(':')
			if (s.size != 2)
				throw IllegalArgumentException()
			return RoomId(s[0], s[1])
		}
	}
	
	override fun toString(): String = "$id:$domain"
}
