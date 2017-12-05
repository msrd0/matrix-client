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

package msrd0.matrix.client

import com.beust.klaxon.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import msrd0.matrix.client.e2e.*
import msrd0.matrix.client.event.*
import msrd0.matrix.client.event.MatrixEventTypes.*
import msrd0.matrix.client.event.encryption.*
import msrd0.matrix.client.event.encryption.EncryptionAlgorithms.*
import msrd0.matrix.client.filter.Filter
import msrd0.matrix.client.filter.uploadFilter
import msrd0.matrix.client.listener.*
import msrd0.matrix.client.util.*
import org.matrix.olm.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit.*

/**
 * This class is the http client for the matrix server.
 */
open class MatrixClient(val hs : HomeServer, val id : MatrixId) : ListenerRegistration
{
	companion object
	{
		private val logger : Logger = LoggerFactory.getLogger(MatrixClient::class.java)
		
		// make sure we load the olm library just in case somebody want's to use it
		init { OlmManager() }
		
		
		/**
		 * A public target to the matrix.org server. Should only be used for user-independent calls. All other calls
		 * should be submitted directly to the homeserver of that user.
		 */
		@JvmStatic
		val publicTarget : HttpTarget = DefaultHttpTarget(URI("https://matrix.org/"),
				"MatrixClient/${MextrixMatrixClient.VERSION} (${MextrixMatrixClient.URL})")
		
		
		/**
		 * Registers a new user at the given homeserver.
		 *
		 * NOTE: Application Services should supply their token. All other clients should NOT supply a token.
		 *
		 * @return A client to the newly created user.
		 * @throws MatrixAnswerException On errors in the matrix answer.
		 * @throws UnsupportedFlowsException If the HS requires additional flows and the `helper` can't provide it.
		 */
		@JvmStatic
		@JvmOverloads
		@Throws(MatrixAnswerException::class, UnsupportedFlowsException::class)
		fun register(localpart : String, hs : HomeServer, password : String, helper : FlowHelper = DefaultFlowHelper(password)) : MatrixClient
		{
			var json = JsonObject()
			json["username"] = localpart
			json["password"] = password
			json["bind_email"] = false
			
			val target = DefaultHttpTarget(hs.base, publicTarget.userAgent)
			var res = target.post("_matrix/client/r0/register", json)
			
			while (res.status.status == 401 && res.json.containsKey("flows"))
			{
				val flowResponse = helper.answer(FlowRequest.fromJson(res.json))
				json["auth"] = flowResponse.json
				res = target.post("_matrix/client/r0/register", json)
			}
			
			checkForError(res)
			
			json = res.json
			
			val client = MatrixClient(hs, MatrixId.fromString(json.string("user_id") ?: missing("user_id")))
			client.userData = MatrixUserData(
					json.string("access_token") ?: missing("access_token"),
					json.string("device_id") ?: missing("device_id")
			)
			logger.info("Registered new user ${client.id}")
			return client
		}
		
		/**
		 * Registers a new user at the given homeserver from an Application Service.
		 *
		 * @return A client to the newly created user.
		 * @throws MatrixAnswerException On errors in the matrix answer.
		 */
		@JvmStatic
		@Throws(MatrixAnswerException::class)
		fun registerFromAs(localpart : String, hs : HomeServer, token : String) : MatrixClient
		{
			var json = JsonObject()
			json["username"] = localpart
			json["type"] = "m.login.application_service"
			
			val target = DefaultHttpTarget(hs.base, publicTarget.userAgent)
			val res = target.post("_matrix/client/r0/register", token, null, json)
			checkForError(res)
			
			json = res.json
			
			val client = MatrixClient(hs, MatrixId.fromString(json.string("user_id") ?: missing("user_id")))
			client.userData = MatrixUserData(
					json.string("access_token") ?: missing("access_token"),
					json.string("device_id") ?: missing("device_id")
			)
			logger.info("Registered new user ${client.id}")
			return client
		}
		
		
		/**
		 * This function checks the given response for error messages and throws an exception
		 * if it finds any errors.
		 *
		 * @throws MatrixErrorResponseException If an error was found
		 */
		@JvmStatic
		@Throws(MatrixErrorResponseException::class)
		fun checkForError(res : HttpResponse)
		{
			try
			{
				val json = res.json
				if (json.containsKey("error"))
				{
					logger.debug("Found error in json: ${res.str}")
					throw MatrixErrorResponseException(json.string("errcode") ?: "MSRD0_UNKNOWN", json.string("error")!!)
				}
			}
			catch (ex : RuntimeException) // unfortunately the json library throws only RuntimeExceptions
			{
				logger.warn("Error while checking for error in response", ex)
			}
			
			val status = res.status
			if (status.family != 2)
				throw MatrixErrorResponseException("${status.status}", status.phrase)
		}
		
		
		
		/** The EventQueue shared by all clients. */
		internal val eventQueue = EventQueue()
		
		/**
		 * Stop the global MatrixEvent Queue. If you didn't specify an event queue for a client, this is the event
		 * queue the client will use.
		 */
		@JvmStatic
		fun stopEventQueue() = eventQueue.stop()
	}
	
