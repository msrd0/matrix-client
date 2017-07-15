package msrd0.matrix.client

class RoomInvitation(val client : Client, val room : RoomId)
{
	@Throws(MatrixAnswerException::class)
	fun accept()
	{
		val res = client.target.post("_matrix/client/r0/rooms/$room/join", client.token ?: throw NoTokenException())
		client.checkForError(res)
	}
	
	override fun toString() = "RoomInvitation($room)"
}
