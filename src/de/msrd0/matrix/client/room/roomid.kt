/*
 matrix-client
 Copyright (C) 2018 Dominic Meiser
 
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

package de.msrd0.matrix.client.room

/**
 * A data class representing a room id.
 */
data class MatrixRoomId(
		val id : String,
		val domain : String
)
{
	companion object
	{
		/**
		 * Parses a string like `!id:example.tld` and returns a pair of `id` and `domain`.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		private fun parse(str : String) : Pair<String, String>
		{
			val s = str.split(':')
			if (s.size < 2)
				throw IllegalArgumentException(str)
			if (!s[0].startsWith("!"))
				throw IllegalArgumentException(str)
			return s[0].substring(1) to s.subList(1, s.size).joinToString(":")
		}
		
		/**
		 * Parses a string like `!id:example.tld` and returns a RoomId.
		 *
		 * @throws IllegalArgumentException If the string doesn't match the format.
		 */
		@Deprecated("Use constructor instead", replaceWith = ReplaceWith("MatrixRoomId(str)"))
		fun fromString(str : String) = MatrixRoomId(str)
	}
	
	/**
	 * Construct a [MatrixRoomId] from the result of [parse].
	 */
	private constructor(parsed : Pair<String, String>) : this(parsed.first, parsed.second)
	
	/**
	 * Construct a [MatrixRoomId] by parsing a string of the form `!id:example.tld`.
	 */
	constructor(roomId : String) : this(parse(roomId))
	
	/**
	 * Returns a string of the form `!id:example.tld` representing this room id.
	 */
	override fun toString() = "!$id:$domain"
}