	private var queue : EventQueue = eventQueue
	
	/**
	 * Move the client to the given event queue. This method should be called before the `sync()` call, although it is
	 * theoretically possible to change the queue afterwards.
	 *
	 * Please note that the `sync()` method will start the queue if it is not running already, but all other methods
	 * assume that the queue is running.
	 */
	fun moveTo(eventQueue : EventQueue)
	{
		queue = eventQueue
	}
	
	/**
	 * Register a new listener for the given event type.
	 */
	override fun on(type : EventType, l : Listener<*>)
	{
		if (l.javaClass.interfaces.find { it == type.listener } == null)
			throw IllegalArgumentException("The listener of type ${l.javaClass.canonicalName} doesn't implement ${type.listener.canonicalName}")
		queue.addListener(type, l)
	}
	
	/**
	 * Fire an event.
	 */
	internal fun fire(ev : Event)
			= queue.enqueue(ev)
	
	/** The last transaction id used by this client. Use `nextTxnId` if you need a new one. */
	var lastTxnId : Long = -1
	
	/** The next transaction id to use by this client. */
	open val nextTxnId : Long
		get() = ++lastTxnId
	
	/**
	 * A convenient constructor call to create the ClientContext from the given parameters.
	 */
	@JvmOverloads
	constructor(domain : String, localpart : String, hsDomain : String = domain, hsBaseUri : URI = URI("https://$hsDomain/"))
			: this(HomeServer(hsDomain, hsBaseUri), MatrixId(localpart, domain))
	
	/** HTTP MatrixClient */
	internal val target : HttpTarget = DefaultHttpTarget(hs.base, publicTarget.userAgent)
	
	
	/** The user data of this client. */
	var userData : MatrixUserData? = null
	
	/** Access Token */
	val token : String?
		get() = userData?.token
	
	/** The device id of this device. */
	val deviceId : String?
		get() = userData?.deviceId
	
	
	/** The next batch field from sync that should be supplied as the since parameter with the next query. */
	var next_batch : String? = null
		protected set
	
	/** The rooms that this user has joined. */
	private val roomsJoined = HashMap<RoomId, Room>()
	/** The rooms that this user has joined. */
	val rooms get() = roomsJoined.values
	/** The rooms that this user has an invitation for. */
	val roomsInvited = ArrayList<RoomInvitation>()
	
	
	
	/**
	 * Returns a list of authentication options.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer
	 */
	@Throws(MatrixAnswerException::class)
	fun auth() : Collection<Auth> = auth(JsonObject())
	
	/**
	 * Returns the authentication option of the selected login type, or `null`
	 * if it was not found.
	 */
	fun auth(type : LoginType) : Auth?
		= auth().filter { it.loginType == type }.firstOrNull()
	
	/**
	 * Returns a list of authentication options. The json object is passed in the request
	 * body.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer
	 */
	@Throws(MatrixAnswerException::class)
	internal fun auth(jsono : JsonObject) : Collection<Auth>
	{
		val l = HashSet<Auth>()
		val res = if (jsono.isEmpty()) target.get("_matrix/client/r0/login") else target.post("_matrix/client/r0/login", jsono)
		val json = res.json
		checkForError(res)
		
		// if the request contains an access_token field, the auth was successfull
		val access_token = json.string("access_token")
		if (access_token != null)
		{
			userData = MatrixUserData(access_token, json.string("device_id") ?: missing("device_id"))
			l.add(Auth(this, LoginType.SUCCESS))
			logger.debug("$id successfully authenticated")
			return l
		}
		
		// extract interesting values from the json object
		val completed = json.array<String>("completed") ?: JsonArray<String>()
		val flows = json.array<JsonObject>("flows") ?: missing("flows")
		val session = json.string("session")
		
		flows@ for (flow in flows)
		{
			if (flow.containsKey("type"))
			{
				val a = Auth(this, LoginType.fromString(flow.string("type")!!))
				a.setProperty("type", a.loginType.type)
				a.setProperty("user", id.localpart)
				l.add(a)
				continue
			}
			
			val stages = flow.array<String>("stages")!!
			var i : Int = 0
			for (j in completed.indices)
			{
				i = j
				if (stages[i] != completed[i])
					continue@flows
			}
			val a = Auth(this, LoginType.fromString(stages[i]))
			a.setProperty("type", a.loginType.type)
			a.setProperty("user", id.localpart)
			if (session != null)
				a.setProperty("session", session)
			l.add(a)
		}
		
		return l
	}
			
	/**
	 * Logout the current user.
	 */
	fun logout()
	{
		val res = target.get("_matrix/client/r0/logout", token ?: throw NoTokenException(), id)
		checkForError(res)
	}
	
	
	/** The ID of the filter used for sync calls. Empty if no filter was uploaded yet. */
	protected var syncFilter = ""
	
	/**
	 * Uploads the filter used for sync calls and sets the `syncFilter` property accordingly.
	 *
	 * @throws MatrixAnswerException On errors when uploading the filter
	 */
	@Throws(MatrixAnswerException::class)
	fun uploadSyncFilter()
	{
		val f = Filter()
		f.room.state.types = arrayListOf("m.room.*")
		f.room.timeline.types = arrayListOf("m.room.message")
		f.room.ephemeral.notTypes = arrayListOf("*")
		f.presence.notTypes = arrayListOf("*")
		syncFilter = uploadFilter(f)
	}
	
