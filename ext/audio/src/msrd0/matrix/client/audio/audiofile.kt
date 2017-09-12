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

package msrd0.matrix.client.audio

import java.io.*
import javax.sound.sampled.AudioFormat

abstract class AudioFile
{
	var audioFormat : AudioFormat? = null
			protected set
	var frames : Long? = null
			protected set
	
	/**
	 * Return the duration in seconds.
	 */
	open fun duration() : Double
	{
		val format = audioFormat ?: throw IllegalStateException("audioFormat is null")
		val frames = frames?.toDouble() ?: throw IllegalStateException("frames is null")
		return (frames / format.frameRate)
	}
	
	/**
	 * Return the duration in milliseconds.
	 */
	open fun durationMillis() : Long
			= Math.ceil(duration()).toLong()
	
	@Throws(IOException::class)
	abstract fun read(file : RandomAccessFile)
	
	@Throws(IOException::class)
	abstract fun write(outputStream : OutputStream)
}

class AudioIOException(msg : String) : IOException(msg)
