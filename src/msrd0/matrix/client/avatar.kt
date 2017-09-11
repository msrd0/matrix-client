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
import msrd0.matrix.client.event.MatrixEventContent
import msrd0.matrix.client.util.*
import java.awt.Image
import java.awt.image.*
import java.io.*
import javax.imageio.ImageIO

data class AvatarInfo(
		val width : Int,
		val height : Int,
		val size : Long,
		val mimetype : String
) : JsonSerializable
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : AvatarInfo
				= AvatarInfo(
					json.int("w") ?: missing("w"),
					json.int("h") ?: missing("h"),
					json.long("size") ?: missing("size"),
					json.string("mimetype") ?: missing("mimetype")
				)
	}
	
	// TODO add support for thumbnails
	
	override val json : JsonObject get()
			= JsonObject(mapOf(
				"w" to width,
				"h" to height,
				"size" to size,
				"mimetype" to mimetype
			))
}	

class Avatar @JvmOverloads constructor(
		val url : MatrixContentUrl,
		var info : AvatarInfo? = null
) : MatrixEventContent()
{
	@JvmOverloads
	constructor(url : String, info : AvatarInfo? = null)
			: this(MatrixContentUrl.fromString(url), info)
	
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : Avatar
		{
			val url = json.string("url") ?: missing("url")
			var info : AvatarInfo? = null
			if (json.containsKey("obj"))
				info = AvatarInfo.fromJson(json.obj("info")!!)
			return Avatar(url, info)
		}
		
		/**
		 * Creates an avatar from the given image. The client is used to upload the image to the matrix homeserver.
		 *
		 * @param image The image of this avatar.
		 * @param client The client used to upload the image.
		 * @param imageType The image type used for writing the image. One of: BMP, GIF, JPG/JPEG, PNG, WBMP. Please
		 * 	make sure the java installation also provides support for it, e.g. by calling `ImageIO.getWriterFormatNames()`.
		 * 	Default: JPG
		 *
		 * @throws MatrixAnswerException On errors while uploading the image.
		 */
		@JvmOverloads
		@JvmStatic
		@Throws(MatrixAnswerException::class)
		fun fromImage(image : RenderedImage, client : MatrixClient, imageType : String = "JPG") : Avatar
		{
			@Suppress("NAME_SHADOWING") var image = image
			val mimetype = when (imageType.toUpperCase()) {
				"BMP" -> "image/x-windows-bmp"
				"GIF" -> "image/gif"
				"JPG", "JPEG" -> "image/jpeg"
				"PNG" -> "image/png"
				"WBMP" -> "image/vnd.wap.wbmp"
				else  -> throw IllegalArgumentException("Unsupported image type: $imageType")
			}
			val baos = ByteArrayOutputStream()
			// bug in OpenJDK - cannot write jpeg images with alpha channel
			if (mimetype == "image/jpeg" && image.colorModel.hasAlpha())
			{
				val bi = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
				val g = bi.createGraphics()
				g.drawImage(image.toImage(), 0, 0, bi.width, bi.height, null)
				g.dispose()
				image = bi
			}
			ImageIO.write(image, imageType, baos)
			val bytes = baos.toByteArray()
			val url = client.upload(bytes, mimetype)
			val info = AvatarInfo(image.width, image.height, bytes.size.toLong(), mimetype)
			return Avatar(url, info)
		}
	}
	
	@Throws(MatrixAnswerException::class)
	fun downloadImage(client : MatrixClient) : BufferedImage
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
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["url"] = "$url"
		if (info != null)
			json["info"] = info!!.json
		return json
	}
}
