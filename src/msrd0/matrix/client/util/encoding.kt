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
@file:JvmName("EncodingUtil")
package msrd0.matrix.client.util

import org.apache.commons.codec.binary.Base64
import kotlin.text.Charsets.UTF_8

fun String.toUtf8() : ByteArray = toByteArray(UTF_8)
fun ByteArray.fromUtf8() : String = String(this, UTF_8)

private val base64 by lazy { Base64(0, ByteArray(0), false) }
fun ByteArray.toBase64() : String = base64.encodeToString(this)
fun ByteArray.toUnpaddedBase64() : String = toBase64().trimEnd('=')
