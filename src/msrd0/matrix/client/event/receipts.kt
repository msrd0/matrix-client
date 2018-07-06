/*
 * matrix-client
 * Copyright (C) 2017-2018 Dominic Meiser
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

package msrd0.matrix.client.event

import com.beust.klaxon.JsonObject
import msrd0.matrix.client.*

/**
 * The content of a receipt event. It is a dictionary of eventId to receiptType to userId to timestamp.
 */
class ReceiptContent(val content : Map<String, Map<String, Map<MatrixId, Long>>>)
{
	companion object
	{
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject)
				= ReceiptContent(json
						.mapKeys { it.key }
						.mapValues { (it.value as JsonObject)
								.mapKeys { it.key }
								.mapValues { (it.value as JsonObject)
										.mapKeys { MatrixId.fromString(it.key) }
										.mapValues {
											val value = it.value
											when (value) {
												is Int -> value.toLong()
												is Long -> value
												else -> throw IllegalArgumentException("")
											}
										}
								}
						}
				)
	}
}
