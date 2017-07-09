package msrd0.matrix.client

import com.beust.klaxon.*
import org.slf4j.*
import java.io.StringReader
import javax.ws.rs.client.*
import javax.ws.rs.core.MediaType.*
import javax.ws.rs.core.*

open class Client(val context : Context)
{
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(Client::class.java)
	}
	
	protected val target : WebTarget = ClientBuilder.newClient().target(context.hs.base)
	
	var token : String? = null
	
	
	@Throws(MatrixErrorResponseException::class)
	protected fun checkForError(json : JsonObject)
	{
		if (json.containsKey("error"))
			throw MatrixErrorResponseException(json.string("error")!!)
	}
	
	
	@Throws(MatrixAnswerException::class)
	fun auth() : Collection<Auth> = auth(JsonObject())
	
	@Throws(MatrixAnswerException::class)
	internal fun auth(json : JsonObject) : Collection<Auth>
	{
		val l = HashSet<Auth>()
		val res = if (json.isEmpty()) target.get("_matrix/client/r0/login") else target.post("_matrix/client/r0/login", json)
		logger.debug("login response: ${res.str}")
		checkForError(res.json)
		
		val access_token = res.json.string("access_token")
		if (access_token != null)
		{
			token = access_token
			l.add(Auth(this, LoginType.SUCCESS))
			return l
		}
		
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
			for (i in completed.indices)
				if (stages[i] != completed[i])
					continue@flows
			val a = Auth(this, LoginType.fromString(stages[i]))
			a.setProperty("type", a.loginType.type)
			a.setProperty("user", context.id.localpart)
			if (session != null)
				a.setProperty("session", session)
			l.add(a)
		}
		
		return l
	}
	
	protected fun queryUrl(url : String) = url + (if (url.contains('?')) "&" else "?") + "&access_token=$token"
	
	fun logout()
	{
		target.get(queryUrl("/_matrix/client/r0/logout"))
	}
}

// extensions to the http lib

fun WebTarget.get(path : String) : Response
		= path(path).request().get()
fun WebTarget.post(path : String, body : JsonBase) : Response
		= path(path).request().post(Entity.entity(body.toJsonString(prettyPrint = false), APPLICATION_JSON_TYPE))

val Response.str : String
		get() = readEntity(String::class.java)
val Response.json : JsonObject
		get() = Parser().parse(StringReader(str)) as JsonObject
