package libsidplay.components.c1541;

import java.util.concurrent.Semaphore;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

/**
 * TODO W.I.P. seems to break compatibility with most loaders
 */
public class SeparateThreadC1541Runner extends C1541Runner {

	private Thread c1541Thread;
	private final Semaphore semaphore = new Semaphore(0, false);

	public SeparateThreadC1541Runner(final EventScheduler c64Context, final EventScheduler c1541Context) {
		super(c64Context, c1541Context);
	}

	/** Event to wait for master to advance. */
	private final Event slaveWaitsForMaster = new Event("Slave waits for master") {
		@Override
		public void event() throws InterruptedException {
			int allowedTicks = semaphore.drainPermits();
			/*
			 * if we just ran out of clocks, freeze this thread on acquiring the next clock.
			 */
			if (allowedTicks == 0) {
				/* notify master scheduler that the kid is up-to-date */
				synchronized (semaphore) {
					semaphore.notify();
				}
				/* resume by acquiring the next permit */
				semaphore.acquire(1);
				allowedTicks = 1 + semaphore.drainPermits();
			}
			c1541Context.schedule(this, allowedTicks, Event.Phase.PHI2);
		}
	};

	private void clockC1541Context(long offset) {
		/* wait until kid has reached our timestamp */
		synchronized (semaphore) {
			int targetTime = updateSlaveTicks(offset);
			/*
			 * By necessity, we must allow the C1541 to advance for at least 1 clock. This
			 * is because 1541 thread may be stalled at c1541Ticks.acquire(1).
			 * Unfortunately, this will reduce the cycle-exactness of our emulation.
			 * Hopefully this doesn't happen often.
			 */
			if (targetTime <= 0) {
				targetTime = 1;
			}
			semaphore.release(targetTime);
			try {
				semaphore.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void reset() {
		cancel();
		super.reset();
		c64Context.schedule(this, 0, Event.Phase.PHI2);

		startThread();
	}

	@Override
	public void cancel() {
		c64Context.cancel(this);

		stopThread();
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

	private void startThread() {
		c1541Context.schedule(slaveWaitsForMaster, 0, Event.Phase.PHI2);
		c1541Thread = new Thread(new Runnable() {
			/**
			 * Runs the scheduler in a dedicated tight loop.
			 */
			public void run() {
				while (true) {
					try {
						c1541Context.clock();
					} catch (InterruptedException e) {
						break;
					}
				}
			}

		});
		c1541Thread.start();
	}

	private void stopThread() {
		c1541Context.cancel(slaveWaitsForMaster);

		semaphore.drainPermits();

		while (c1541Thread != null && c1541Thread.isAlive()) {
			c1541Thread.interrupt();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