	/**
	 * Synchronize the account.
	 */
	@Throws(MatrixAnswerException::class)
	fun sync()
	{
		// check that the event queue is running
		if (!queue.isRunning)
			queue.start()
		
		// check that our filter is uploaded
		if (syncFilter.isEmpty())
			uploadSyncFilter()
		
		val params = HashMap<String, String>()
		params["filter"] = syncFilter
		params["access_token"] = token ?: throw NoTokenException()
		params["user_id"] = "$id"
		if (next_batch != null)
			params["since"] = next_batch!!
		val res = target.get("_matrix/client/r0/sync", params)
		checkForError(res)
		val json = res.json
		
		next_batch = json.string("next_batch") ?: missing("next_batch")
		
		// parse all rooms and add them
		val rooms = json.obj("rooms") ?: missing("rooms")
		val roomsJoined = rooms.obj("join") ?: missing("rooms.join")
		val roomsInvited = rooms.obj("invite") ?: missing("rooms.invite")
		
		for (room in roomsJoined.keys.map { Room(this, RoomId.fromString(it)) })
		{
			if (this.roomsJoined.containsKey(room.id))
			{
				// TODO there are new messages in this room
				continue
			}
			this.roomsJoined[room.id] = room
			fire(RoomJoinEvent(room))
		}
		
		for (room in roomsInvited.keys.map { RoomInvitation(this, RoomId.fromString(it)) })
		{
			this.roomsInvited.add(room)
			fire(RoomInvitationEvent(room))
		}
		
		// parse to-device events
		parseSyncToDeviceSection(json)
		
		// TODO!!
	}
	
	/** The filter id for the blocking sync call. */
	protected var syncBlockingFilter : String? = null
	
	/**
	 * Uploads the blocking sync filter and sets the `syncBlockingFilter`.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun uploadSyncBlockingFilter()
	{
		val f = Filter()
		// we don't care about accountData, ephemeral or presence events
		f.accountData.notTypes = arrayListOf("*")
		f.room.ephemeral.notTypes = arrayListOf("*")
		f.presence.notTypes = arrayListOf("*")
		// limit the event count to 30
		f.room.timeline.limit = 30
		
		// finally, upload the filter
		syncBlockingFilter = uploadFilter(f)
	}
	
	/** Set to true if there is no [syncBlocking] call at the moment or if it should stop. */
	private var syncBlockingStopped : Boolean = true
	/** Mutex for [syncBlocking]. */
	private var syncBlockingMutex = Mutex()
	
	/** Returns true if the method [syncBlocking] is active. */
	val isSyncBlockingRunning : Boolean get() = !syncBlockingStopped
	
	/**
	 * Stop the [syncBlocking] method. Please note that it might take a while until it will finish; this
	 * depends on the timeout of that function.
	 *
	 * @throws IllegalStateException If no [syncBlocking] call is active.
	 */
	@Throws(IllegalStateException::class)
	fun stopSyncBlocking()
	{
		synchronized(syncBlockingStopped) {
			if (syncBlockingStopped)
				throw IllegalStateException("No active syncBlocking call found")
			syncBlockingStopped = true
		}
	}
	
	/**
	 * Starts a sync request for this client that blocks until an event is available. As soon as an event was received,
	 * a corresponding event will be fired and a new request will be made.
	 *
	 * Please note that at the moment this will only receive room joins, invites and message events. To receive
	 * everything else please call the [sync] method. **This behaviour might change in the future without prior notice!!**
	 *
	 * @param timeout The timeout in milliseconds.
	 *
	 * @throws MatrixAnswerException On errors that happened before the blocking call started. If an error occurred
	 * 		afterwards, it will be logged and a new request will be made.
	 * @throws IllegalStateException When another call to [syncBlocking] is active.
	 */
	@Throws(MatrixAnswerException::class, IllegalStateException::class)
	@JvmOverloads
	suspend fun syncBlocking(timeout : Int = 30000)
	{
		syncBlockingMutex.withLock {
			if (!syncBlockingStopped)
				throw IllegalStateException("Another syncBlocking method is already waiting for events")
			syncBlockingStopped = false
		}
		
		if (syncBlockingFilter == null)
			uploadSyncBlockingFilter()
		
		logger.debug("starting to sync blocking")
		while (!syncBlockingStopped)
		{
			try
			{
				val params = HashMap<String, String>()
				params["access_token"] = token ?: throw NoTokenException()
				params["user_id"] = "$id"
				params["timeout"] = "$timeout"
				params["filter"] = syncBlockingFilter ?: throw IllegalStateException("For some reason syncBlockingFilter is null while it shouldn't be")
				params["since"] = next_batch ?: throw IllegalStateException("Please call sync at least once before calling syncBlocking")
				val res = target.get("_matrix/client/r0/sync", params)
				checkForError(res)
				val json = res.json
				
				next_batch = json.string("next_batch") ?: missing("next_batch")
				val rooms = json.obj("rooms") ?: missing("rooms")
				val join = rooms.obj("join") ?: missing("rooms.join")
				for (roomId in join.keys.map { RoomId.fromString(it) })
				{
					if (!roomsJoined.contains(roomId))
					{
						val room = Room(this, roomId)
						roomsJoined[roomId] = room
						fire(RoomJoinEvent(room))
					}
					val room = roomsJoined[roomId] ?: continue
					val timeline = join.obj("$roomId")?.obj("timeline") ?: missing("timeline")
					val events = timeline.array<JsonObject>("events") ?: missing("timeline.events")
					for (msg in Messages.fromJson(room, events))
						fire(RoomMessageReceivedEvent(room, msg))
				}
				val invite = rooms.obj("invite") ?: missing("rooms.invite")
				for (roomId in invite.keys.map { RoomId.fromString(it) })
				{
					val inv = RoomInvitation(this, roomId)
					roomsInvited.add(inv)
					fire(RoomInvitationEvent(inv))
				}
				
				parseSyncToDeviceSection(json)
			}
			catch (ex : Exception)
			{
				logger.warn("syncBlocking received exception", ex)
			}
		}
	}
	
