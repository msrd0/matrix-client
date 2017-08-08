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

open class RoomCache
{
	internal var cache : MutableMap<String, Any?> = emptyMutableMap()
}

class RoomCacheEventDelegate <in R : RoomCache, T>(
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

typealias RoomEventDelegate<T> = RoomCacheEventDelegate<Room, T>
