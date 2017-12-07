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

package msrd0.matrix.client.e2e.olm

import msrd0.matrix.client.e2e.MatrixE2EException
import org.matrix.olm.OlmException

class MatrixOlmException(val olmException : OlmException) : MatrixE2EException(olmException)
{
	val code get() = olmException.exceptionCode
	override val message get() = olmException.message
}

@Throws(MatrixOlmException::class)
inline fun <T> wrapOlmEx(callback : () -> T) : T
{
	try
	{
		return callback()
	}
	catch (ex : OlmException)
	{
		throw MatrixOlmException(ex)
	}
}


class OlmEncryptionException : MatrixE2EException("Encryption failed")

class OlmDecryptionException : MatrixE2EException("Decryption failed")


/**
 * This exception is used internally to indicate mismatches in the received room key and the received metadata.
 */
class RoomKeyMismatchException(message : String) : Exception(message)