	/**
	 * Start the [syncBlocking] method in a new coroutine.
	 */
	@JvmOverloads
	fun startSyncBlocking(timeout : Int = 30000)
	{
		launch {
			syncBlocking(timeout)
		}
	}
	
	
	private fun parseSyncToDeviceSection(json : JsonObject)
	{
		val toDevice = json.obj("to_device")?.array<JsonObject>("events") ?: missing("to_device.events")
		eventLoop@for (event in toDevice.map { it as JsonObject })
		{
			val sender = MatrixId.fromString(event.string("sender") ?: missing("sender"))
			val plain : JsonObject
			
			// if the event is encrypted, decrypt the event
			if (event.string("type") == ROOM_ENCRYPTED)
			{
				val content = event.obj("content") ?: missing("content")
				if (content.string("algorithm") != OLM_V1_RATCHET)
				{
					logger.warn("Unknown encryption algorithm ${content.string("algorithm")}")
					continue
				}
				val senderKey = content.string("sender_key") ?: missing("content.sender_key")
				val ciphertext = content.obj("ciphertext") ?: missing("content.ciphertext")
				val account = keyStore?.account ?: throw IllegalStateException("E2E has not been initialised")
				val curve25519 = account.identityKeys().string("curve25519")!!
				val ourCiphertext = ciphertext.obj(curve25519) ?: missing("content.ciphertext.$curve25519")
				val message = OlmMessage(
						ourCiphertext.string("body") ?: missing("content.ciphertext.$curve25519.body"),
						ourCiphertext.int("type") ?: missing("content.ciphertext.$curve25519.type"))
				
				// attempt to decrypt
				var session : OlmSession? = keyStore!!.allSessions().find { it.matchesInboundSession(message.cipherText) }
				if (session == null)
				{
					session = OlmSession()
					session.initInboundSessionFrom(account, senderKey, message.cipherText)
					account.removeOneTimeKeys(session)
				}
				val decrypted = session.decryptMessage(message)
				keyStore!!.account = account
				keyStore!!.storeSession(session)
				plain = Parser().parse(StringBuilder(decrypted)) as JsonObject
			}
			else
				plain = event
			
			
			// parse the plain event
			when (plain.string("type") ?: missing("type"))
			{
				ROOM_KEY -> {
					val content = plain.obj("content") ?: missing("content")
					val session = OlmInboundGroupSession(content.string("session_key") ?: missing("content.session_key"))
					keyStore!!.storeInboundSession(session)
				}
				
				FORWARDED_ROOM_KEY -> {
					val content = ForwardedRoomKeyEventContent(plain.obj("content") ?: missing("content"))
					if (content.algorithm != MEGOLM_V1_RATCHET)
					{
						logger.warn("received forwarded room key for unknown algorithm ${content.algorithm}")
						continue@eventLoop
					}
					// TODO verify the sender of the forwarded room key event
					val session = OlmInboundGroupSession.importSession(content.sessionKey)
					if (session.sessionIdentifier() != content.sessionId)
					{
						logger.warn("received forwarded room key, but session ${content.sessionId} doesn't match imported ${session.sessionIdentifier()}")
						continue@eventLoop
					}
					keyStore!!.storeInboundSession(session)
				}
				
				ROOM_KEY_REQUEST -> {
					val content = RoomKeyRequestEventContent(plain.obj("content") ?: missing("content"))
					if (content.algorithm != MEGOLM_V1_RATCHET)
					{
						logger.warn("received room key request for unknown algorithm ${content.algorithm}")
						continue@eventLoop
					}
					if (content.action != RoomKeyRequestActions.REQUEST)
					{
						logger.warn("received room key request with unknown action ${content.action}")
						continue@eventLoop
					}
					// TODO check that the requested key was issued for that room
					// find the room
					val room = roomsJoined[content.roomId]
					if (room == null)
					{
						logger.warn("received room key request for unknown room ${content.roomId}")
						continue@eventLoop
					}
					// check that the requesting user is in that room
					if (!room.members.contains(sender))
					{
						logger.warn("received room key request from $sender, but he is not a member of ${content.roomId}")
						continue@eventLoop
					}
					// send him the keys
					val session = room.findInboundSession(content.sessionId ?: missing("content.session_id"))
					if (session != null)
					{
						val account = keyStore!!.account
						val curve25519 = account.identityKeys().string("curve25519")!!
						val ed25519 = account.identityKeys().string("ed25519")!!
						
						val roomKey = ForwardedRoomKeyEventContent(
								MEGOLM_V1_RATCHET, room.id, curve25519,
								"", // TODO ed25519 keys of the original sender, not our own
								session.sessionIdentifier(),
								session.export(session.firstKnownIndex),
								chainIndex = session.firstKnownIndex
						)
					}
				}
				
				else -> {
					logger.warn("Received unknown to-device event of type ${plain.string("type")}")
				}
			}
		}
	}
	
	
	
