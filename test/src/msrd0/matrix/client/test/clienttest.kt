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
import msrd0.matrix.client.event.ImageMessageContent
import msrd0.matrix.client.event.state.*
import msrd0.matrix.client.util.emptyMutableMap
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.testng.Assert.*
import org.testng.annotations.Test
import java.net.URI
import javax.imageio.ImageIO

class ClientTest
{
	companion object
	{
		val domain = if (System.getenv().containsKey("CI")) "synapse:8008" else "localhost:8008"
		val hs = HomeServer(domain, URI("http://$domain"))
		var id = MatrixId("test${System.currentTimeMillis()}", "synapse")
		val password = "Eish2nies9peifaez7uX"
		var userData : MatrixUserData? = null
		var roomId : RoomId? = null
		
		fun newClient() : Client
		{
			val c = Client(hs, id)
			c.userData = userData
			return c
		}
	}
	
	@Test(groups = arrayOf("api"))
	fun client_register()
	{
		val client = Client.register(id.localpart, hs, password)
		assertThat(client.hs, equalTo(hs))
		assertThat(client.id.localpart, equalTo(id.localpart))
		assertNotNull(client.userData)
		
		// sync to make sure our token is working
		client.sync()
		userData = client.userData
		id = client.id // to make sure the domain is what synapse think it would be
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun client_login()
	{
		val client = newClient()
		client.userData = null // just make sure we aren't authenticated yet
		
		val auth = client.auth(LoginType.PASSWORD)
		assertNotNull(auth)
		auth!! // this is already covered by the assertion before
		auth.setProperty("password", password)
		
		val res = auth.submit()
		assertThat(res.filter { it.isSuccess }.size, greaterThan(0))
		assertNotNull(client.userData)
		
		// sync to make sure our token is working
		client.sync()
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun client_update()
	{
		val client = newClient()
		val displayname = "test user"
		client.updateDisplayname(displayname)
		assertThat(client.displayname(), equalTo(displayname))
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun device_update()
	{
		val client = newClient()
		val name = "this device was created by matrix client tests"
		client.updateDeviceDisplayName(client.deviceId!!, name)
		
		val device = client.device(client.deviceId!!)
		assertNotNull(device)
		assertThat(device!!.displayName, equalTo(name))
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("client_register"))
	fun room_create()
	{
		val client = newClient()
		val name = "test room"
		val topic = "room created by gradle tests"
		val room = client.createRoom(name = name, topic = topic, public = false)
		assertThat(room.name, equalTo(name))
		assertThat(room.topic, equalTo(topic))
		assertThat(room.members, contains(id))
		roomId = room.id
		
		// check that there are no aliases right now
		assertThat(room.retrieveAliases("synapse").size, equalTo(0))
		assertNull(room.retrieveCanonicalAlias())
		
		// check that we have owner power levels in the room. if they changed the owner power level we need to do so
		// as well.
		val powerLevels = room.retrievePowerLevels()
		assertThat(powerLevels.users, hasKey(id))
		assertThat(powerLevels.users[id], equalTo(RoomPowerLevels.OWNER))
		
		// check that all other power levels are default. if they change the default power level of something we need
		// to do so as well.
		powerLevels.users.clear()
		assertThat(powerLevels, equalTo(RoomPowerLevels()))
		
		// make sure the join rule is invite right now so we can change them later
		room.updateJoinRule(RoomJoinRules.INVITE)
		assertThat(room.retrieveJoinRule(), equalTo(RoomJoinRules.INVITE))
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("room_create"))
	fun room_update()
	{
		val client = newClient()
		val newName = "updated room name"
		val newTopic = "updated room topic"
		val aliases = listOf(
				"#test${System.currentTimeMillis()}:synapse",
				"#dummy${System.currentTimeMillis()}:synapse"
		).map { RoomAlias.fromString(it) }
		var room = Room(client, roomId!!)
		
		var powerLevels = room.retrievePowerLevels()
		val powerLevelEvent = "msrd0.matrix.client.test.power_level_event" to 7
		powerLevels.events[powerLevelEvent.first] = powerLevelEvent.second
		
		room.updateName(newName)
		room.updateTopic(newTopic)
		aliases.forEach { room.addAlias(it) }
		room.updateCanonicalAlias(aliases.first())
		room.updatePowerLevels(powerLevels)
		room.updateJoinRule(RoomJoinRules.PUBLIC)
		
		// test with dirty cache
		assertThat(room.name, equalTo(newName))
		assertThat(room.topic, equalTo(newTopic))
		
		room = Room(client, roomId!!)
		
		// test with clean cache
		assertThat(room.name, equalTo(newName))
		assertThat(room.topic, equalTo(newTopic))
		
		// test those that aren't cached
		assertThat(room.retrieveAliases("synapse"), equalTo(aliases))
		assertNotNull(room.retrieveCanonicalAlias())
		assertThat(room.retrieveCanonicalAlias(), equalTo(aliases.first()))
		powerLevels = room.retrievePowerLevels()
		assertThat(powerLevels.events, hasKey(powerLevelEvent.first))
		assertThat(powerLevels.events[powerLevelEvent.first], equalTo(powerLevelEvent.second))
		assertThat(room.retrieveJoinRule(), equalTo(RoomJoinRules.PUBLIC))
	}
	
	@Test(groups = arrayOf("api"), dependsOnMethods = arrayOf("room_create"))
	fun room_send_image()
	{
		val client = newClient()
		val room = Room(client, roomId!!)
		
		val msg = ImageMessageContent("matrix-logo.png")
		val img = ImageIO.read(ClassLoader.getSystemResourceAsStream("matrix-logo.png"))
		msg.uploadImage(img, client)
		room.sendMessage(msg)
		
		val messages = room.retrieveMessages()
		val filtered = messages.filter { it.body == "matrix-logo.png" }
		assertThat(filtered.size, greaterThan(0))
		val content = filtered.first().content
		assertThat(content, instanceOf(ImageMessageContent::class.java))
		content as ImageMessageContent
		assertThat(content.width, equalTo(img.width))
		assertThat(content.height, equalTo(img.height))
		val downloaded = content.downloadImage(client)
		assertThat(downloaded.width, equalTo(img.width))
		assertThat(downloaded.height, equalTo(img.height))
	}
}
