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

import net.sourceforge.jaad.mp4.MP4Container
import net.sourceforge.jaad.mp4.api.AudioTrack
import net.sourceforge.jaad.spi.javasound.AACAudioFileReader
import java.io.*

class AacAudioFile : AudioFile()
{
	companion object
	{
		val reader = AACAudioFileReader()
	}
	
	@Throws(IOException::class)
	override fun read(file : RandomAccessFile)
	{
		val container = MP4Container(file)
		val movie = container.movie
		val content = movie.tracks
		if (content.isEmpty())
			throw AudioIOException("No tracks found")
		val track = content[0] as AudioTrack
		
	}
	
	override fun write(output : OutputStream)
	{
		TODO("not implemented")
	}
	
}
