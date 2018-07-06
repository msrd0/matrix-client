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

package msrd0.matrix.client

import msrd0.matrix.client.MatrixClient.Companion.checkForError
import msrd0.matrix.client.util.toImage
import java.awt.image.*
import java.io.*
import java.util.regex.Pattern
import javax.imageio.ImageIO

/**
 * This class represents a matrix content url of format `mxc://example.tld/FHyPlCeYUSFFxlgbQYZmoEoe`.
 */
data class MatrixContentUrl(
		val domain : String,
		val mediaId : String
)
{
	companion object
	{
		private val pattern : Pattern = Pattern.compile("mxc://(?<domain>[^/]+)/(?<mediaId>[^/]+)")
		
		@JvmStatic
		fun fromString(str : String) : MatrixContentUrl
		{
			val matcher = pattern.matcher(str)
			if (!matcher.matches())
				throw IllegalArgumentException("The supplied url doesn't match the required pattern")
			return MatrixContentUrl(
					matcher.group("domain"),
					matcher.group("mediaId")
			)
		}
	}
	
	override fun toString() = "mxc://$domain/$mediaId"
}

data class ImageInfo(
		val width : Int,
		val height : Int,
		val mimetype : String,
		val size : Int
)
{
	constructor(image : RenderedImage, mimetype : String, size : Int)
			: this(image.width, image.height, mimetype, size)
}

/**
 * This object helps accessing the matrix content repository.
 */
object ContentRepo
{
	/**
	 * Upload the bytes with the specified mimetype to the server.
	 *
	 * @return The content url.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun upload(bytes : ByteArray, mimetype : String, client : MatrixClient) : MatrixContentUrl
	{
		val res = client.target.post("_matrix/media/r0/upload", client.token ?: throw NoTokenException(), client.id,
				bytes, mimetype)
		checkForError(res)
		return MatrixContentUrl.fromString(res.json.string("content_uri") ?: missing("content_uri"))
	}
	
	/**
	 * Upload the image with the specified image type to the server.
	 *
	 * @param image The image to upload.
	 * @param client The client used to upload the image.
	 * @param imageType The image type used for writing the image. One of: BMP, GIF, JPG/JPEG, PNG, WBMP. Please
	 * 	make sure the java installation also provides support for it, e.g. by calling `ImageIO.getWriterFormatNames()`.
	 * 	Default: PNG
	 *
	 * @return The content url plus information about the uploaded image.
	 */
	@JvmOverloads
	@JvmStatic
	@Throws(MatrixAnswerException::class, IOException::class)
	fun uploadImage(image : RenderedImage, client : MatrixClient, imageType : String = "PNG") : Pair<MatrixContentUrl, ImageInfo>
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
		// bug in OpenJDK - cannot write jpeg images with alpha channel
		if (mimetype == "image/jpeg" && image.colorModel.hasAlpha())
		{
			val bi = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
			val g = bi.createGraphics()
			g.drawImage(image.toImage(), 0, 0, bi.width, bi.height, null)
			g.dispose()
			image = bi
		}
		val baos = ByteArrayOutputStream()
		ImageIO.write(image, imageType, baos)
		val bytes = baos.toByteArray()
		val url = client.upload(bytes, mimetype)
		return url to ImageInfo(image, mimetype, bytes.size)
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun downloadBytes(url : MatrixContentUrl, client : MatrixClient) : Pair<ByteArray, String>
	{
		val res = client.target.get("_matrix/media/r0/download/${url.domain}/${url.mediaId}", client.token ?: throw NoTokenException(), client.id)
		val status = res.status
		if (status.family != 2)
			throw MatrixErrorResponseException("${status.status}", status.phrase)
		return Pair(res.bytes, res.header("Content-Type") ?: throw MatrixAnswerException("Missing Content-Type header"))
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun downloadBytes(url : String, client : MatrixClient)
			= downloadBytes(MatrixContentUrl.fromString(url), client)
	
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun downloadStream(url : MatrixContentUrl, client : MatrixClient) : Pair<InputStream, String>
	{
		val res = client.target.get("_matrix/media/r0/download/${url.domain}/${url.mediaId}", client.token ?: throw NoTokenException(), client.id)
		val status = res.status
		if (status.family != 2)
			throw MatrixErrorResponseException("${status.status}", status.phrase)
		return Pair(res.stream, res.header("Content-Type") ?: throw MatrixAnswerException("Missing Content-Type header"))
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun downloadStream(url : String, client : MatrixClient)
			= downloadStream(MatrixContentUrl.fromString(url), client)
	
	
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	@Deprecated("Deprecated API call", replaceWith = ReplaceWith("downloadBytes(url, client)"))
	fun download(url : MatrixContentUrl, client : MatrixClient) = downloadBytes(url, client)
	
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	@Deprecated("Deprecated API call", replaceWith = ReplaceWith("downloadBytes(url, client)"))
	fun download(url : String, client : MatrixClient) = downloadBytes(url, client)
}

// extend the client with the content repo functions

@Throws(MatrixAnswerException::class)
fun MatrixClient.upload(bytes : ByteArray, mimetype : String)
		= ContentRepo.upload(bytes, mimetype, this)

@Throws(MatrixAnswerException::class, IOException::class)
fun MatrixClient.uploadImage(image : RenderedImage, imageType : String = "PNG")
		= ContentRepo.uploadImage(image, this, imageType)

@Throws(MatrixAnswerException::class)
@Deprecated("Deprecated API call", replaceWith = ReplaceWith("downloadBytes(url)"))
fun MatrixClient.download(url : MatrixContentUrl)
		= @Suppress("DEPRECATION") ContentRepo.download(url, this)

@Throws(MatrixAnswerException::class)
@Deprecated("Deprecated API call", replaceWith = ReplaceWith("downloadBytes(url)"))
fun MatrixClient.download(url : String)
		= @Suppress("DEPRECATION") ContentRepo.download(url, this)

@Throws(MatrixAnswerException::class)
fun MatrixClient.downloadBytes(url : MatrixContentUrl)
		= ContentRepo.downloadBytes(url, this)

@Throws(MatrixAnswerException::class)
fun MatrixClient.downloadBytes(url : String)
		= ContentRepo.downloadBytes(url, this)

@Throws(MatrixAnswerException::class)
fun MatrixClient.downloadStream(url : MatrixContentUrl)
		= ContentRepo.downloadStream(url, this)

@Throws(MatrixAnswerException::class)
fun MatrixClient.downloadStream(url : String)
		= ContentRepo.downloadStream(url, this)
