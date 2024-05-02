package libsidplay.components.c1541;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

public class SameThreadC1541Runner extends C1541Runner {
	protected boolean notTerminated;

	private final Event terminationEvent = Event.of("Pause C1541", event -> notTerminated = false);

	public SameThreadC1541Runner(final EventScheduler c64Context, final EventScheduler c1541Context) {
		super(c64Context, c1541Context);
	}

	private void clockC1541Context(long offset) {
		final int targetTime = updateSlaveTicks(offset);
		if (targetTime <= 0) {
			return;
		}

		c1541Context.schedule(terminationEvent, targetTime, Event.Phase.PHI2);
		notTerminated = true;

		try {
			while (notTerminated) {
				c1541Context.clock();
			}
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset() {
		cancel();
		super.reset();
		c64Context.schedule(this, 0, Event.Phase.PHI2);
	}

	@Override
	public void cancel() {
		c64Context.cancel(this);

		c1541Context.cancel(terminationEvent);
	}

	/**
	 * Synchronize C1541 and C64 schedulers. Called by C64; C1541 will be sleeping
	 * and in sync once we return.
	 */
	@Override
	public void synchronize(long offset) {
		clockC1541Context(offset);
	}

	@Override
	public void event() throws InterruptedException {
		synchronize(0);
		c64Context.schedule(this, 2000);
	}

}