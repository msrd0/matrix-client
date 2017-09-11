/*
 * matrix-client-audio
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

package msrd0.matrix.client.event

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.MessageTypes.*

/**
 * The content of an audio message. Please make sure to call [uploadAudio] before trying to send events of this type.
 */
open class AudioMessageContent(alt : String) : UrlMessageContent(alt, AUDIO, "audio/aac")
{
	/** The duration of the audio track in milliseconds. */
	var duration : Long? = null
			protected set
	
	@Throws(IllegalJsonException::class)
	open fun loadFromJson(info : JsonObject, url : MatrixContentUrl)
	{
		this.url = url
		this.mimetype = info.string("mimetype") ?: missing("mimetype")
		this.size = info.int("size") ?: missing("size")
		this.duration = info.long("duration") ?: missing("duration")
	}
	@Throws(IllegalJsonException::class)
	fun loadFromJson(info : JsonObject, url : String)
			= loadFromJson(info, MatrixContentUrl.fromString(url))
	
	@Throws(IllegalJsonException::class)
	override fun loadFromJson(json : JsonObject)
			= loadFromJson(json.obj("info") ?: missing("info"), json.string("url") ?: missing("url"))
	
	override val infoJson : JsonObject get()
	{
		val json = super.infoJson
		json["duration"] = duration
		return json
	}
}
