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
@file:JvmName("Collections")
package msrd0.matrix.client.util

/**
 * Convert a map to a mutable map.
 */
fun <K, V> mutableMap(map : Map<K, V>) : MutableMap<K, V> = when (map) {
	is MutableMap<K, V> -> map
	else                -> HashMap(map)
}

/**
 * Convert this map to a mutable map.
 */
fun <K, V> Map<K, V>.toMutableMap() : MutableMap<K, V> = mutableMap(this)

/**
 * An empty mutable map.
 */
fun <K, V> emptyMutableMap() : MutableMap<K, V> = HashMap()
