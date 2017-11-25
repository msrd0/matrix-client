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

interface KeyStore
{
	/**
	 * Store the identity key pair. Make sure that the private keys are kept safe.
	 */
	fun storeIdentityKeyPair(keyPair : IdentityKeyPair)
	
	/**
	 * Return `true` if this key store has an identity key pair.
	 */
	val hasIdentityKeyPair : Boolean
	
	/**
	 * Return the stored identity key pair. If no key pair is stored, [hasIdentityKeyPair] must return
	 * `false` and this method must throw an [IllegalStateException].
	 */
	@Throws(IllegalStateException::class)
	fun retrieveIdentityKeyPair() : IdentityKeyPair
}


open class InMemoryKeyStore : KeyStore
{
	private var idKeyPair : IdentityKeyPair? = null
	
	override fun storeIdentityKeyPair(keyPair : IdentityKeyPair)
	{
		idKeyPair = keyPair
	}
	
	override val hasIdentityKeyPair get() = idKeyPair != null
	
	@Throws(IllegalStateException::class)
	override fun retrieveIdentityKeyPair() = idKeyPair ?: throw IllegalStateException()
}
