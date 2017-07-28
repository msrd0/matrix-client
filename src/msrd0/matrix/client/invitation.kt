package msrd0.matrix.client

import msrd0.matrix.client.Client.Companion.checkForError

class RoomInvitation(val client : Client, val room : RoomId)
{
	@Throws(MatrixAnswerException::class)
	fun accept()
	{
		val res = client.target.post("_matrix/client/r0/rooms/$room/join", client.token ?: throw NoTokenException(), client.id)
		checkForError(res)
	}
	
	override fun toString() = "RoomInvitation($room)"
}
