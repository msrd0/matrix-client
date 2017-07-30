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

import com.beust.klaxon.JsonObject

enum class LoginType(val type : String)
{
	PASSWORD("m.login.password"),
	RECAPTCHA("m.login.recaptcha"),
	OAUTH2("m.login.oauth2"),
	EMAIL("m.login.email.identity"),
	TOKEN("m.login.token"),
	DUMMY("m.login.dummy"),
	SUCCESS("");
	
	companion object
	{
		fun fromString(str : String) : LoginType
		{
			for (value in values())
				if (value.type == str)
					return value
			throw IllegalArgumentException("Unknown login type: $str")
		}
	}
}

class Auth(val client : Client, val loginType : LoginType)
{
	val isSuccess : Boolean
		get() = loginType == LoginType.SUCCESS
	
	private val dict = JsonObject()
	
	init
	{
		dict["type"] = loginType.type
		dict["user"] = client.id.localpart
	}
	
	fun setProperty(key : String, value : String)
	{
		dict[key] = value
	}
	
	fun submit() : Collection<Auth> = client.auth(dict)
	
	override fun equals(other: Any?): Boolean = (other is Auth && loginType == other.loginType)
	override fun hashCode(): Int = loginType.hashCode()
}
