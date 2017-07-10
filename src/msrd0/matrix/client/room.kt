package msrd0.matrix.client

import com.beust.klaxon.*
import org.slf4j.*

/**
 * This class represents a matrix room.
 */
open class Room(
		val client : Client,
		val id : RoomId
) {
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(Room::class.java)
	}
	
	/** The name of this room or it's id. */
	var name : String = id.id
		protected set
	val members = ArrayList<MatrixId>()
	
	init
	{
		try { retrieveName() }
		catch (ex : MatrixErrorResponseException)
		{
			if (ex.errcode == "M_NOT_FOUND")
				/* The room does not have a name */
			else
				logger.warn("Failed to retrieve room name", ex)
		}
		
		retrieveMembers()
	}
	
	/**
	 * Retrieves the room's name.
	 *
	 * @throws MatrixAnswerException On errors in the matrix answer
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveName()
	{
		val res = client.target.get("/_matrix/client/r0/rooms/$id/state/m.room.name", client.token ?: throw NoTokenException())
		client.checkForError(res)
		
		name = res.json.string("name") ?: throw IllegalJsonException("Missing: 'name'")
	}
	
	/**
	 * Retrieves the room's members.
	 */
	@Throws(MatrixAnswerException::class)
	protected fun retrieveMembers()
	{
		val res = client.target.get("/_matrix/client/r0/rooms/$id/members", client.token ?: throw NoTokenException())
		client.checkForError(res)
		
		members.clear()
		
		val chunk = res.json.array<JsonObject>("chunk") ?: throw IllegalJsonException("Missing: 'chunk'")
		for (member in chunk)
		{
			val content = member.obj("content") ?: throw IllegalJsonException("Missing: 'chunk.[].content'")
			val membership = content.string("membership") ?: throw IllegalJsonException("Missing: 'chunk.[].content.membership")
			if (membership != "join")
				continue
			
			val userid = member.string("state_key") ?: throw IllegalJsonException("Missing: 'chunk.[].state_key")
			members.add(MatrixId.fromString(userid))
		}
	}
}
