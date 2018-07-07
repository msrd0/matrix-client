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
import de.msrd0.matrix.client.event.*
import de.msrd0.matrix.client.event.state.*
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.testng.Assert.*
import org.testng.annotations.Test
import java.awt.image.RenderedImage
import java.lang.ClassLoader.*
import java.net.URI
import java.nio.charset.StandardCharsets.*
import javax.imageio.ImageIO

class MatrixClientTest
{
	companion object
	{
		val domain = if (System.getenv().containsKey("CI")) "synapse:8008" else "localhost:8008"
		val hs = HomeServer(domain, URI("http://$domain"))
		var id = MatrixId("test${System.currentTimeMillis()}", "synapse")
		val password = "Eish2nies9peifaez7uX"
		var userData : MatrixUserData? = null
		var roomId : RoomId? = null
		
		val testImage : RenderedImage = ImageIO.read(getSystemResourceAsStream("matrix-logo.png"))
		var testAvatar : Avatar? = null
		var testFile : String = IOUtils.toString(getSystemResource("blindtext.txt"), UTF_8)
		
		var numClient : Long = 0
		fun newClient() : MatrixClient
		{
			val c = MatrixClient(hs, id)
			c.userData = userData
			synchronized(numClient) {
				c.lastTxnId = numClient
				numClient += 100
			}
			return c
		}
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register"])
	fun avatar()
	{
		val client = newClient()
		testAvatar = Avatar.fromImage(testImage, client)
	}
	
	@Test(groups = ["api"])
	fun client_register()
	{
		val client = MatrixClient.register(id.localpart, hs, password)
		assertThat(client.hs, equalTo(hs))
		assertThat(client.id.localpart, equalTo(id.localpart))
		assertNotNull(client.userData)
		
		// sync to make sure our token is working
		client.sync()
		userData = client.userData
		id = client.id // to make sure the domain is what synapse think it would be
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register"])
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
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register", "avatar"])
	fun client_update()
	{
		val client = newClient()
		val displayname = "test user"
		client.updateDisplayname(displayname)
		client.updateAvatar(testAvatar!!)
		assertThat(client.displayname(), equalTo(displayname))
		val avatar = client.avatar()
		assertNotNull(avatar)
		avatar as Avatar // asserted not null
		assertThat(avatar.url, equalTo(testAvatar!!.url))
		assertThat(client.downloadBytes(avatar.url).first.size, equalTo(testAvatar!!.info!!.size))
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register"])
	fun device_update()
	{
		val client = newClient()
		val name = "this device was created by matrix client tests"
		client.updateDeviceDisplayName(client.deviceId!!, name)
		
		val device = client.device(client.deviceId!!)
		assertNotNull(device)
		assertThat(device!!.displayName, equalTo(name))
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register"])
	fun device_delete()
	{
		val client = newClient()
		
		// create a new client
		val tbd = newClient()
		tbd.userData = null // make sure we create a new device
		val auth = tbd.auth(LoginType.PASSWORD)!!
		auth.setProperty("password", password)
		val res = auth.submit()
		assertThat(res.filter { it.isSuccess }.size, greaterThan(0))
		val deviceId = tbd.deviceId
		assertNotNull(deviceId) // we are now logged in with a new device
		deviceId!!
		
		// make sure the device is one of our devices
		//assertThat(client.devices().map { it.deviceId }, contains(deviceId))
		
		// delete the device
		client.deleteDevice(deviceId, DefaultFlowHelper(password))
		
		// make sure the device is no longer our device
		assertThat(client.devices().map { it.deviceId }, not(contains(deviceId)))
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["client_register"])
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
		
		// the room shouldn't have an avatar right now
		assertNull(room.avatar)
		
		// check that there are no aliases right now
		assertThat(room.aliases["synapse"].size, equalTo(0))
		assertNull(room.canonicalAlias)
		
		// check that we have owner power levels in the room. if they changed the owner power level we need to do so
		// as well.
		val powerLevels = room.powerLevels
		assertThat(powerLevels.users, hasKey(id))
		assertThat(powerLevels.users[id], equalTo(RoomPowerLevels.OWNER))
		
		// check that all other power levels are default. if they change the default power level of something we need
		// to do so as well.
		powerLevels.users.clear()
		assertThat(powerLevels, equalTo(RoomPowerLevels()))
		
		// make sure the join rule is invite right now so we can change it later
		room.joinRule = RoomJoinRules.INVITE
		assertThat(room.joinRule, equalTo(RoomJoinRules.INVITE))
		
		// make sure the history visibility is shared so we can change it later
		room.historyVisibility = RoomHistoryVisibility.SHARED
		assertThat(room.historyVisibility, equalTo(RoomHistoryVisibility.SHARED))
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["room_create", "avatar"])
	fun room_update()
	{
		val client = newClient()
		val newName = "updated room name"
		val newTopic = "updated room topic"
		val aliases = listOf(
				"#test${System.currentTimeMillis()}:synapse",
				"#dummy${System.currentTimeMillis()}:synapse"
		).map { RoomAlias.fromString(it) }
		val room = Room(client, roomId!!)
		
		var powerLevels = room.powerLevels
		val powerLevelEvent = "msrd0.matrix.client.test.power_level_event" to 7
		powerLevels.events[powerLevelEvent.first] = powerLevelEvent.second
		
		room.name = newName
		room.topic = newTopic
		aliases.forEach { room.addAlias(it) }
		room.canonicalAlias = aliases.first()
		room.powerLevels = powerLevels
		room.joinRule = RoomJoinRules.PUBLIC
		room.historyVisibility = RoomHistoryVisibility.WORLD_READABLE
		room.avatar = testAvatar
		
		room.clearCache()
		
		assertThat(room.name, equalTo(newName))
		assertThat(room.topic, equalTo(newTopic))
		assertThat(room.aliases["synapse"], equalTo(aliases))
		assertNotNull(room.canonicalAlias)
		assertThat(room.canonicalAlias, equalTo(aliases.first()))
		powerLevels = room.powerLevels
		assertThat(powerLevels.events, hasKey(powerLevelEvent.first))
		assertThat(powerLevels.events[powerLevelEvent.first], equalTo(powerLevelEvent.second))
		assertThat(room.joinRule, equalTo(RoomJoinRules.PUBLIC))
		assertThat(room.historyVisibility, equalTo(RoomHistoryVisibility.WORLD_READABLE))
		val avatar = room.avatar
		assertNotNull(avatar)
		avatar as Avatar // asserted not null
		assertThat(avatar.url, equalTo(testAvatar!!.url))
		assertThat(client.downloadBytes(avatar.url).first.size, equalTo(testAvatar!!.info!!.size))
		
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["room_create"])
	fun room_send_message()
	{
		val client = newClient()
		val room = Room(client, roomId!!)
		
		val msg = FormattedTextMessageContent(
				"This is a test message",
				TextMessageFormats.HTML,
				"<p><i>This</i> is a <b>test</b> message</p>"
		)
		room.sendMessage(msg)
		
		val messages = room.retrieveMessages()
		val filtered = messages.filter { it.body == msg.body }
		assertThat(filtered.size, greaterThan(0))
		val content = filtered.first().content
		assertThat(content, instanceOf(FormattedTextMessageContent::class.java))
		content as FormattedTextMessageContent
		assertThat(content.format, equalTo(msg.format))
		assertThat(content.formattedBody, equalTo(msg.formattedBody))
	}
	
	@Test(groups = ["api"], dependsOnMethods = ["room_create"])
	fun room_send_image()
	{
		val client = newClient()
		val room = Room(client, roomId!!)
		
		val msg = ImageMessageContent("matrix-logo.png")
		val img = testImage
		msg.uploadImage(img, client)
		room.sendMessage(msg)
		
		val messages = room.retrieveMessages()
		val filtered = messages.filter { it.body == msg.body }
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
	
	@Test(groups = ["api"], dependsOnMethods = ["room_create"])
	fun room_send_file()
	{
		val client = newClient()
		val room = Room(client, roomId!!)
		
		val msg = FileMessageContent("blindtext.txt")
		val bytes = testFile.toByteArray(UTF_8)
		msg.uploadFile(bytes, "text/plain;charset=utf-8", client)
		room.sendMessage(msg)
		
		val messages = room.retrieveMessages()
		val filtered = messages.filter { it.body == msg.body }
		assertThat(filtered.size, greaterThan(0))
		val content = filtered.first().content
		assertThat(content, instanceOf(FileMessageContent::class.java))
		content as FileMessageContent
		assertThat(content.filename, equalTo(msg.filename))
		assertThat(content.mimetype, equalTo(msg.mimetype))
		assertThat(content.size, equalTo(bytes.size))
		val downloaded = content.downloadFile(client)
		assertThat(String(downloaded, UTF_8), equalTo(testFile))
	}
}
