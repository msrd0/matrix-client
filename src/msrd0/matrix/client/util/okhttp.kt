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

package msrd0.matrix.client.util

import com.beust.klaxon.*
import msrd0.matrix.client.MatrixId
import okhttp3.*
import java.io.*
import java.net.*
import java.util.concurrent.TimeUnit.*

class OkHttpTarget(uri : URI, userAgent : String) : HttpTarget(uri, userAgent)
{
	companion object
	{
		val client = OkHttpClient.Builder()
					.connectTimeout(2, MINUTES)
					.writeTimeout(2, MINUTES)
					.readTimeout(2, MINUTES)
					.build()
	}
	
	private fun encode(str : String) : String
			= URLEncoder.encode(str, "UTF-8")
	
	private var uriString = "$uri"
	init { if (!uriString.endsWith("/")) uriString += "/" }
	
	private fun toUrl(path : String, args : Map<String, String>) : String
	{
		var url = "$uriString${encode(path).replace("%2F", "/")}"
		var i = 0
		for ((key, value) in args)
		{
			if (i == 0)
				url += "?"
			else
				url += "&"
			url += encode(key) + "=" + encode(value)
			i++
		}
		return url
	}
	
	private fun req(path : String, args : Map<String, String>) : Request.Builder
		= Request.Builder()
			.url(toUrl(path, args))
			.header("User-Agent", userAgent)
	
	private fun body(body : ByteArray, type : String) : RequestBody
		= RequestBody.create(MediaType.parse(type), body)
	
	private fun log(call : Call)
			= log(call.request().method(), call.request().url().uri())
	
	override fun get(path : String, args : Map<String, String>) : HttpResponse
	{
		val call = client.newCall(req(path, args).get().build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun post(path : String, body : ByteArray, type : String) : HttpResponse
	{
		val call = client.newCall(req(path, emptyMap()).post(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun post(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	{
		val args = HashMap<String, String>()
		args["access_token"] = token
		if (userId != null)
			args["user_id"] = "$userId"
		val call = client.newCall(req(path, args).post(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun put(path : String, body : ByteArray, type : String) : HttpResponse
	{
		val call = client.newCall(req(path, emptyMap()).put(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun put(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	{
		val args = HashMap<String, String>()
		args["access_token"] = token
		if (userId != null)
			args["user_id"] = "$userId"
		val call = client.newCall(req(path, args).put(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun delete(path : String, body : ByteArray, type : String) : HttpResponse
	{
		val call = client.newCall(req(path, emptyMap()).delete(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
	
	override fun delete(path : String, token : String, userId : MatrixId?, body : ByteArray, type : String) : HttpResponse
	{
		val args = HashMap<String, String>()
		args["access_token"] = token
		if (userId != null)
			args["user_id"] = "$userId"
		val call = client.newCall(req(path, args).delete(body(body, type)).build())
		log(call)
		return OkHttpResponse(call.execute())
	}
}

class OkHttpResponse(val res : Response) : HttpResponse()
{
	override val status : HttpStatusInfo
			by lazy { HttpStatusInfo(res.code(), res.message()) }
	
	override fun header(name : String) : String?
			= res.header(name)
	
	override val stream : InputStream
		get() = res.body()!!.byteStream()
	override val bytes : ByteArray
			by lazy { res.body()!!.bytes() }
	override val str : String
			by lazy { String(bytes) }
	override val json : JsonObject
			by lazy { Parser().parse(stream) as JsonObject }
}
