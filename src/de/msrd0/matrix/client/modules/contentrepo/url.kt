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

package de.msrd0.matrix.client.modules.contentrepo

import java.util.regex.Pattern

/**
 * This class represents a matrix content url of format `mxc://example.tld/FHyPlCeYUSFFxlgbQYZmoEoe`.
 */
data class MatrixContentUrl(
		val domain : String,
		val mediaId : String
)
{
	companion object
	{
		/** Regex pattern to match a matrix content url. */
		private val pattern : Pattern = Pattern.compile("mxc://(?<domain>[^/]+)/(?<mediaId>[^/]+)")
		
		/**
		 * Parse the supplied matrix content url using [pattern] and return a pair of `domain` and `mediaId`.
		 *
		 * @throws IllegalArgumentException If the [url] could not be parsed.
		 */
		private fun parseUrl(url : String) : Pair<String, String>
		{
			val matcher = pattern.matcher(url)
			if (!matcher.matches())
				throw IllegalArgumentException("The supplied url doesn't match the required pattern")
			return matcher.group("domain") to matcher.group("mediaId")
		}
		
		@JvmStatic
		@Deprecated("Use constructor instead", replaceWith = ReplaceWith("MatrixContentUrl(str)"))
		fun fromString(str : String) = MatrixContentUrl(str)
	}
	
	/**
	 * Construct a [MatrixContentUrl] from the result of [parseUrl].
	 */
	private constructor(parsedUrl : Pair<String, String>) : this(parsedUrl.first, parsedUrl.second)
	
	/**
	 * Construct a [MatrixContentUrl] by parsing a matrix content url of format `mxc://example.tld/FHyPlCeYUSFFxlgbQYZmoEoe`.
	 */
	constructor(url : String) : this(parseUrl(url))
	
	/**
	 * Convert this matrix content url into a string of format `mxc://example.tld/FHyPlCeYUSFFxlgbQYZmoEoe`.
	 */
	override fun toString() = "mxc://$domain/$mediaId"
}