	/**
	 * Retrieve and return the user's presence.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun presence(user : MatrixId) : Presence
	{
		val res = target.get("_matrix/client/r0/presence/$user/status", token ?: throw NoTokenException(), id)
		checkForError(res)
		return Presence.fromJson(user, res.json)
	}
	
	
	/**
	 * Retrieve and return the user's display name.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun displayname(user : MatrixId = id) : String?
	{
		val res = target.get("_matrix/client/r0/profile/$user/displayname") // this api shouldn't require a token
		if (res.status.status == 404)
			return null
		checkForError(res)
		return res.json.string("displayname")
	}
	
	/**
	 * Update the display name of this user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateDisplayname(displayname : String)
	{
		val res = target.put("_matrix/client/r0/profile/$id/displayname", token ?: throw NoTokenException(), id,
				JsonObject(mapOf("displayname" to displayname)))
		checkForError(res)
	}
	
	
	/**
	 * Retrieve and return the user's avatar.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun avatar(user : MatrixId = id) : Avatar?
	{
		val res = target.get("_matrix/client/r0/profile/$user/avatar_url") // this api shouldn't require a token
		if (res.status.status == 404)
			return null
		checkForError(res)
		return Avatar(res.json.string("avatar_url") ?: return null)
	}
	
	/**
	 * Update the avatar of this user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateAvatar(avatar : Avatar)
	{
		val res = target.put("_matrix/client/r0/profile/$id/avatar_url", token ?: throw NoTokenException(), id,
				JsonObject(mapOf("avatar_url" to "${avatar.url}")))
		checkForError(res)
	}
	
	
	
	/** The key store for e2e. */
	var keyStore : KeyStore? = null
		private set
	
	/**
	 * Enable E2E encryption for this client. If the [keyStore] contains no account, one will be created. Please
	 * make sure that you call [uploadIdentityKeys] if necessary.
	 */
	fun enableE2E(keyStore : KeyStore)
	{
		this.keyStore = keyStore
		if (!keyStore.hasAccount())
			keyStore.account = OlmAccount()
	}
	
	
	
