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

import msrd0.matrix.client.e2e.verifySignature
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmAccount.*
import org.matrix.olm.OlmManager
import org.testng.Assert.*
import org.testng.annotations.Test

class OlmTest
{
	companion object
	{
		val olm = OlmManager()
		
		lateinit var account : OlmAccount
		lateinit var idKeys : IdentityKeys
	}
	
	@Test(groups = ["base"])
	fun version()
	{
		assertNotNull(olm.olmLibVersion)
	}
	
	@Test(groups = ["base"])
	fun generateKeys()
	{
		account = OlmAccount()
		idKeys = account.identityKeys()
	}
	
	@Test(groups = ["base"], dependsOnMethods = ["generateKeys"])
	fun sign()
	{
		val message = idKeys.json.toJsonString(canonical = true)
		val signature = account.signMessage(message)
		assert(verifySignature(signature, idKeys.ed25519, message))
	}
}
