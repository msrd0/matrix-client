package msrd0.matrix.client

import java.net.URI

data class HomeServer(
		val domain : String,
		val base : URI
)

data class MatrixID(
		val localpart : String,
		val domain : String
)
{
	val id : String
		get() = "@$localpart:$domain"
	
	val isAcceptable : Boolean
		get() = id.length < 255
}

data class Context(
		val hs : HomeServer,
		val id : MatrixID
)
