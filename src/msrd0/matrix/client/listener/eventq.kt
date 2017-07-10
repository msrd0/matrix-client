/*
 matrix-client
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client.listener

import org.slf4j.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class EventQueue
{
	companion object
	{
		val logger : Logger = LoggerFactory.getLogger(EventQueue::class.java)
		
		
	}
	
	/** Whether the EventQueue is running. */
	var isRunning = false
		private set
	
	/** All listeners sorted after their id. */
	private val listeners = HashMap<String, Deque<Listener<*>>>()
	
	/** Adds a listener. */
	fun addListener(listener : Listener<*>)
	{
		if (!listeners.containsKey(listener.name))
			listeners.put(listener.name, LinkedList<Listener<*>>())
		listeners[listener.name]!!.addFirst(listener)
	}
	
	
	/** The lock that is used to notify the queue of new events. */
	private val lock = Object()
	/** The queue that is used to store events. */
	private val q : Deque<Event> = LinkedList()
	/** Tells the queue whether it should be stopped. */
	private var shouldStop = false
	
	fun start()
	{
		if (isRunning)
			throw IllegalStateException("The queue is already running")
		
		thread(name = EventQueue::class.qualifiedName) {
			isRunning = true
			logger.debug("EventQueue started on Thread ${Thread.currentThread()}")
			
			while (!shouldStop)
			{
				if (q.isEmpty())
					synchronized(lock) {
						lock.wait()
					}
				val ev = q.pollFirst() ?: continue
			}
		}
	}
}