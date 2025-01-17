/**
 *                   Event scheduler (based on alarm from Vice)
 *                   ------------------------------------------
 *  begin                : Wed May 9 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken H�ndel
 *
 */
package libsidplay.common;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.Event.Phase;

/**
 * Fast EventScheduler, which maintains a linked list of Events.
 * This scheduler takes neglible time even when it is used to
 * schedule events for nearly every clock.
 * 
 * Events occur on an internal clock which is 2x the visible clock.
 * The visible clock is divided to two phases called phi1 and phi2.
 * 
 * The phi1 clocks are used by VIC and CIA chips, phi2 clocks by CPU.
 * 
 * Scheduling an event for a phi1 clock when system is in phi2 causes the
 * event to be moved to the next phi1 cycle. Correspondingly, requesting
 * a phi1 time when system is in phi2 returns the value of the next phi1.
 *
 * @author Antti S. Lankila
 */
public final class EventScheduler implements Runnable {
	/** EventScheduler's current clock */
	private int currentTime;

	/**
	 * The tail event, always after every other event.
	 */
	private final Event lastEvent = new Event("Tail") {
		{ triggerTime = Integer.MAX_VALUE; }

		@Override
		public void event() {
			throw new RuntimeException("Event scheduler ran out of events to execute");
		}
	};

	/**
	 * The root of event chain, always before and after every other event.
	 */
	private final Event firstEvent = new Event("Root") {
		{
			triggerTime = Integer.MIN_VALUE;
		}

		@Override
		public void event() {
			throw new RuntimeException("Event scheduler executed the root event");
		}
	};

	/**
	 * Periodic thread-safe event scheduling mechanism.
	 */
	private final Event threadSafeQueueingEvent = new Event("Inject events in thread-safe manner.") {
		@Override
		public void event() throws InterruptedException {
			synchronized (threadSafeQueue) {
				for (final Event e : threadSafeQueue) {
					e.event();
				}
				threadSafeQueue.clear();
			}
			schedule(this, 1000000, Event.Phase.PHI1);
		}
	};

	List<Event> threadSafeQueue = new ArrayList<Event>();

	/**
	 * Schedule an event in a thread-safe manner.
	 * 
	 * The thread-safe queue is moved to the unsafe queue periodically,
	 * and execution time of the event is unpredictable.
	 * 
	 * @param event The event to schedule.
	 */
	public void scheduleThreadSafe(final Event event) {
		synchronized (threadSafeQueue) {
			threadSafeQueue.add(event);
		}
	}

	public EventScheduler() {
		reset();
	}

	/** Add event to pending queue.
	 * 
	 * At PHI2, specify cycles=0 and Phase=PHI1 to fire on the very next PHI1.
	 * 
	 * @param event The event to add
	 * @param cycles How many cycles from now to fire
	 * @param phase The phase when to fire the event.
	 */
	public final void schedule(final Event event, final int cycles, final Event.Phase phase) {
		// this strange formulation always selects the next available slot regardless of specified phase.
		event.triggerTime = (cycles << 1) + currentTime + (currentTime & 1 ^ (phase == Event.Phase.PHI1 ? 0 : 1));
		addEventToSchedule(event);
	}

	/** Add event to pending queue in the same phase as current event.
	 * 
	 * @param event The event to add
	 * @param cycles How many cycles from now to fire.
	 */
	public final void schedule(final Event event, final int cycles) {
		event.triggerTime = (cycles << 1) + currentTime;
		addEventToSchedule(event);
	}

	/**
	 * Schedule event to occur at some absolute time.
	 * 
	 * @param event The event to add
	 * @param absoluteCycles When to fire
	 * @param phase Phase when event fires
	 */
	public void scheduleAbsolute(Event event, int absoluteCycles, Phase phase) {
		event.triggerTime = (absoluteCycles << 1) + (phase == Phase.PHI2 ? 1 : 0);
		addEventToSchedule(event);
	}
	
	/**
	 * Scan the event queue and schedule event for execution.
	 * 
	 * @param event The event to add
	 */
	private void addEventToSchedule(final Event event) {
		Event scan = firstEvent;
		while (true) {
			final Event next = scan.next;
			/* find the right spot where to tuck this new event */
			if (next.triggerTime > event.triggerTime) {
				event.next = next;
				scan.next = event;
				break;
			}

			scan = next;
		}
	}

	/** Cancel the specified event.
	 * 
	 * @param event The event to cancel
	 * @return true if an event was actually removed
	 */
	public final boolean cancel(final Event event) {
		Event prev = firstEvent;
		Event scan = firstEvent.next;

		while (scan != lastEvent) {
			if (event == scan) {
				prev.next = scan.next;
				return true;
			}

			prev = scan;
			scan = scan.next;
		}

		return false;
	}

	/** Cancel all pending events and reset time. */
	public final void reset() {
		threadSafeQueue.clear();
		currentTime = 0;
		firstEvent.next = lastEvent;
		schedule(threadSafeQueueingEvent, 0, Event.Phase.PHI1);
	}

	/** Fire next event, advance system time to that event 
	 * @throws InterruptedException */
	public final void clock() throws InterruptedException {
		final Event event = firstEvent.next;
		firstEvent.next = event.next;
		currentTime = event.triggerTime;
		event.event();
	}

	/** Is the event pending in this scheduler?
	 * 
	 *  @param event the event
	 *  @return true when pending
	 */
	public final boolean isPending(final Event event) {
		Event scan = firstEvent.next;
		while (scan != lastEvent) {
			if (event == scan) {
				return true;
			}
			scan = scan.next;
		}
		return false;
	}

	/** Get time with respect to a specific clock phase
	 * 
	 * @param phase The phase
	 * @return the time according to specified phase.
	 */
	public final int getTime(final Event.Phase phase) {
		return currentTime + (phase == Event.Phase.PHI1 ? 1 : 0) >> 1;
	}

	/** Return current clock phase
	 *
	 * @return The current phase
	 */
	public final Event.Phase phase() {
		return (currentTime & 1) == 0 ? Event.Phase.PHI1 : Event.Phase.PHI2;
	}

	/**
	 * Runs the scheduler in a dedicated tight loop.
	 */
	public void run() {
		try {
			while (true) {
				this.clock();
			}
		}
		catch (InterruptedException e) {
		}
	}
}
