/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
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

package msrd0.matrix.client.e2e

import msrd0.matrix.client.MatrixAnswerException
import msrd0.matrix.client.MatrixClient

/**
 * This exception indicates that someone is trying to used end-to-end encryption on a client without calling
 * [MatrixClient.enableE2E] first.
 */
open class E2ENotEnabledException : IllegalStateException()

/**
 * This exception indicates that there was an error with end-to-end encryption. Please use one of the subclasses or
 * create one yourself if no one fits your needs.
 */
abstract class MatrixE2EException : Exception
{
	constructor() : super()
	constructor(msg : String) : super(msg)
	constructor(t : Throwable) : super(t)
}

/**
 * This exception indicates that the session for the given session id is missing.
 */
open class NoSuchSessionException(
		val sessionId : String
) : MatrixE2EException("Unknown session for id '$sessionId'")
