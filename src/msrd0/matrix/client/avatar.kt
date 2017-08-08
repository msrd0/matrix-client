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
import java.awt.image.*
import java.io.*
import javax.imageio.ImageIO

data class AvatarInfo(
		val width : Int,
		val height : Int,
		val size : Long,
		val mimetype : String
)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : AvatarInfo
				= AvatarInfo(
					json.int("width") ?: throw IllegalJsonException("Missing: 'width'"),
					json.int("height") ?: throw IllegalJsonException("Missing: 'height'"),
					json.long("size") ?: throw IllegalJsonException("Missing: 'size'"),
					json.string("mimetype") ?: throw IllegalJsonException("Missing: 'mimetype'")
				)
	}
	
	// TODO add support for thumbnails
}

class Avatar @JvmOverloads constructor(
		val url : String,
		var info : AvatarInfo? = null
)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : Avatar
		{
			val url = json.string("url") ?: throw IllegalJsonException("Missing: 'url'")
			var info : AvatarInfo? = null
			if (json.containsKey("obj"))
				info = AvatarInfo.fromJson(json.obj("info")!!)
			return Avatar(url, info)
		}
		
		@JvmStatic
		@Throws(MatrixAnswerException::class)
		fun fromImage(image : RenderedImage, client : Client) : Avatar
		{
			val baos = ByteArrayOutputStream()
			ImageIO.write(image, "PNG", baos)
			val bytes = baos.toByteArray()
			val url = client.upload(bytes, "image/png")
			val info = AvatarInfo(image.width, image.height, bytes.size.toLong(), "image/png")
			return Avatar(url, info)
		}
	}
	
	@Throws(MatrixAnswerException::class)
	fun downloadImage(client : Client) : BufferedImage
	{
		val res = client.download(url)
		if (info != null)
		{
			if (res.first.size.toLong() != info!!.size)
				throw MatrixInfoMismatchException("size", info!!.size, res.first.size)
			if (res.second != info!!.mimetype)
				throw MatrixInfoMismatchException("mimetype", info!!.mimetype, res.second)
		}
		val bais = ByteArrayInputStream(res.first)
		val img = ImageIO.read(bais)
		if (info != null)
		{
			if (img.width != info!!.width)
				throw MatrixInfoMismatchException("width", info!!.width, img.width)
			if (img.height != info!!.height)
				throw MatrixInfoMismatchException("height", info!!.height, img.height)
		}
		return img
	}
}
