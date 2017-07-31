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
		val domain = if (System.getenv().containsKey("CI")) "synapse:8008" else "localhost:8008"
		val hs = HomeServer("$domain", URI("http://$domain"))
		val id = MatrixId("test${System.currentTimeMillis()}", "synapse")
		val password = "Eish2nies9peifaez7uX"
		var token : String? = null
		
		fun newClient() : Client
		{
			val c = Client(hs, id)
			c.token = token
			return c
		}
	}
	
	@Test(groups = arrayOf("api"))
	fun client_register()
	{
		val client = Client.register(id.localpart, hs, password)
		assertThat(client.hs, equalTo(hs))
		assertThat(client.id, equalTo(id))
		assertThat(client.token, notNullValue())
		
		// sync to make sure our token is working
		client.sync()
		token = client.token
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun client_login()
	{
		val client = newClient()
		client.token = null // just make sure we aren't authenticated yet
		
		val auth = client.auth(LoginType.PASSWORD)
		assertThat(auth, notNullValue())
		auth!! // this is already covered by the assertion before
		auth.setProperty("password", password)
		
		val res = auth.submit()
		assertThat(res.filter { it.isSuccess }.size, greaterThan(0))
		assertThat(client.token, notNullValue())
		
		// sync to make sure our token is working
		client.sync()
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun create_room()
	{
		val client = newClient()
		val name = "test room"
		val topic = "room created by gradle tests"
		val room = client.createRoom(name = name, topic = topic, public = false)
		assertThat(room.name, equalTo(name))
		assertThat(room.members, contains(id))
	}
}
