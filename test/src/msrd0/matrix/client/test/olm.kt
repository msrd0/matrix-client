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

package msrd0.matrix.client.test

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.e2e.olm.InMemoryKeyStore
import msrd0.matrix.client.e2e.olm.OlmE2E
import org.testng.annotations.Test

class OlmTest
{
	companion object
	{
		val olm = OlmE2E(InMemoryKeyStore())
		lateinit var idKeys : JsonObject
	}
	
	@Test(groups = ["base"])
	fun generateKeys()
	{
		olm.initialise()
		idKeys = olm.identityKeys
	}
	
	@Test(groups = ["base"], dependsOnMethods = ["generateKeys"])
	fun sign()
	{
		val message = idKeys.toJsonString(canonical = true)
		val signature = olm.sign(message)
		assert(olm.verifySignature(signature, olm.fingerprintKey, message))
	}
}
