/*
 matrix-client
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client

import com.beust.klaxon.string
import java.util.regex.Pattern
import javax.ws.rs.client.Entity.entity
import javax.ws.rs.core.*

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
	fun upload(bytes : ByteArray, mimetype : String, client : Client) : String
	{
		val res = client.target.post("_matrix/media/r0/upload", client.token ?: throw NoTokenException(),
				entity(bytes, MediaType.valueOf(mimetype)))
		client.checkForError(res)
		return res.json.string("content_uri") ?: throw IllegalJsonException("Missing: 'content_uri'")
	}
	
	/**
	 * Download the content with the specified url.
	 *
	 * @return A pair of the bytes and the mimetype.
	 */
	@JvmStatic
	@Throws(MatrixAnswerException::class)
	fun download(url : MatrixContentUrl, client : Client) : Pair<ByteArray, String>
	{
		val res = client.target.get("_matrix/media/r0/download/${url.domain}/${url.mediaId}", client.token ?: throw NoTokenException())
		val status = res.statusInfo
		if (status.family != Response.Status.Family.SUCCESSFUL)
			throw MatrixErrorResponseException("${status.statusCode}", status.reasonPhrase)
		return Pair<ByteArray, String>(res.bytes, res.getHeaderString("Content-Type"))
	}
}

// extend the client with the content repo functions
fun Client.upload(bytes : ByteArray, mimetype : String) = ContentRepo.upload(bytes, mimetype, this)
fun Client.download(url : MatrixContentUrl) = ContentRepo.download(url, this)
