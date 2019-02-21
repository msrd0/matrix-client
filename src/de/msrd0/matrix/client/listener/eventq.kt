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

package de.msrd0.matrix.client.listener

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.*

class EventQueue
{
	companion object
	{
		private val logger : Logger = LoggerFactory.getLogger(EventQueue::class.java)
		
		private val timeout : Long = 200 /* ms */
	}
	
	/** Whether the EventQueue is running. */
	var isRunning = false
		private set
	
	/** All listeners sorted after their id. */
	private val listeners = HashMap<EventType, LinkedList<Listener<*>>>()
	
	/** Adds a listener. */
	fun addListener(type : EventType, listener : Listener<*>)
	{
		if (!listeners.containsKey(type))
			listeners.put(type, LinkedList())
		listeners[type]!!.addFirst(listener)
	}
	
	
	/** The queue that is used to store events. */
	private val q : BlockingDeque<Event> = LinkedBlockingDeque()
	/** Tells the queue whether it should be stopped. */
	private var shouldStop = false
	
	@Throws(NoSuchMethodException::class)
	private fun invoke(l : Listener<*>, ev : Event) : Boolean
	{
		for (method in l.javaClass.methods)
		{
			if (method.name != "call")
				continue
			if (method.parameterCount != 1)
				continue
			if (method.returnType != java.lang.Boolean.TYPE)
				continue
			try
			{
				method.isAccessible = true
				return method.invoke(l, ev) as Boolean
			}
			catch (ex : ReflectiveOperationException)
			{
				logger.warn("Unable to invoke '$method' on listener", ex)
			}
		}
		throw NoSuchMethodException("Unable to find suitable 'call' method in Listener")
	}
	
	/**
	 * Start the EventQueue in a coroutine.
	 */
	fun start()
	{
		if (isRunning)
			throw IllegalStateException("The queue is already running")
		
		GlobalScope.launch {
			isRunning = true
			logger.debug("EventQueue started on Thread ${Thread.currentThread()}")
			
			while (!shouldStop)
			{
				val ev = q.pollFirst(timeout, MILLISECONDS) ?: continue
				val l = listeners.get(ev.type) ?: continue
				for (listener : Listener<*> in l)
				{
					try
					{
						if (invoke(listener, ev))
							break
					}
					catch (ex : NoSuchMethodException)
					{
						logger.warn("Unable to invoke listener", ex)
					}
				}
			}
		}
	}
	
	/**
	 * Enqueues the Event.
	 */
	fun enqueue(ev : Event)
	{
		q.putFirst(ev)
	}
	
	/**
	 * Tells the EventQueue to stop.
	 */
	fun stop()
	{
		shouldStop = true
	}
}