	/**
	 * Send an event to a list of user ids and devices. To send the event to all device ids of a certain
	 * user id, one can use a wildcard as device id.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param devices A map of user id to a collection of devices of that user id.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun sendToDevice(ev : MatrixEventContent, evType : String, devices : Map<MatrixId, Collection<String>>)
	{
		val evJson = ev.json
		val messages = JsonObject()
		for ((userId, deviceIds) in devices.filterValues { it.isNotEmpty() })
		{
			val json = JsonObject()
			for (deviceId in deviceIds)
				json[deviceId] = evJson
			messages["$userId"] = json
		}
		if (messages.isEmpty())
		{
			logger.warn("Called sendToDevice without any devices")
			return
		}
		val json = JsonObject(mapOf("messages" to messages))
		
		val res = target.put("_matrix/client/r0/sendToDevice/$evType/$nextTxnId", token ?: throw NoTokenException(), id, json)
		checkForError(res)
	}
	
	/**
	 * Send an event to a list of user ids.
	 *
	 * This call is equivalent to calling [sendToDevice] with the given user ids and a wildcard
	 * as device id.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param users A list of user ids.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun sendToDevice(ev : MatrixEventContent, evType : String, users : Iterable<MatrixId>)
	{
		val devices = HashMap<MatrixId, List<String>>()
		for (user in users)
			devices[user] = listOf("*")
		sendToDevice(ev, evType, devices)
	}
	
	/**
	 * Encrypt and send an event to a list of user ids and devices. To send the event to all device ids of a certain
	 * user id, one can use an empty list of device ids.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param devices A map of user id to a collection of devices of that user id.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 * @throws OlmException On errors while encrypting the message.
	 */
	@Throws(MatrixAnswerException::class, OlmException::class)
	fun sendEncryptedToDevice(ev : MatrixEventContent, evType : String, devices : Map<MatrixId, Collection<String>>)
	{
		val account = keyStore?.account ?: throw IllegalStateException("E2E has not been enabled")
		
		val plain = JsonObject()
		plain["type"] = evType
		plain["content"] = ev.json
		plain["sender"] = "$id"
		plain["sender_device"] = deviceId ?: throw NoDeviceIdException()
		plain["keys"] = account.identityKeys()
		
		val deviceKeys = queryIdentityKeys(devices).toMap()
		val oneTimeKeys = claimOneTimeKeys(devices.mapValues { (_, v) -> if (v.isEmpty()) listOf("*") else v }).toMap()
		
		val messages = JsonObject()
		for ((userId, userDevices) in devices.mapValues { (k, v) -> if (v.isEmpty()) deviceKeys[k]?.keys else v })
		{
			if (userDevices == null)
				continue
			
			val json = JsonObject()
			for (deviceId in userDevices)
			{
				val deviceKey = deviceKeys[userId]?.get(deviceId)?.keys
				val receiverEd25519 = deviceKey?.get("ed25519:$deviceId")
				val receiverCurve25519 = deviceKey?.get("curve25519:$deviceId")
				val oneTimeKey = oneTimeKeys[userId]?.get(deviceId)?.key
				if (receiverEd25519 == null || receiverCurve25519 == null || oneTimeKey == null)
				{
					logger.warn("Unable to find required keys for $userId/$deviceId to send encrypted to-device message")
					continue
				}
				
				plain["recipient"] = "$userId"
				plain["recipient_keys"] = mapOf(
						"ed25519" to receiverEd25519,
						"curve25519" to receiverCurve25519
				)
				
				val session = OlmSession()
				session.initOutboundSession(account, receiverCurve25519, oneTimeKey)
				val message = session.encryptMessage(plain.toJsonString())
				keyStore!!.storeSession(session)
				
				val encrypted = JsonObject()
				encrypted["algorithm"] = OLM_V1_RATCHET
				encrypted["sender_key"] = account.identityKeys().string("curve25519")
				val ciphertext = JsonObject()
				ciphertext["type"] = message.type
				ciphertext["body"] = message.cipherText
				encrypted["ciphertext"] = mapOf(receiverCurve25519 to ciphertext)
				json[deviceId] = encrypted
			}
			messages["$userId"] = json
		}
		if (messages.isEmpty())
		{
			logger.warn("Called sendEncryptedToDevice without any devices")
			return
		}
		
		val json = JsonObject()
		json["messages"] = messages
		
		val res = target.put("_matrix/client/r0/sendToDevice/$ROOM_ENCRYPTED/$nextTxnId", token ?: throw NoTokenException(), id, json)
		checkForError(res)
	}
	
	/**
	 * Send an event to a list of user ids.
	 *
	 * This call is equivalent to calling [sendEncryptedToDevice] with the given user ids and a wildcard
	 * as device id.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param users A list of user ids.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun sendEncryptedToDevice(ev : MatrixEventContent, evType : String, users : Iterable<MatrixId>)
	{
		val devices = HashMap<MatrixId, List<String>>()
		for (user in users)
			devices[user] = listOf("*")
		sendEncryptedToDevice(ev, evType, devices)
	}
	
	
	/**
	 * Return all devices from the current user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun devices() : List<Device>
	{
		val res = target.get("_matrix/client/r0/devices", token ?: throw NoTokenException(), id)
		checkForError(res)
		return res.json.array<JsonObject>("devices")
				?.map { Device(it) }
				?: missing("devices")
	}
	
	/**
	 * Return a particular device from the current user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun device(deviceId : String) : Device?
	{
		val res = target.get("_matrix/client/r0/devices/$deviceId", token ?: throw NoTokenException(), id)
		if (res.status.status == 404)
			return null
		checkForError(res)
		return Device(res.json)
	}
	
	/**
	 * Update the display name of a certain device of the current user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun updateDeviceDisplayName(deviceId : String, displayName : String)
	{
		val res = target.put("_matrix/client/r0/devices/$deviceId", token ?: throw NoTokenException(), id,
				JsonObject(mapOf("display_name" to displayName)))
		checkForError(res)
	}
	
	/**
	 * Delete a certain device of the current user. Note that this might require authentication with the
	 * matrix server, although a valid access token is present. It is recommended to use at least the
	 * `DefaultFlowHelper` with the password of the user.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 * @throws UnsupportedFlowsException If the flow helper can't authenticate.
	 */
	@Throws(MatrixAnswerException::class, UnsupportedFlowsException::class)
	fun deleteDevice(deviceId : String, helper : FlowHelper = DefaultFlowHelper())
	{
		val json = JsonObject()
		var res = target.delete("_matrix/client/r0/devices/$deviceId", json)
		
		while (res.status.status == 401 && res.json.containsKey("flows"))
		{
			val flowResponse = helper.answer(FlowRequest.fromJson(res.json))
			json["auth"] = flowResponse.json
			res = target.delete("_matrix/client/r0/devices/$deviceId", json)
		}
		
		checkForError(res)
	}
	
	
	/**
	 * Create a new room.
	 *
	 * @param name If not null, set the `m.room.name` event.
	 * @param topic If not null, set the `m.room.topic` event.
	 * @param public If true, this room will be published to the room list.
	 * @return The created room.
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	@JvmOverloads
	fun createRoom(name : String? = null, topic : String? = null, public : Boolean = false) : Room
	{
		val json = JsonObject()
		if (name != null)
			json["name"] = name
		if (topic != null)
			json["topic"] = topic
		json["preset"] = if (public) "public_chat" else "private_chat"
		val res = target.post("_matrix/client/r0/createRoom", token ?: throw NoTokenException(), id, json)
		checkForError(res)
		return Room(this, RoomId.fromString(res.json.string("room_id") ?: missing("room_id")))
	}
	
	
	
	/**
	 * Upload the identity keys to the homeserver. The identity keys will be retrieved from the [keyStore].
	 * Make sure to call [enableE2E] to populate the key store!
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 * @throws OlmException On errors while signing the keys.
	 */
	@Throws(MatrixAnswerException::class, OlmException::class)
	fun uploadIdentityKeys()
	{
		val json = JsonObject()
		val deviceKeys = DeviceKeys(id,
				deviceId ?: throw NoDeviceIdException(),
				EncryptionAlgorithms.ALGORITHMS,
				keyStore?.account?.identityKeys()
						?.mapKeys { (k, _) -> "$k:$deviceId" }
						?.mapValues { (_, v) -> v as String }
						?: throw IllegalStateException()
		)
		deviceKeys.sign(keyStore?.account ?: throw IllegalStateException(), id, deviceId ?: throw NoDeviceIdException())
		json["device_keys"] = deviceKeys.json
		val res = target.post("_matrix/client/r0/keys/upload", token ?: throw NoTokenException(), id, json)
		checkForError(res)
	}
	
