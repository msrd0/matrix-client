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

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.event.MessageTypes.*

/**
 * The content of an audio message. Please make sure to call [uploadAudio] before trying to send events of this type.
 */
open class AudioMessageContent(alt : String) : UrlMessageContent(alt, AUDIO, "audio/aac")
{
	/** The duration of the audio track in milliseconds. */
	var duration : Long? = null
			protected set
	
	override fun loadFromJson(json : JsonObject)
	{
		TODO("not implemented")
	}
	
	override val infoJson : JsonObject get()
	{
		val json = super.infoJson
		json["duration"] = duration
		return json
	}
}
