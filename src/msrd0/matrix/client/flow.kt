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

import com.beust.klaxon.*
import msrd0.matrix.client.util.JsonSerializable

interface FlowHelper
{
	@Throws(UnsupportedFlowsException::class)
	fun answer(request : FlowRequest) : FlowResponse
}

/**
 * A default flow helper that can handle dummy and password login.
 */
class DefaultFlowHelper(val password : String? = null) : FlowHelper
{
	@Throws(UnsupportedFlowsException::class)
	override fun answer(request : FlowRequest) : FlowResponse
	{
		for (flow in request.flows)
		{
			if (flow.stages.size != 1)
				continue
			val type = flow.stages[0]
			if (type == LoginType.DUMMY.type)
				return FlowResponse(type, request.session)
			if (type == LoginType.PASSWORD.type)
				return FlowResponse(type, request.session).addProperty("password", password ?: continue)
		}
		throw UnsupportedFlowsException(request.flows)
	}
}

/**
 * This class represents a flow, consisting of stages.
 */
data class Flow(val stages : List<String>)
{
	companion object
	{
		/**
		 * Parse a flow from json.
		 *
		 * @throws IllegalJsonException On errors in the supplied json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : Flow
			= Flow(json.array<String>("stages") ?: throw IllegalJsonException("Missing: 'stages'"))
	}
}

/**
 * This class represents a flow request. It consists of a session assigned by the server, multiple flows to choose
 * from, and a list of completed stages which can be empty.
 */
open class FlowRequest(
		val session : String,
		val flows : List<Flow>,
		val completed : List<String>
)
{
	companion object
	{
		/**
		 * Parse a flow request from json.
		 *
		 * @throws IllegalJsonException On errors in the supplied json.
		 */
		@JvmStatic
		@Throws(IllegalJsonException::class)
		fun fromJson(json : JsonObject) : FlowRequest
		{
			val session = json.string("session") ?: throw IllegalJsonException("Missing: 'session'")
			val flows = json.array<JsonObject>("flows")?.map { Flow.fromJson(it) } ?: throw IllegalJsonException("Missing: 'flows'")
			val completed : List<String> = json.array("completed") ?: emptyList()
			return FlowRequest(session, flows, completed)
		}
	}
}

/**
 * This class represents a one-stage response. It stores the type of the flow at the current stage you chose
 * as well as the session assigned by the matrix server. You can (and should) add more properties, depending on
 * the type, using the `addProperty` method.
 */
open class FlowResponse(
		val type : String,
		val session : String
) : JsonSerializable
{
	private val dict = HashMap<String, String>()
	
	init
	{
		dict["type"] = type
		dict["session"] = session
	}
	
	/**
	 * Add a property to the response. Note that `type` and `session` are added automatically.
	 */
	fun addProperty(key : String, value : String) : FlowResponse
	{
		dict[key] = value
		return this
	}
	
	override val json : JsonObject get()
			= JsonObject(dict)
}
