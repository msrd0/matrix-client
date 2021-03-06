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

package de.msrd0.matrix.client

import com.beust.klaxon.JsonObject
import de.msrd0.matrix.client.event.MatrixEventContent
import de.msrd0.matrix.client.util.JsonSerializable
import java.awt.image.*
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

data class AvatarInfo(
		val width : Int,
		val height : Int,
		val size : Int,
		val mimetype : String
) : JsonSerializable
{
	constructor(info : ImageInfo) : this(info.width, info.height, info.size, info.mimetype)
	
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : AvatarInfo
				= AvatarInfo(
					json.int("w") ?: missing("w"),
					json.int("h") ?: missing("h"),
					json.int("size") ?: missing("size"),
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
			val (url, info) = client.uploadImage(image, imageType)
			return Avatar(url, AvatarInfo(info))
		}
	}
	
	@Throws(MatrixAnswerException::class)
	fun downloadImage(client : MatrixClient) : BufferedImage
	{
		val res = client.downloadBytes(url)
		if (info != null)
		{
			if (res.first.size != info!!.size)
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
