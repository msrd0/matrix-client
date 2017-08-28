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

import com.beust.klaxon.string
import msrd0.matrix.client.MatrixClient.Companion.checkForError
import java.util.regex.Pattern

/**
 * This class represents a matrix content url of format `mxc://example.tld/FHyPlCeYUSFFxlgbQYZmoEoe`.
 */
class MatrixContentUrl(
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
	fun upload(bytes : ByteArray, mimetype : String, client : MatrixClient) : String
	{
		val res = client.target.post("_matrix/media/r0/upload", client.token ?: throw NoTokenException(), client.id,
				bytes, mimetype)
		checkForError(res)
		return res.json.string("content_uri") ?: missing("content_uri")
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun download(url : MatrixContentUrl, client : MatrixClient) : Pair<ByteArray, String>
	{
		val res = client.target.get("_matrix/media/r0/download/${url.domain}/${url.mediaId}", client.token ?: throw NoTokenException(), client.id)
		val status = res.status
		if (status.family != 2)
			throw MatrixErrorResponseException("${status.status}", status.phrase)
		return Pair<ByteArray, String>(res.bytes, res.header("Content-Type") ?: throw MatrixAnswerException("Missing Content-Type header"))
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun download(url : String, client : MatrixClient)
			= download(MatrixContentUrl.fromString(url), client)
}

// extend the client with the content repo functions
@Throws(MatrixAnswerException::class) fun MatrixClient.upload(bytes : ByteArray, mimetype : String) = ContentRepo.upload(bytes, mimetype, this)
@Throws(MatrixAnswerException::class) fun MatrixClient.download(url : MatrixContentUrl) = ContentRepo.download(url, this)
@Throws(MatrixAnswerException::class) fun MatrixClient.download(url : String) = ContentRepo.download(url, this)
