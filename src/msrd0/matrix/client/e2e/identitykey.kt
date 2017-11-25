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

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.util.JsonSerializable

class IdentityKey(
		val ed25519 : ByteArray,
		val curve25519 : ByteArray
)

class IdentityKeyPair(
		val pubEd25519 : ByteArray,
		val privEd25519 : ByteArray,
		val pubCurve25519 : ByteArray,
		val privCurve25519 : ByteArray
)
{
	fun toIdentityKey() = IdentityKey(pubEd25519, pubCurve25519)
}