	/**
	 * Query the identity keys for the given list of user and device ids from the homeserver.
	 *
	 * If the list of devices is empty for one or more user ids, all device ids for that user will
	 * be queried.
	 *
	 * @param devices A map of user id to list of device ids.
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun queryIdentityKeys(devices : Map<MatrixId, Iterable<String>>) : List<DeviceKeys>
	{
		val json = JsonObject()
		val keys = JsonObject()
		for ((user, userDevices) in devices)
			keys["$user"] = JsonArray<String>(userDevices.toMutableList())
		if (keys.isEmpty())
		{
			logger.warn("Called queryIdentityKeys without any devices")
			return emptyList()
		}
		json["device_keys"] = keys
		val res = target.post("_matrix/client/r0/keys/query", token ?: throw NoTokenException(), id, json)
		checkForError(res)
		return (res.json.obj("device_keys") ?: missing("device_keys"))
				.map { (_, obj) -> obj as JsonObject } // returns a list of all user objects
				.flatMap { it.map { (_, obj) -> obj as JsonObject } } // returns a list of all device objects
				.map { deviceJson -> DeviceKeys(deviceJson) } // maps the device objects to a DeviceKey
	}
	
	/**
	 * Query the identity keys for the given list of user ids from the homeserver.
	 *
	 * @param users The user ids to query.
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun queryIdentityKeys(users : Iterable<MatrixId>) : List<DeviceKeys>
	{
		val devices = HashMap<MatrixId, List<String>>()
		for (user in users)
			devices[user] = emptyList()
		return queryIdentityKeys(devices)
	}
	
	/**
	 * Query the amount of currently stored one-time keys.
	 *
	 * @return A map of key algorithm to amount of one-time keys.
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun oneTimeKeyCounts() : Map<String, Int>
	{
		val res = target.post("_matrix/client/r0/keys/upload", token ?: throw NoTokenException(), id)
		checkForError(res)
		return (res.json.obj("one_time_key_counts") ?: missing("one_time_key_counts"))
				.mapValues { (_, it) -> it as? Int ?: (it as Number).toInt() }
	}
	
	
	/** Set to true if there is no [updateOneTimeKeysBlocking] call at the moment or if it should stop. */
	private var updateOneTimeKeysBlockingStopped : Boolean = true
	/** Mutex for [updateOneTimeKeysBlocking]. */
	private var updateOneTimeKeysBlockingMutex = Mutex()
	
	/** Returns true if the method [updateOneTimeKeysBlocking] is active. */
	val isUpdateOneTimeKeysBlockingRunning : Boolean get() = !updateOneTimeKeysBlockingStopped
	
	/**
	 * Start the [updateOneTimeKeysBlocking] method in a coroutine.
	 *
	 * @throws IllegalStateException If the [updateOneTimeKeysBlocking] method is already running.
	 */
	@JvmOverloads
	@Throws(IllegalStateException::class)
	fun startUpdateOneTimeKeysBlocking(interval : Long = 600000)
	{
		if (isUpdateOneTimeKeysBlockingRunning)
			throw IllegalStateException("updateOneTimeKeysBlocking() is already running")
		launch {
			updateOneTimeKeysBlocking(interval)
		}
	}
	
	/**
	 * Update the one time keys in the given [interval].
	 *
	 * @param interval The interval to wait between updates to the one time keys.
	 */
	@JvmOverloads
	suspend fun updateOneTimeKeysBlocking(interval : Long = 600000)
	{
		updateOneTimeKeysBlockingMutex.withLock {
			updateOneTimeKeysBlockingStopped = false
		}
		
		while (isUpdateOneTimeKeysBlockingRunning)
		{
			updateOneTimeKeys()
			delay(interval, MILLISECONDS)
		}
	}
	
