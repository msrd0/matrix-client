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

package msrd0.matrix.client.test

import msrd0.matrix.client.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.testng.annotations.Test
import java.net.URI

class ClientTest
{
	companion object
	{
		val hs = HomeServer("matrix.org", URI("https://matrix.org"))
		val id = MatrixId("msrd0", "matrix.org")
	}
	
	@Test(expectedExceptions = arrayOf(MatrixErrorResponseException::class))
	fun client_auth()
	{
		val client = Client(hs, id)
		val authList = client.auth()
		assertThat(authList.size, greaterThan(0))
		val auth = authList.first()
		auth.submit()
	}
}
