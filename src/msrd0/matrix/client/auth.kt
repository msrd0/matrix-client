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
		dict["user"] = client.context.id.localpart
	}
	
	fun setProperty(key : String, value : String)
	{
		dict[key] = value
	}
	
	fun submit() : Collection<Auth> = client.auth(dict)
	
	override fun equals(other: Any?): Boolean = (other is Auth && loginType == other.loginType)
	override fun hashCode(): Int = loginType.hashCode()
}