	/**
	 * Update the one time keys. The client will try to manage half the maximum amount of supported one-time
	 * keys by olm.
	 */
	@Throws(OlmException::class)
	fun updateOneTimeKeys()
	{
		val counts = oneTimeKeyCounts()
		val signedCurve25519 = counts["signed_curve25519"] ?: 0
		val account = keyStore?.account ?: throw IllegalStateException()
		val intendedKeys = (account.maxOneTimeKeys() / 2).toInt()
		
		if (signedCurve25519 < intendedKeys)
		{
			account.generateOneTimeKeys(intendedKeys - signedCurve25519)
			val keys = account.oneTimeKeys()
			
			val oneTimeKeysJson = JsonObject()
			for ((algo, obj) in keys.mapValues { (_, it) -> it as JsonObject })
			{
				for ((name, key) in obj.mapValues { (_, it) -> it as String })
				{
					val json = JsonObject()
					json["key"] = key
					val signature = account.signMessage(json.toJsonString(canonical = true))
					val signatures = JsonObject()
					signatures["$id"] = mapOf("ed25519:$deviceId" to signature)
					json["signatures"] = signatures
					oneTimeKeysJson["signed_$algo:$name"] = json
				}
			}
			uploadOneTimeKeys(oneTimeKeysJson)
			account.markOneTimeKeysAsPublished()
			
			// store the account since it has been modified
			keyStore!!.account = account
		}
	}
	
	/**
	 * Upload the one-time keys json object to the homeserver.
	 *
	 * See also [updateOneTimeKeys] and [updateOneTimeKeysBlocking].
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@Throws(MatrixAnswerException::class)
	fun uploadOneTimeKeys(oneTimeKeysJson : JsonObject)
	{
		val json = JsonObject()
		json["one_time_keys"] = oneTimeKeysJson
		val res = target.post("_matrix/client/r0/keys/upload", token ?: throw NoTokenException(), id, json)
		checkForError(res)
	}
	
	
	/**
	 * Claim a one-time key for the given list of user ids and devices.
	 *
	 * @param devices A map of user id to list of device ids.
	 * @param algorithm The algorithm of the one-time key.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	fun claimOneTimeKeys(devices : Map<MatrixId, Collection<String>>, algorithm : String = "signed_curve25519") : List<OneTimeKey>
	{
		val json = JsonObject()
		for ((user, userDevices) in devices.filterValues { it.isNotEmpty() })
		{
			val userJson = JsonObject()
			for (device in userDevices)
				userJson[device] = algorithm
			json["$user"] = userJson
		}
		if (json.isEmpty())
		{
			logger.warn("Called claimOneTimeKeys without any device")
			return emptyList()
		}
		
		val res = target.post("_matrix/client/r0/keys/claim", token ?: throw NoTokenException(), id,
				JsonObject(mapOf("one_time_keys" to json)))
		checkForError(res)
		
		val oneTimeKeysJson = res.json.obj("one_time_keys") ?: missing("one_time_keys")
		val oneTimeKeys = ArrayList<OneTimeKey>()
		for ((userId, o0) in oneTimeKeysJson.map { (key, value) -> MatrixId.fromString(key) to (value as JsonObject) })
			for ((deviceId, o1) in o0.mapValues { (_, value) -> value as JsonObject })
				for ((keyName, o2) in o1.mapValues { (_, value) -> value as JsonObject })
					oneTimeKeys.add(OneTimeKey(userId, deviceId, keyName, o2))
		return oneTimeKeys
	}
	
	
	/**
	 * Send a [ROOM_KEY_REQUEST] for the given [roomId] and [sessionId] to the [receivers].
	 */
	@Throws(MatrixAnswerException::class)
	fun requestRoomKey(roomId : RoomId, sessionId : String, receivers : Collection<MatrixId>)
	{
		if (receivers.isEmpty())
		{
			logger.warn("receivers of room key request is empty, not sending anything")
			return
		}
		
		val account = keyStore!!.account
		val curve25519 = account.identityKeys().string("curve25519")!!
		
		val request = RoomKeyRequestEventContent(
				RoomKeyRequestActions.REQUEST, deviceId ?: throw NoDeviceIdException(),
				"$roomId$sessionId${System.currentTimeMillis()}".toUtf8().md5().toUnpaddedBase64(),
				roomId, MEGOLM_V1_RATCHET, curve25519, sessionId
		)
		
		println("Sending ${request.json.toJsonString(prettyPrint = true)} to $receivers")
		sendToDevice(request, ROOM_KEY_REQUEST, receivers)
	}
}

@Deprecated("Use MatrixClient instead")
open class Client(hs : HomeServer, id : MatrixId) : MatrixClient(hs, id)
{
	@JvmOverloads
	@Suppress("DEPRECATION")
	constructor(domain : String, localpart : String, hsDomain : String = domain, hsBaseUri : URI = URI("https://$hsDomain/"))
			: this(HomeServer(hsDomain, hsBaseUri), MatrixId(localpart, domain))
}
