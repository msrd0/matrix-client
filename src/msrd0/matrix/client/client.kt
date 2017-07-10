package msrd0.matrix.client

import com.beust.klaxon.*
import org.slf4j.*
import java.io.StringReader
import java.net.URLEncoder
import javax.ws.rs.client.*
import javax.ws.rs.core.MediaType.*
import javax.ws.rs.core.*

/**
 * This class is the http client for the matrix server.
 */
open class Client(val context : ClientContext)
{
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(Client::class.java)
	}
	
	/** HTTP Client */
	internal val target : WebTarget = ClientBuilder.newClient().target(context.hs.base)
	
	/** Access Token */
	var token : String? = null
	/** The next batch field from sync that should be supplied as the since parameter with the next query. */
	var next_batch : String? = null
		protected set
	
	/** The rooms that this user has joined. */
	val rooms = ArrayList<Room>()
	/** The rooms that this user has an invitation for. */
	val roomsInvited = ArrayList<Room>()
	
	
	/**
	 * This function checks the given response for error messages and throws an exception
	 * if it finds any errors.
	 *
	 * @throws MatrixErrorResponseException If an error was found
	 */
	@Throws(MatrixErrorResponseException::class)
	internal fun checkForError(res : Response)
	{
		val json = res.json
		if (json.containsKey("error"))
		{
			logger.debug("Found error in json: ${res.str}")
			throw MatrixErrorResponseException(json.string("errcode") ?: "MSRD0_UNKNOWN", json.string("error")!!)
		}
	}
	
	
	/**
	 * Returns a list of authentication options.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer
	 */
	@Throws(MatrixAnswerException::class)
	fun auth() : Collection<Auth> = auth(JsonObject())
	
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
			logger.debug("$context successfully authenticated")
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
				a.setProperty("user", context.id.localpart)
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
			a.setProperty("user", context.id.localpart)
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
		val res = target.get("_matrix/client/r0/logout", token ?: throw NoTokenException())
		checkForError(res)
	}
	
	
	/**
	 * Processes the rooms hash and returns a list containing all rooms.
	 */
	protected fun processRooms(hash : JsonObject) : List<Room>
			= hash.keys.map { Room(this, RoomId.fromString(it)) }
	
	/**
	 * Synchronize the account.
	 */
	fun sync()
	{
		val params = HashMap<String, Any>()
		params["access_token"] = token ?: throw NoTokenException()
		if (next_batch != null)
			params["since"] = next_batch!!
		val res = target.get("_matrix/client/r0/sync", params)
		checkForError(res)
		
		next_batch = res.json.string("next_batch") ?: throw IllegalJsonException("Missing: 'next_batch'")
		
		// parse all rooms and add them
		val rooms = res.json.obj("rooms") ?: throw IllegalJsonException("Missing: 'rooms'")
		val roomsJoined = rooms.obj("join") ?: throw IllegalJsonException("Missing: 'rooms.join'")
		val roomsInvited = rooms.obj("invite") ?: throw IllegalJsonException("Missing: 'rooms.invite'")
		this.rooms.addAll(processRooms(roomsJoined))
		this.roomsInvited.addAll(processRooms(roomsInvited))
		
		// TODO!!
	}
}

// extensions to the http lib

/** Run a GET request on the given path. */
fun WebTarget.get(path : String) : Response
		= path(path).request().get()
fun WebTarget.get(path : String, token : String) : Response
		= path(path).queryParam("access_token", token).request().get()
fun WebTarget.get(path : String, args : Map<String, Any>) : Response
{
	var t = path(path)
	for (key in args.keys)
		t = t.queryParam(key, args[key])
	return t.request().get()
}
/** Run a POST request on the given path. */
fun WebTarget.post(path : String, body : JsonBase) : Response
		= path(path).request().post(Entity.entity(body.toJsonString(prettyPrint = false), APPLICATION_JSON_TYPE))

/** Return the response body as a string. */
val Response.str : String
		get() = readEntity(String::class.java)
/** Return the response body as a json object. */
val Response.json : JsonObject
		get() = Parser().parse(StringReader(str)) as JsonObject
