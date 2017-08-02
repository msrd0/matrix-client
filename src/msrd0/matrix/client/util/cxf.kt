/*
 matrix-client
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client.util

import com.beust.klaxon.*
import msrd0.matrix.client.MatrixId
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.net.URI
import javax.ws.rs.client.*
import javax.ws.rs.client.Entity.*
import javax.ws.rs.core.Response

private fun Invocation.Builder.ua(ua : String) : Invocation.Builder
		= header("User-Agent", ua)

/**
 * A `HttpTarget` implementation using Apache CXF.
 */
class CxfHttpTarget(uri : URI, userAgent : String) : HttpTarget(uri, userAgent)
{
	private val target : WebTarget = ClientBuilder.newBuilder().build().target(uri)
	
	override fun get(path : String, args : Map<String, String>) : HttpResponse
	{
		var t = target.path(path)
		for ((key, value) in args)
			t = t.queryParam(key, value)
		log("GET", t.uri)
		
		return CxfHttpResponse(t.request().ua(userAgent).get())
	}
	
	override fun post(path : String, body : ByteArray, type : String) : HttpResponse
	{
		val t = target.path(path)
		log("POST", t.uri)
		return CxfHttpResponse(t.request().ua(userAgent).post(entity(body, type)))
	}
	
	override fun post(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	{
		var t = target.path(path)
		t = t.queryParam("access_token", token)
		if (userId != null)
			t = t.queryParam("user_id", "$userId")
		log("POST", t.uri)
		return CxfHttpResponse(t.request().ua(userAgent).post(entity(body, type)))
	}
	
	override fun put(path : String, body : ByteArray, type : String) : HttpResponse
	{
		val t = target.path(path)
		log("PUT", t.uri)
		return CxfHttpResponse(t.request().ua(userAgent).put(entity(body, type)))
	}
	
	override fun put(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	{
		var t = target.path(path)
		t = t.queryParam("access_token", token)
		if (userId != null)
			t = t.queryParam("user_id", "$userId")
		log("PUT", t.uri)
		return CxfHttpResponse(t.request().ua(userAgent).put(entity(body, type)))
	}
}

/**
 * A `HttpResponse` implementation using Apache CXF.
 */
class CxfHttpResponse(val res : Response) : HttpResponse()
{
	override val status = HttpStatusInfo(res.status, res.statusInfo.reasonPhrase)
	
	override fun header(name : String) : String
			= res.getHeaderString(name)
	
	override val bytes : ByteArray
		get() = IOUtils.toByteArray(res.readEntity(InputStream::class.java))
	
	override val str : String
		get() = res.readEntity(String::class.java)
	
	override val json : JsonObject
		get() = Parser().parse(StringBuilder(str)) as JsonObject
}
