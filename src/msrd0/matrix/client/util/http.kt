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

@file:JvmName("HttpUtil")
package msrd0.matrix.client.util

import com.beust.klaxon.*
import msrd0.matrix.client.MatrixId
import org.slf4j.*
import java.io.InputStream
import java.lang.Math.*
import java.net.URI
import java.nio.charset.StandardCharsets.*

typealias DefaultHttpTarget = OkHttpTarget

abstract class HttpTarget(
		val uri : URI,
		val userAgent : String)
{
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(HttpTarget::class.java)
		
		val accessTokenRegex = Regex("access_token=([^\\s&/?]+)")
	}
	
	/**
	 * Log this request.
	 */
	fun log(method : String, uri : URI)
			= logger.info(method.toUpperCase() + " " + uri.toString().replace(accessTokenRegex, "access_token=redacted"))
	
	
	/**
	 * Run a GET request on the given path without any arguments.
	 */
	open fun get(path : String) : HttpResponse
			= get(path, emptyMap())
	
	/**
	 * Run a GET request on the given path using the supplied token and user id.
	 */
	open fun get(path : String, token : String, userId : MatrixId?) : HttpResponse
	{
		val args = HashMap<String, String>()
		args["access_token"] = token
		if (userId != null)
			args["user_id"] = "$userId"
		return get(path, args)
	}
	
	/**
	 * Run a GET request on the given path using the map of arguments.
	 */
	abstract fun get(path : String, args : Map<String, String>) : HttpResponse
	
	
	/**
	 * Run a POST request on the given path with a json body.
	 */
	open fun post(path : String, body : JsonBase = JsonObject()) : HttpResponse
			= post(path, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a POST request on the given path with a json body using the supplied token and user id.
	 */
	open fun post(path : String, token : String, userId : MatrixId?, body : JsonBase = JsonObject()) : HttpResponse
			= post(path, token, userId, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a POST request on the given path with an arbitrary body.
	 */
	abstract fun post(path : String, body : ByteArray, type : String) : HttpResponse
	
	/**
	 * Run a POST request on the given path with an arbitrary body using the supplied token and user id.
	 */
	abstract fun post(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	
	
	/**
	 * Run a PUT request on the given path with a json body.
	 */
	open fun put(path : String, body : JsonBase = JsonObject()) : HttpResponse
			= put(path, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a PUT request on the given path with a json body using the supplied token and user id.
	 */
	open fun put(path : String, token : String, userId : MatrixId?, body : JsonBase = JsonObject()) : HttpResponse
			= put(path, token, userId, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a PUT request on the given path with an arbitrary body.
	 */
	abstract fun put(path : String, body : ByteArray, type : String) : HttpResponse
	
	/**
	 * Run a PUT request on the given path with an arbitrary body using the supplied token and user id.
	 */
	abstract fun put(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	
	
	/**
	 * Run a DELETE request on the given path with a json body.
	 */
	open fun delete(path : String, body : JsonBase = JsonObject()) : HttpResponse
			= delete(path, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a DELETE request on the given path with a json body using the supplied token and user id.
	 */
	open fun delete(path : String, token : String, userId : MatrixId?, body : JsonBase = JsonObject()) : HttpResponse
			= delete(path, token, userId, body.toJsonString(prettyPrint = false).toByteArray(UTF_8), "application/json;charset=utf-8")
	
	/**
	 * Run a DELETE request on the given path with an arbitrary body.
	 */
	abstract fun delete(path : String, body : ByteArray, type : String) : HttpResponse
	
	/**
	 * Run a DELETE request on the given path with an arbitrary body using the supplied token and user id.
	 */
	abstract fun delete(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
}

data class HttpStatusInfo(
		val status : Int,
		val family : Int,
		val phrase : String
)
{
	constructor(status : Int, phrase : String) : this(status, floorDiv(status, 100), phrase)
}

abstract class HttpResponse
{
	abstract val status : HttpStatusInfo
	abstract fun header(name : String) : String?
	
	abstract val stream : InputStream
	abstract val bytes : ByteArray
	abstract val str : String
	abstract val json : JsonObject
}
