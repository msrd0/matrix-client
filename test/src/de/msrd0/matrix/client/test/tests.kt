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

package de.msrd0.matrix.client.test

import de.msrd0.matrix.client.*
import de.msrd0.matrix.client.room.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.testng.annotations.Test

/**
 * This class has some common test cases.
 */
class Tests
{
	@Test(groups = arrayOf("base"))
	fun matrixIdFromString()
	{
		val ids = arrayOf(
				Pair("@example:matrix.org", MatrixId("example", "matrix.org")),
				Pair("@example:localhost:8008", MatrixId("example", "localhost:8008"))
		)
		for ((str, id) in ids)
		{
			assertThat(str, equalTo("$id"))
			assertThat(id, equalTo(MatrixId.fromString(str)))
		}
	}
	
	@Test(groups = arrayOf("base"))
	fun roomIdFromString()
	{
		val ids = arrayOf(
				Pair("!AikeXjUeFRjsQZJWhb:matrix.org", MatrixRoomId("AikeXjUeFRjsQZJWhb", "matrix.org")),
				Pair("!AikeXjUeFRjsQZJWhb:localhost:8008", MatrixRoomId("AikeXjUeFRjsQZJWhb", "localhost:8008"))
		)
		for ((str, id) in ids)
		{
			assertThat(str, equalTo("$id"))
			assertThat(id, equalTo(MatrixRoomId(str)))
		}
	}
	
	@Test(groups = arrayOf("base"))
	fun roomAliasFromString()
	{
		val aliases = arrayOf(
				Pair("#matrix-dev:matrix.org", MatrixRoomAlias("matrix-dev", "matrix.org")),
				Pair("#test:localhost:8008", MatrixRoomAlias("test", "localhost:8008"))
		)
		for ((str, alias) in aliases)
		{
			assertThat(str, equalTo("$alias"))
			assertThat(alias, equalTo(MatrixRoomAlias(str)))
		}
	}
}
