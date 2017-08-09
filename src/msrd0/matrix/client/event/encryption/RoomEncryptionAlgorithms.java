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

package msrd0.matrix.client.event.encryption;

import static lombok.AccessLevel.*;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class RoomEncryptionAlgorithms
{
	/**
	 * Version 1 of the OLM ratchet. This uses:
	 *
	 *  - Curve25519 for the initial key agreement.
	 *  - HKDF-SHA-256 for ratchet key derivation.
	 *  - Curve25519 for the DH ratchet.
	 *  - HMAC-SHA-256 for the hash ratchet.
	 *  - HKDF-SHA-256, AES-256 in CBC mode, and 8 byte truncated HMAC-SHA-256 for authenticated encryption.
	 */
	public static final String OLM_V1_RATCHET = "m.olm.v1.curve25519-aes-sha2", OLM_CURVE25519_AES_SHA2 = OLM_V1_RATCHET;
	
	
	/**
	 * Version 1 of the MegOLM ratchet. This uses:
	 *
	 *  - HMAC-SHA-256 for the hash ratchet.
	 *  - HKDF-SHA-256, AES-256 in CBC mode, and 8 byte truncated HMAC-SHA-256 for authenticated encryption.
	 *  - Ed25519 for message authenticity.
	 */
	public static final String MEGOLM_V1_RATCHET = "m.megolm.v1.aes-sha2", MEGOLM_AES_SHA2 = MEGOLM_V1_RATCHET;
}
