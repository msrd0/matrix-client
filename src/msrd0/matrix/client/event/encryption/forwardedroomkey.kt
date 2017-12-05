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

package msrd0.matrix.client.event.encryption

import com.beust.klaxon.*
import msrd0.matrix.client.*
import msrd0.matrix.client.event.MatrixEventContent
import msrd0.matrix.client.event.MatrixEventTypes.*

class ForwardedRoomKeyEventContent(
		/** The algorithm of the forwarded key. */
		val algorithm : String,
		/** The room id this key belongs to. */
		val roomId : RoomId,
		/** Curve25519 key of the sender. */
		val senderKey : String,
		/** Ed25519 key of the original sender of the original [ROOM_KEY] event. */
		val senderClaimedEd25519Key : String,
		/** The session id of the olm inbound session. */
		val sessionId : String,
		/** The session key as exported from the olm inbound session. */
		val sessionKey : String,
		/**
		 * A list of Curve25519 keys that forwarded this key. Every time the key gets forwarded, the sender's curve25519
		 * key gets appended to the chain. This does not include the original sender of the [ROOM_KEY] event.
		 */
		val forwardingCurve25519KeyChain : List<String> = emptyList(),
		/** The chain index in the inbound session. Currently ignored by everyone as it is also part of the [sessionKey]. */
		val chainIndex : Long? = null
) : MatrixEventContent()
{
	@Throws(IllegalJsonException::class)
	constructor(json : JsonObject) : this(
			json.string("algorithm") ?: missing("algorithm"),
			json.string("room_id")?.let { RoomId.fromString(it) } ?: missing("room_id"),
			json.string("sender_key") ?: missing("sender_key"),
			json.string("sender_claimed_ed25519_key") ?: missing("sender_claimed_ed25519_key"),
			json.string("session_id") ?: missing("session_id"),
			json.string("session_key") ?: missing("session_key"),
			json.array<String>("forwarding_curve25519_key_chain") ?: emptyList(),
			json.long("chain_index")
	)
	
	override val json : JsonObject get()
	{
		val json = JsonObject()
		json["algorithm"] = algorithm
		json["room_id"] = "$roomId"
		json["sender_key"] = senderKey
		json["sender_claimed_ed25519_key"] = senderClaimedEd25519Key
		json["session_id"] = sessionId
		json["session_key"] = sessionKey
		json["forwarding_curve25519_key_chain"] = forwardingCurve25519KeyChain
		if (chainIndex != null)
			json["chain_index"] = chainIndex
		return json
	}
}
