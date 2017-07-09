package msrd0.matrix.client

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

open class MatrixErrorResponseException : Exception
{
	constructor(error : String) : super(error)
}
