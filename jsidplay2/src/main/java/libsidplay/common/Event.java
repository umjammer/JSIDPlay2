package libsidplay.common;

/**
 * Event scheduler (based on alarm from Vice). Created in 2001 by Simon A.
 * White.
 *
 * Optimized EventScheduler and corresponding Event class by Antti S. Lankila in
 * 2009.
 *
 * @author Antti Lankila
 */
public abstract class Event {
	/**
	 * C64 system runs actions at system clock high and low states. The PHI1
	 * corresponds to the auxiliary chip activity and PHI2 to CPU activity. For any
	 * clock, PHI1s are before PHI2s.
	 *
	 * @author Antti Lankila
	 */
	public enum Phase {
		PHI1, PHI2
	}

	/** Describe event for humans. */
	protected final String name;

	/** The clock this event fires */
	protected long triggerTime;

	/** The next event in sequence */
	protected Event next;

	/**
	 * Events are used for delayed execution. Name is not used by code, but is
	 * useful for debugging.
	 *
	 * @param name Descriptive string of the event.
	 */
	protected Event(final String name) {
		this.name = name;
	}

	/**
	 * Event code to be executed. Events are allowed to safely reschedule themselves
	 * with the EventScheduler during invocations.
	 *
	 * @throws InterruptedException
	 */
	public abstract void event() throws InterruptedException;

	@Override
	public String toString() {
		return "[" + name + ",triggerTime=" + triggerTime + "]";
	}

	public static final Event of(String name, ConsumerThatThrows<Event> eventMethod) {
		return new Event(name) {

			@Override
			public void event() throws InterruptedException {
				eventMethod.accept(this);
			}
		};
	}

	@FunctionalInterface
	public interface ConsumerThatThrows<T> {
		void accept(T t) throws InterruptedException;
	}
}
