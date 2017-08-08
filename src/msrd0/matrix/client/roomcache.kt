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

package msrd0.matrix.client

import msrd0.matrix.client.util.emptyMutableMap
import kotlin.reflect.KProperty

/**
 * Superclass of `Room` with a cache property. Used by the delegates for cached properties.
 */
open class RoomCache
{
	internal val cache : MutableMap<String, Any?> = emptyMutableMap()
}

/**
 * Delegate for room properties from a state event with only one state key.
 */
class RoomEventDelegate <in R : RoomCache, T>(
		val retrieve : R.() -> T,
		val update : R.(T) -> Unit
)
{
	operator fun getValue(self : R, property : KProperty<*>) : T
	{
		val name = property.name
		
		if (!self.cache.containsKey(name))
			self.cache[name] = retrieve(self)
		
		@Suppress("UNCHECKED_CAST")
		return self.cache[name] as T
	}
	
	operator fun setValue(self : R, property : KProperty<*>, value : T)
	{
		val name = property.name
		
		update(self, value)
		self.cache[name] = value
	}
}

/**
 * A map-like class with a get and set operator to use with `RoomEventStateKeyDelegate`.
 */
class RoomEventStateKeyMap<V>(
		val retrieve : (String) -> V,
		val update : (String, V) -> Unit
)
{
	private val cache : MutableMap<String, V> = emptyMutableMap()
	
	operator fun get(key : String) : V
	{
		if (!cache.containsKey(key))
			cache[key] = retrieve(key)
		
		return cache[key]!!
	}
	
	operator fun set(key : String, value : V)
	{
		update(key, value)
		cache[key] = value
	}
}

/**
 * Delegate for room properties from a state event with multiple state keys.
 */
class RoomEventStateKeyDelegate <V>(
		val retrieve : Room.(String) -> V,
		val update : Room.(String, V) -> Unit
)
{
	operator fun getValue(self : Room, property : KProperty<*>) : RoomEventStateKeyMap<V>
	{
		val name = property.name
		
		if (!self.cache.containsKey(name))
			self.cache[name] = RoomEventStateKeyMap<V>(
					{ key -> retrieve(self, key) },
					{ key, value -> update(self, key, value) }
			)
		
		@Suppress("UNCHECKED_CAST")
		return self.cache[name] as RoomEventStateKeyMap<V>
	}
}
