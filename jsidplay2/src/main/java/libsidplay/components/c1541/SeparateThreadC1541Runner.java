package libsidplay.components.c1541;

import java.util.concurrent.Semaphore;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

/**
 * TODO W.I.P. seems to break compatibility with most loaders
 */
public class SeparateThreadC1541Runner extends C1541Runner {

	private Thread c1541Thread;
	private final Semaphore c1541Ticks = new Semaphore(0, false);

	public SeparateThreadC1541Runner(final EventScheduler c64Context, final EventScheduler c1541Context) {
		super(c64Context, c1541Context);
	}

	/** Event to wait for master to advance. */
	private final Event slaveWaitsForMaster = new Event("Slave waits for master") {
		@Override
		public void event() throws InterruptedException {
			int allowedTicks = c1541Ticks.drainPermits();
			/*
			 * if we just ran out of clocks, freeze this thread on acquiring the next clock.
			 */
			if (allowedTicks == 0) {
				/* notify master scheduler that the kid is up-to-date */
				synchronized (c1541Ticks) {
					c1541Ticks.notify();
				}
				/* resume by acquiring the next permit */
				c1541Ticks.acquire(1);
				allowedTicks = 1 + c1541Ticks.drainPermits();
			}
			c1541Context.schedule(this, allowedTicks, Event.Phase.PHI2);
		}
	};

	/**
	 * Synchronize C1541 and C64 schedulers. Called by C64; C1541 will be sleeping
	 * and in sync once we return.
	 */
	@Override
	public void synchronize(long offset) {
		try {
			/* wait until kid has reached our timestamp */
			synchronized (c1541Ticks) {
				if (offset == 0) {
					offset = 1;
				}
				int clocks = updateSlaveTicks(offset);
				/*
				 * By necessity, we must allow the C1541 to advance for at least 1 clock. This
				 * is because 1541 thread may be stalled at c1541Ticks.acquire(1).
				 * Unfortunately, this will reduce the cycle-exactness of our emulation.
				 * Hopefully this doesn't happen often.
				 */
				if (clocks == 0) {
					clocks = 1;
				}
				c1541Ticks.release(clocks);
				c1541Ticks.wait();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void event() throws InterruptedException {
		c1541Ticks.release(updateSlaveTicks(1)); // XXX 1?
		c64Context.schedule(this, 2000, Event.Phase.PHI2);
	}

	@Override
	public void reset() {
		c1541Ticks.drainPermits();

		cancel();
		
		super.reset();
		
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

		c64Context.schedule(this, 0, Event.Phase.PHI2);
	}

	@Override
	public void cancel() {
		c64Context.cancel(this);

		while (c1541Thread !=null &&c1541Thread.isAlive()) {
			c1541Thread.interrupt();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		c1541Context.cancel(slaveWaitsForMaster);
	}

}
