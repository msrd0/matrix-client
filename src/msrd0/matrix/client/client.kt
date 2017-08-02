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
import msrd0.matrix.client.event.*
import msrd0.matrix.client.filter.*
import msrd0.matrix.client.listener.*
import msrd0.matrix.client.util.*
import org.slf4j.*
import java.net.URI
import kotlin.concurrent.thread

/**
 * This class is the http client for the matrix server.
 */
open class Client(val hs : HomeServer, val id : MatrixId) : ListenerRegistration
{
	companion object
	{
		private val logger : Logger = LoggerFactory.getLogger(Client::class.java)
		
		
		
		/**
		 * A public target to the matrix.org server. Should only be used for user-independent calls. All other calls
		 * should be submitted directly to the homeserver of that user.
		 */
		@JvmStatic
		val publicTarget : HttpTarget = CxfHttpTarget(URI("https://matrix.org/"),
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
		fun register(localpart : String, hs : HomeServer, password : String, helper : FlowHelper = DefaultFlowHelper(password)) : Client
		{
			var json = JsonObject()
			json["username"] = localpart
			json["password"] = password
			json["bind_email"] = false
			
			val target = CxfHttpTarget(hs.base, publicTarget.userAgent)
			var res = target.post("_matrix/client/r0/register", json)
			
			while (res.status.status == 401 && res.json.containsKey("flows"))
			{
				val flowResponse = helper.answer(FlowRequest.fromJson(res.json))
				json["auth"] = flowResponse.json
				res = target.post("_matrix/client/r0/register", json)
			}
			
			checkForError(res)
			
			json = res.json
			
			val client = Client(hs, MatrixId.fromString(json.string("user_id") ?: throw IllegalJsonException("Missing: 'user_id'")))
			client.token = json.string("access_token") ?: throw IllegalJsonException("Missing: 'access_token'")
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
		fun registerFromAs(localpart : String, hs : HomeServer, token : String) : Client
		{
			var json = JsonObject()
			json["username"] = localpart
			json["type"] = "m.login.application_service"
			
			val target = CxfHttpTarget(hs.base, publicTarget.userAgent)
			val res = target.post("_matrix/client/r0/register", token, null, json)
			checkForError(res)
			
			json = res.json
			
			val client = Client(hs, MatrixId.fromString(json.string("user_id") ?: throw IllegalJsonException("Missing: 'user_id'")))
			client.token = json.string("access_token") ?: throw IllegalJsonException("Missing: 'access_token'")
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
	
	/** The last transaction id used by this client. */
	private var lastTxnId : Long = -1
	
	/** The next transaction id to use by this client. */
	val nextTxnId : Long
		get() = ++lastTxnId
	
	/**
	 * A convenient constructor call to create the ClientContext from the given parameters.
	 */
	@JvmOverloads
	constructor(domain : String, localpart : String, hsDomain : String = domain, hsBaseUri : URI = URI("https://$hsDomain/"))
			: this(HomeServer(hsDomain, hsBaseUri), MatrixId(localpart, domain))
	
	/** HTTP Client */
	internal val target : HttpTarget = CxfHttpTarget(hs.base, publicTarget.userAgent)
	
	/** Access Token */
	var token : String? = null
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
	internal fun auth(json : JsonObject) : Collection<Auth>
	{
		val l = HashSet<Auth>()
		val res = if (json.isEmpty()) target.get("_matrix/client/r0/login") else target.post("_matrix/client/r0/login", json)
		checkForError(res)
		
		// if the request contains an access_token field, the auth was successfull
		val access_token = res.json.string("access_token")
		if (access_token != null)
		{
			token = access_token
			l.add(Auth(this, LoginType.SUCCESS))
			logger.debug("$id successfully authenticated")
			return l
		}
		
		// extract interesting values from the json object
		val completed = res.json.array<String>("completed") ?: JsonArray<String>()
		val flows = res.json.array<JsonObject>("flows") ?: throw IllegalJsonException("Missing: 'flows'")
		val session = res.json.string("session")
		
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
		
		next_batch = res.json.string("next_batch") ?: throw IllegalJsonException("Missing: 'next_batch'")
		
		// parse all rooms and add them
		val rooms = res.json.obj("rooms") ?: throw IllegalJsonException("Missing: 'rooms'")
		val roomsJoined = rooms.obj("join") ?: throw IllegalJsonException("Missing: 'rooms.join'")
		val roomsInvited = rooms.obj("invite") ?: throw IllegalJsonException("Missing: 'rooms.invite'")
		
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
	
	/** Set to true if there is no `syncBlocking` call at the moment or if it should stop. */
	private var syncBlockingStopped : Boolean = true
	
	/** Returns true if the method `retrieveMessagesBlocking` is active. */
	val isSyncBlockingRunning : Boolean get() = !syncBlockingStopped
	
	/**
	 * Stop the `syncBlocking` method. Please note that it might take a while until it will finish; this
	 * depends on the timeout of that function.
	 *
	 * @throws IllegalStateException If no `syncBlocking` call is active.
	 */
	@Throws(IllegalStateException::class)
	fun stopSyncBlocking()
	{
		synchronized(syncBlockingStopped) {
			if (syncBlockingStopped)
				throw IllegalStateException("No active retrieveMessagesBlocking call found")
			syncBlockingStopped = true
		}
	}
	
	/**
	 * Starts a sync request for this client that blocks until an event is available. As soon as an event was received,
	 * a corresponding event will be fired and a new request will be made. If `threaded` is false, this method will
	 * never return. Otherwise it will run in a new thread.
	 *
	 * Please note that at the moment this will only receive room joins, invites and message events. To receive
	 * everything else please call the `sync` method. **This behaviour might change in the future without prior notice!!**
	 *
	 * @param timeout The timeout in milliseconds.
	 * @param threaded Controls whether this method will run in a new thread or in the current thread.
	 *
	 * @throws MatrixAnswerException On errors that happened before the blocking call started. If an error occurred
	 * 		afterwards, it will be logged and a new request will be made.
	 */
	@Throws(MatrixAnswerException::class)
	@JvmOverloads
	fun syncBlocking(timeout : Int = 30000, threaded : Boolean = true)
	{
		if (threaded)
		{
			thread(start = true) {
				syncBlocking(timeout, false)
			}
			return
		}
		
		synchronized(syncBlockingStopped) {
			if (!syncBlockingStopped)
				throw IllegalStateException("Another thread is already waiting for events")
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
				next_batch = json.string("next_batch") ?: throw IllegalJsonException("Missing: 'next_batch'")
				val rooms = json.obj("rooms") ?: throw IllegalJsonException("Missing: 'rooms'")
				val join = rooms.obj("join") ?: throw IllegalJsonException("Missing: 'rooms.join'")
				for (roomId in join.keys.map { RoomId.fromString(it) })
				{
					if (!roomsJoined.contains(roomId))
					{
						val room = Room(this, roomId)
						roomsJoined[roomId] = room
						fire(RoomJoinEvent(room))
					}
					val room = roomsJoined[roomId] ?: continue
					val timeline = join.obj("$roomId")?.obj("timeline") ?: throw IllegalJsonException("Missing: 'timeline'")
					val events = timeline.array<JsonObject>("events") ?: throw IllegalJsonException("Missing: 'timeline.events'")
					for (event in events)
						fire(RoomMessageEvent(room, Message.fromJson(room, event)))
				}
				val invite = rooms.obj("invite") ?: throw IllegalJsonException("Missing: 'rooms.invite'")
				for (roomId in invite.keys.map { RoomId.fromString(it) })
				{
					val inv = RoomInvitation(this, roomId)
					roomsInvited.add(inv)
					fire(RoomInvitationEvent(inv))
				}
			}
			catch (ex : MatrixAnswerException)
			{
				logger.warn("syncBlocking received exception", ex)
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
	 * Send an event to a list of user ids and devices. To send the event to all device ids of a certain
	 * user id, one can use a wildcard as device id.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param devices A map of user id to a collection of devices of that user id.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	fun sendToDevice(ev : MatrixEventContent, evType : String, devices : Map<MatrixId, Collection<String>>)
	{
		val evJson = ev.json
		val messages = JsonObject()
		for ((userId, deviceIds) in devices)
		{
			val json = JsonObject()
			for (deviceId in deviceIds)
				json[deviceId] = evJson
			messages["$userId"] = json
		}
		val json = JsonObject(mapOf("messages" to messages))
		
		val res = target.put("_matrix/client/unstable/sendToDevice/$evType/$nextTxnId", json)
		checkForError(res)
	}
	
	/**
	 * Send an event to a list of user ids.
	 *
	 * This call is equivalent to calling `sendToDevice` with the given user ids and a wildcard
	 * as device id.
	 *
	 * @param ev The event content to send.
	 * @param evType The type of the event, for example `m.new_device`.
	 * @param users A list of user ids.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer.
	 */
	fun sendToDevice(ev : MatrixEventContent, evType : String, users : Collection<MatrixId>)
	{
		val devices = HashMap<MatrixId, Collection<String>>()
		for (user in users)
			devices[user] = listOf("*")
		sendToDevice(ev, evType, devices)
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
		return Room(this, RoomId.fromString(res.json.string("room_id") ?: throw IllegalJsonException("Missing: 'room_id'")))
	}
}
