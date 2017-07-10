package msrd0.matrix.client

open class NoTokenException() : IllegalStateException()

open class MatrixAnswerException : Exception
{
	constructor() : super()
	constructor(msg : String) : super(msg)
}

open class IllegalJsonException : MatrixAnswerException
{
	constructor() : super()
	constructor(msg : String) : super(msg)
}

open class MatrixErrorResponseException(val errcode : String, val error : String) : MatrixAnswerException("$errcode: $error")
