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
import org.apache.commons.io.IOUtils
import org.slf4j.*
import java.io.InputStream
import javax.ws.rs.client.*
import javax.ws.rs.core.*


private val logger : Logger = LoggerFactory.getLogger("msrd0.matrix.client")
private val accessTokenRegex = Regex("access_token=([^\\s&/?]+)")


/**
 * A simple interface that has an abstract property `json`.
 */
interface JsonSerializable
{
	val json : JsonObject
}


private fun jsonEntity(body : JsonBase)
		= Entity.entity(body.toJsonString(prettyPrint = false), MediaType.APPLICATION_JSON_TYPE)

/** Run a GET request on the given path. */
fun WebTarget.get(path : String) : Response
		= path(path).log().request().get()
fun WebTarget.get(path : String, token : String, userId : MatrixId?) : Response
		= path(path).queryParam("access_token", token).queryParam("user_id", "$userId").log().request().get()
fun WebTarget.get(path : String, args : Map<String, Any>) : Response
{
	var t = path(path)
	for (key in args.keys)
		t = t.queryParam(key, args[key])
	return t.log().request().get()
}

/** Run a POST request on the given path. */
fun WebTarget.post(path : String, body : JsonBase = JsonObject()) : Response
		= path(path).log().request().post(jsonEntity(body))
fun WebTarget.post(path : String, token : String, userId : MatrixId?, body : JsonBase = JsonObject()) : Response
		= path(path).queryParam("access_token", token).queryParam("user_id", "$userId").log().request().post(jsonEntity(body))
fun <T> WebTarget.post(path : String, token : String, userId : MatrixId?, body : Entity<T>) : Response
		= path(path).queryParam("access_token", token).queryParam("user_id", "$userId").log().request().post(body)

/** Run a PUT request on the given path. */
fun WebTarget.put(path : String, body : JsonBase = JsonObject()) : Response
		= path(path).log().request().put(jsonEntity(body))
fun WebTarget.put(path : String, token : String, userId : MatrixId?, body : JsonBase = JsonObject()) : Response
		= path(path).queryParam("access_token", token).queryParam("user_id", "$userId").log().request().put(jsonEntity(body))

/** Log this request */
fun WebTarget.log() : WebTarget
{
	logger.info(uri.toString().replace(accessTokenRegex, "access_token=redacted"))
	return this
}


/** Return the response body as a byte array. Make sure to call this only once as it will consume the InputStream. */
val Response.bytes : ByteArray
	get() = IOUtils.toByteArray(this.readEntity(InputStream::class.java))
/** Return the response body as a string. */
val Response.str : String
	get() = readEntity(String::class.java)
/** Return the response body as a json object. */
val Response.json : JsonObject
	get() = Parser().parse(StringBuilder(str)) as JsonObject
