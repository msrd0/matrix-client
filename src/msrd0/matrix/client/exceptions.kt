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

/**
 * This exception indicates that no token has been specified while the api call requires one.
 */
open class NoTokenException() : IllegalStateException()

/**
 * This exception indicates that none of the available flows are known or applicable.
 */
open class UnsupportedFlowsException : Exception
{
	constructor() : super()
	constructor(msg : String) : super(msg)
	constructor(flows : Collection<Flow>) : super("$flows")
}

/**
 * This exception indicates that there was an error in the matrix answer. Please use one of the subclasses or create
 * one yourself if no one fits your needs.
 */
open class MatrixAnswerException : Exception
{
	constructor() : super()
	constructor(msg : String) : super(msg)
}

/**
 * This exception indicates that there is an error in the json. Usually, this means that a required value is missing.
 */
open class IllegalJsonException : MatrixAnswerException
{
	constructor() : super()
	constructor(msg : String) : super(msg)
}

/**
 * This exception indicates that the matrix server has returned with either an error code or an error response.
 */
open class MatrixErrorResponseException(
		val errcode : String,
		val error : String
) : MatrixAnswerException("$errcode: $error")
