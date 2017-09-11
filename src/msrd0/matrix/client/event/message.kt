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

package msrd0.matrix.client.event

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.MatrixEventTypes.*
import msrd0.matrix.client.event.MessageTypes.*
import java.awt.image.RenderedImage
import java.io.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.*
import java.util.*
import java.util.regex.Pattern
import javax.imageio.ImageIO

/**
 * The content of a message. Every message has a body and a message type. While you can use this class directly, the
 * use of one of the subclasses is recommended.
 */
open class MessageContent(
		val body : String,
		val msgtype : String
) : MatrixEventContent()
{
	companion object
	{
		/**
		 * Constructs a message content by parsing the supplied json. For a documentation of the json see the matrix
		 * specifications.
		 *
		 * @throws MatrixAnswerException On errors in the json.
		 */
		@JvmStatic
		@Throws(MatrixAnswerException::class)
		fun fromJson(json : JsonObject) : MessageContent
		{
			val type = json.string("msgtype") ?: missing("msgtype")
			val body = json.string("body") ?: missing("body")
			
			if (type == TEXT)
			{
				if (json.containsKey("format"))
					return FormattedTextMessageContent(body,
							json.string("format") ?: missing("format"),
							json.string("formatted_body") ?: missing("formatted_body"))
				return TextMessageContent(body)
			}
			
			if (type == IMAGE)
			{
				val content = ImageMessageContent(body)
				content.loadFromJson(
						json.obj("info") ?: missing("info"),
						json.string("url") ?: missing("url")
				)
				return content
			}
			
			throw MatrixAnswerException("Unknown message type $type")
		}
	}
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["body"] = body
		json["msgtype"] = msgtype
		return json
	}
}

/**
 * The content of a text message.
 */
open class TextMessageContent(body : String) : MessageContent(body, TEXT)

/**
 * The content of a formattet text message.
 */
open class FormattedTextMessageContent(
		body : String,
		val format : String,
		val formattedBody : String
) : TextMessageContent(body)
{
	override val json : JsonObject get()
	{
		val json = super.json
		json["format"] = format
		json["formatted_body"] = formattedBody
		return json
	}
}

/**
 * The content of an image message. Please make sure to call `uploadImage` before trying to send events of this type.
 */
open class ImageMessageContent(alt : String) : MessageContent(alt, IMAGE)
{
	var url : MatrixContentUrl? = null
			protected set
	var mimetype : String = "image/png"
			protected set
	var width : Int? = null
			protected set
	var height : Int? = null
			protected set
	var size : Int? = null
			protected set
	
	/**
	 * Loads the image from the json of a received message event.
	 *
	 * @throws IllegalJsonException On errors in the json.
	 */
	@Throws(IllegalJsonException::class)
	open fun loadFromJson(info : JsonObject, url : MatrixContentUrl)
	{
		this.url = url
		this.mimetype = info.string("mimetype") ?: missing("mimetype")
		this.width = info.int("w") ?: missing("w")
		this.height = info.int("h") ?: missing("h")
		this.size = info.int("size") ?: missing("size")
	}
	@Throws(IllegalJsonException::class)
	fun loadFromJson(info : JsonObject, url : String)
			= loadFromJson(info, MatrixContentUrl.fromString(url))
	
	/**
	 * Uploads the image to the matrix server. This method needs to be called before sending this message.
	 *
	 * @param image The image of this avatar.
	 * @param client The client used to upload the image.
	 * @param imageType The image type used for writing the image. One of: BMP, GIF, JPG/JPEG, PNG, WBMP. Please
	 * 	make sure the java installation also provides support for it, e.g. by calling `ImageIO.getWriterFormatNames()`.
	 * 	Default: PNG
	 *
	 * @throws MatrixAnswerException On errors in the answer.
	 */
	@JvmOverloads
	@Throws(MatrixAnswerException::class)
	open fun uploadImage(image : RenderedImage, client : MatrixClient, imageType : String = "PNG")
	{
		val (url, info) = client.uploadImage(image, imageType)
		this.url = url
		this.width = info.width
		this.height = info.height
		this.mimetype = info.mimetype
		this.size = info.size
	}
	
	/**
	 * Downloads the image from the matrix server. Please make sure that either `uploadImage` or `loadFromJson` was
	 * called before to populate the url of this image message.
	 *
	 * @throws MatrixAnswerException On errors in the answer.
	 * @throws IOException On errors when reading the image.
	 */
	@Throws(MatrixAnswerException::class, IOException::class)
	open fun downloadImage(client : MatrixClient) : RenderedImage
	{
		val res = client.download(url ?: throw IllegalStateException("url is null"))
		return ImageIO.read(ByteArrayInputStream(res.first))
	}
	
	override val json : JsonObject get()
	{
		val json = super.json
		json["url"] = url?.toString() ?: throw IllegalStateException("You need to call ImageMessageContent::uploadImage first")
		json["info"] = mapOf(
				"mimetype" to mimetype,
				"h" to height,
				"w" to width,
				"size" to size
		)
		return json
	}
}

/**
 * This class represents a message in a room.
 */
class Message
@Throws(IllegalJsonException::class)
constructor(room : Room, json : JsonObject)
	: MatrixRoomEvent<MessageContent>(room, json,
		MessageContent.fromJson(json.obj("content") ?: missing("content")))
{
	val body get() = content.body
	val msgtype get() = content.msgtype
}

class Messages(
		val start : String,
		val end : String,
		messages : Collection<Message> = Collections.emptyList()
) : ArrayList<Message>(messages)
