package common;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidutils.CBMCodeUtils;
import sidplay.Player;
import ui.JSidPlay2;
import ui.JSidPlay2Main;
import ui.entities.config.Configuration;

public class JSIDPlay2Test extends FxRobot {

	private static JSidPlay2 jSidplay2;

	public static class JSidPlay2TestMain extends JSidPlay2Main {
		@Override
		public void start(Stage primaryStage) {
			super.start(primaryStage);
			JSIDPlay2Test.jSidplay2 = jSidplay2;
			primaryStage.setOnCloseRequest(evt -> evt.consume());
			primaryStage.show();
		}
	}

	/**
	 * Sleep time to wait for JSIDPlay2 to start.
	 */
	protected static final int JSIDPLAY2_STARTUP_SLEEP = 1000;
	/**
	 * Timeout for the file browser to open.
	 */
	protected static final int FILE_BROWSER_OPENED_TIMEOUT = 2000;
	/**
	 * Timeout until the file chooser has selected a file completely.
	 */
	protected static final int FILE_CHOOSER_TIMEOUT = 1500;
	/**
	 * Timeout until the C64 has been reset completely.
	 */
	protected static final int C64_RESET_TIMEOUT = 5000;
	/**
	 * Timeout for a thread-safe scheduled event.
	 */
	protected static final int SCHEDULE_THREADSAFE_TIMEOUT = 2000;
	/**
	 * Maximum Fast forward speed.
	 */
	protected static final int SPEED_FACTOR = 3;

	protected Configuration config;
	protected Player player;

	private boolean abortTest;

	/**
	 * Launch JSIDPlay2.
	 *
	 * @throws Exception launch error
	 */
	@BeforeClass
	public static void setUpClass() throws Exception {
		try {
			if (jSidplay2 == null) {
				FxToolkit.registerPrimaryStage();
				FxToolkit.setupApplication(JSidPlay2TestMain.class);
			}
			do {
				Thread.sleep(JSIDPLAY2_STARTUP_SLEEP);
			} while (jSidplay2 == null);
		} catch (InterruptedException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Before
	public final void jsidplay2Before() {
		jSidplay2.setCloseActionEnabler(() -> {
			// abort test on close operation
			abortTest = true;
			// do not close application
			return false;
		});
		abortTest = false;
		config = jSidplay2.getUtil().getConfig();
		player = jSidplay2.getUtil().getPlayer();
		player.setMenuHook(player -> {
		});
	}

	@Override
	public FxRobot sleep(long milliseconds) {
		if (abortTest) {
			fail("Application closed!");
		}
		// we want to stay responsive for application abort
		while (milliseconds > 500) {
			milliseconds -= 500;
			super.sleep(500);
			if (abortTest) {
				fail("Application closed!");
			}
		}
		return super.sleep(milliseconds);
	}

	public void load() {
		clickOn("#VIDEO");
		press(KeyCode.CONTROL);
		type(KeyCode.L);
		release(KeyCode.CONTROL);
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
	}

	public void insertDisk() {
		clickOn("#VIDEO");
		press(KeyCode.CONTROL);
		type(KeyCode.DIGIT8);
		release(KeyCode.CONTROL);
		sleep(FILE_BROWSER_OPENED_TIMEOUT);
	}

	/**
	 * Check C64 RAM contents.
	 *
	 * @param address  start address
	 * @param expected expected byte contents at the specified address
	 * @return RAM contents equals
	 */
	protected boolean checkRam(int address, byte[] expected) {
		final byte[] ram = player.getC64().getRAM();
		for (int i = 0; i < expected.length; i++) {
			if (ram[address + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check a message on C64 video screen RAM.
	 *
	 * @param expected expected message visible on the video screen
	 * @param row      expected message appears on this row
	 * @param column   expected message appears on this column
	 * @return Video RAM contains the specified message
	 */
	protected boolean checkScreenMessage(String expected, int row, int column) {
		final byte[] ram = player.getC64().getRAM();
		final int offset = (row - 1) * 40 + column - 1;
		for (int i = 0; i < expected.length(); i++) {
			final byte screenCode = CBMCodeUtils.iso88591ToScreenRam(expected.charAt(i));
			if (ram[0x0400 + offset + i] != screenCode) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Schedule a thread-safe C64 event.
	 *
	 * @param eventConsumer event consumer with event behavior
	 */
	protected void schedule(java.util.function.Consumer<C64> eventConsumer) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(Event.of("Test Event", event -> eventConsumer.accept(player.getC64())));
	}

	protected void fastForward(int speedFactor) {
		player.configureMixer(mixer -> {
			for (int i = 0; i < speedFactor; i++) {
				mixer.fastForward();
			}
		});
	}

}
