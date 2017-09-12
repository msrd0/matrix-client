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

package msrd0.matrix.client.audio.test

import msrd0.matrix.client.audio.AacAudioFile
import net.sourceforge.jaad.spi.javasound.AACAudioFileReader
import org.apache.commons.io.IOUtils
import org.testng.annotations.*
import java.io.*
import java.lang.ClassLoader.*
import javax.sound.sampled.AudioInputStream

class AudioTest
{
	companion object
	{
		val m4aFile = File.createTempFile("matrix-client-audio-", ".m4a")
	}
	
	@BeforeTest
	fun copyFiles()
	{
		IOUtils.copy(getSystemResourceAsStream("sample.m4a"), FileOutputStream(m4aFile))
	}
	
	@Test
	fun javaxSoundSupport()
	{
	
	}
	
	@Test
	fun readAac()
	{
		val audioFile = AacAudioFile()
		audioFile.read(RandomAccessFile(m4aFile, "r"))
		println(audioFile.duration())
	}
}
