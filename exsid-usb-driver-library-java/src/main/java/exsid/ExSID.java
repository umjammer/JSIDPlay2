/*
  exSID.c
	A simple I/O library for exSID/exSID+ USB

  (C) 2015-2018 Thibaut VARENE
  License: GPLv2 - http://www.gnu.org/licenses/gpl-2.0.html
 */
package exsid;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ftdi.FTD2XXException;
import com.ftdi.FTDevice;
import com.ftdi.FlowControl;
import com.ftdi.Parity;
import com.ftdi.StopBits;
import com.ftdi.WordLength;

/**
 * 
 * This driver will control the first exSID device available. All public API
 * functions are only valid after a successful call to exSID_init(). To release
 * the device and resources, exSID_exit() must be called.
 * 
 * @author ken
 *
 */
public class ExSID implements IExSID {

	private static final Logger logger = Logger.getLogger(ExSID.class.getName());

	private FTDevice device;

	private long clkdrift;

	private HardwareSpecs hardwareSpecs = new HardwareSpecs();

//#ifndef	EXSID_THREADED
//	private byte backbuf[] = new byte[XS_BUFFSZ];
//	private int backbufIdx = 0;
//#else
	private byte[] bufptr = null;
//#endif

//	#ifdef	EXSID_THREADED
	// Global variables for flip buffering
	private byte bufchar0[] = new byte[XS_BUFFSZ];
	private byte bufchar1[] = new byte[XS_BUFFSZ];
	private byte frontbuf[] = bufchar0, backbuf[] = bufchar1;
	private int frontbufIdx, backbufIdx;
	private Object frontbufMtx = new Object();
//	#endif	// EXSID_THREADED

	@Override
	public String exSID_error_str() {
		return "";
	}

	/**
	 * Write routine to send data to the device.
	 * 
	 * <b>Note:</b> BLOCKING.
	 * 
	 * @param buff pointer to a byte array of data to send
	 * @param size number of bytes to send
	 * @throws FTD2XXException
	 */
	private void xSwrite(byte[] buff, int size) throws FTD2XXException {
		device.write(buff, 0, size);
	}

	/**
	 * Read routine to get data from the device.
	 * 
	 * <b>Note:</b> BLOCKING.
	 * 
	 * @param buff pointer to a byte array that will be filled with read data
	 * @param size number of bytes to read
	 * @return
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	private int xSread(byte[] buff, int size) throws FTD2XXException, InterruptedException {
//	#ifdef	EXSID_THREADED
		synchronized (frontbufMtx) {
			while (frontbufIdx != 0)
				frontbufMtx.wait();
//	#endif
			return device.read(buff, 0, size);
//	#ifdef	EXSID_THREADED
		}
//	#endif
	}

//	#ifdef	EXSID_THREADED
	/**
	 * Writer thread. ** consumer ** This thread consumes buffer prepared in
	 * xSoutb(). Since writes to the FTDI subsystem are blocking, this thread blocks
	 * when it's writing to the chip, and also while it's waiting for the front
	 * buffer to be ready. This ensures execution time consistency as xSoutb()
	 * periodically waits for the front buffer to be ready before flipping buffers.
	 * 
	 * <B>Note: </B> BLOCKING. DOES NOT RETURN, exits when frontbufIdx is negative.
	 *
	 */
	private Thread exSIDthreadOutput = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				while (true) {
					synchronized (frontbufMtx) {

						// wait for frontbuf ready (not empty)
						while (frontbufIdx == 0)
							frontbufMtx.wait();

						// exit condition
						if (frontbufIdx < 0) {
							return;
						}

						xSwrite(frontbuf, frontbufIdx);
						frontbufIdx = 0;

						// xSread() and xSoutb() are in the same thread of execution
						// so it can only be one or the other waiting.
						frontbufMtx.notify();

					}
				}
			} catch (FTD2XXException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	});
//	#endif	// EXSID_THREADED

	/**
	 * Single byte output routine. ** producer ** Fills a static buffer with bytes
	 * to send to the device until the buffer is full or a forced write is
	 * triggered.
	 * 
	 * <b>Note:</b> No drift compensation is performed on read operations.
	 * 
	 * @param b     byte to send
	 * @param flush force write flush if positive, trigger thread exit if negative
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	private void xSoutb(byte b, int flush) throws FTD2XXException, InterruptedException {
		backbuf[backbufIdx++] = b;

		if (backbufIdx < XS_BUFFSZ && flush == 0)
			return;

//		#ifdef	EXSID_THREADED
		// buffer dance
		synchronized (frontbufMtx) {

			// wait for frontbuf available (empty). Only triggers if previous
			// write buffer hasn't been consummed before we get here again.
			while (frontbufIdx != 0)
				frontbufMtx.wait();

			if (flush < 0)
				// indicate exit request
				frontbufIdx = -1;
			else {
				// flip buffers
				bufptr = frontbuf;
				frontbuf = backbuf;
				frontbufIdx = backbufIdx;
				backbuf = bufptr;
				backbufIdx = 0;
			}

			frontbufMtx.notify();
		}
//	#else	// unthreaded
//		xSwrite(backbuf, backbufIdx);
//		backbufIdx = 0;
//	#endif
	}

	/**
	 * Device init routine. Must be called once before any operation is attempted on
	 * the device. Opens first available device, and sets various parameters:
	 * baudrate, parity, flow control and USB latency, and finally clears the RX and
	 * TX buffers.
	 * 
	 * @return 0 on success, !0 otherwise.
	 */
	@Override
	public int exSID_init() {
		if (device != null && device.isOpen()) {
			System.err.println("Device is already open!");
			return -1;
		}

		try {
			/* Attempt to open all supported devices until first success. */
			device = null;
			List<FTDevice> devices = FTDevice.getDevices(false).stream()
					.sorted((d1, d2) -> d1.getDevSerialNumber().compareTo(d2.getDevSerialNumber()))
					.collect(Collectors.toList());

			for (FTDevice ftDevice : devices) {
				for (SupportedDevices xSsup : xSsupported) {
					logger.finest(String.format("Trying %s...\n", xSsup.getDescription()));
					if (ftDevice.getDevDescription().equals(xSsup.getDescription())) {
						device = ftDevice;
						device.open();
						hardwareSpecs = xSsup.getHardwareSpecs();
						break;
					}
				}
			}
			if (device == null) {
				System.err.println("No device could be opened");
				return -1;
			}

			xSfw_usb_setup(XS_BDRATE, XS_USBLAT);

//		#ifdef	EXSID_THREADED
			backbufIdx = frontbufIdx = 0;
			exSIDthreadOutput.setDaemon(true);
			exSIDthreadOutput.start();
//		#endif

			xSfw_usb_purge_buffers();

			// Wait for device ready by trying to read FV and wait for the answer
			// XXX Broken with libftdi due to non-blocking read :-/
			xSoutb(XS_AD_IOCTFV, 1);
			xSread(new byte[1], 1);
			return 0;

		} catch (FTD2XXException | InterruptedException e1) {
			e1.printStackTrace();
			return -1;
		}
	}

	/**
	 * Device exit routine. Must be called to release the device. Resets the SIDs
	 * and clears RX/TX buffers, releases all resources allocated in exSID_init().
	 */
	@Override
	public void exSID_exit() {
		try {
			if (device != null) {
				exSID_reset((byte) 0);

//		#ifdef	EXSID_THREADED
				xSoutb(XS_AD_IOCTFV, -1); // signal end of thread
				exSIDthreadOutput.join();
//		#endif

				xSfw_usb_purge_buffers();

				xSfw_usb_close();
				device = null;
			}
			clkdrift = 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * SID reset routine. Performs a hardware reset on the SIDs.
	 * 
	 * <b>Note:</b> since the reset procedure in firmware will stall the device,
	 * reset forcefully waits for enough time before resuming execution via a call
	 * to usleep();
	 * 
	 * @param volume volume to set the SIDs to after reset.
	 */
	@Override
	public void exSID_reset(byte volume) {
		try {
			// this will stall
			xSoutb(XS_AD_IOCTRS, 1);
			// sleep for 100us
			usleep(100);
			// this only needs 2 bytes which matches the input buffer of the PIC so all is
			// well
			exSID_write((byte) 0x18, volume, 1);
			clkdrift = 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * exSID+ clock selection routine. Selects between PAL, NTSC and 1MHz clocks.
	 * 
	 * <B>Note:</B> upon clock change the hardware resync itself and resets the
	 * SIDs, which takes approximately 50us: this function waits for enough time
	 * before resuming execution via a call to usleep(); Output should be muted
	 * before execution
	 * 
	 * @param clock clock selector value
	 * @return execution status
	 */
	@Override
	public int exSID_clockselect(int clock) {

		try {
			if (XS_MODEL_PLUS != hardwareSpecs.getModel())
				return -1;

			ClockSelect clk = ClockSelect.get(clock);
			switch (clk) {
			case XS_CL_PAL:
				xSoutb(XSP_AD_IOCTCP, 1);
				break;
			case XS_CL_NTSC:
				xSoutb(XSP_AD_IOCTCN, 1);
				break;
			case XS_CL_1MHZ:
				xSoutb(XSP_AD_IOCTC1, 1);
				break;
			default:
				return -1;
			}
			usleep(100); // sleep for 100us

			clkdrift = 0; // reset drift

			return 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * exSID+ audio operations routine. Selects the audio mixing / muting option.
	 * Only implemented in exSID+ devices.
	 * 
	 * <B>Warning:</B> all these operations (excepting unmuting obviously) will mute
	 * the output by default. <B>Note:</B> no accounting for SID cycles consumed.
	 * 
	 * @param operation audio operation value
	 * @return execution status
	 */
	@Override
	public int exSID_audio_op(int operation) {
		try {
			if (XS_MODEL_PLUS != hardwareSpecs.getModel())
				return -1;

			AudioOp op = AudioOp.get(operation);
			switch (op) {
			case XS_AU_6581_8580:
				xSoutb(XSP_AD_IOCTA0, 0);
				break;
			case XS_AU_8580_6581:
				xSoutb(XSP_AD_IOCTA1, 0);
				break;
			case XS_AU_8580_8580:
				xSoutb(XSP_AD_IOCTA2, 0);
				break;
			case XS_AU_6581_6581:
				xSoutb(XSP_AD_IOCTA3, 0);
				break;
			case XS_AU_MUTE:
				xSoutb(XSP_AD_IOCTAM, 0);
				break;
			case XS_AU_UNMUTE:
				xSoutb(XSP_AD_IOCTAU, 0);
				break;
			default:
				return -1;
			}

			return 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * SID chipselect routine. Selects which SID will play the tunes. If neither
	 * CHIP0 or CHIP1 is chosen, both SIDs will operate together. Accounts for
	 * elapsed cycles.
	 * 
	 * @param chip SID selector value
	 */
	@Override
	public void exSID_chipselect(int chip) {
		try {
			clkdrift -= hardwareSpecs.getCsioctlCycles();

			ChipSelect ch = ChipSelect.get(chip);
			switch (ch) {
			case XS_CS_CHIP0:
				xSoutb(XS_AD_IOCTS0, 0);
				break;
			case XS_CS_CHIP1:
				xSoutb(XS_AD_IOCTS1, 0);
				break;
			default:
				xSoutb(XS_AD_IOCTSB, 0);
				break;
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Device hardware model. Queries the driver for the hardware model currently
	 * identified.
	 * 
	 * @return hardware model, negative value on error.
	 */
	@Override
	public int exSID_hwmodel() {
		switch (hardwareSpecs.getModel()) {
		case XS_MODEL_STD:
			return HardwareModel.XS_MD_STD.getHardwareModel();
		case XS_MODEL_PLUS:
			return HardwareModel.XS_MD_PLUS.getHardwareModel();
		default:
			return -1;
		}
	}

	/**
	 * Hardware and firmware version of the device. Queries the device for the
	 * hardware revision and current firmware version and returns both in the form
	 * of a 16bit integer: MSB is an ASCII character representing the hardware
	 * revision (e.g. 0x42 = "B"), and LSB is a number representing the firmware
	 * version in decimal integer. Does NOT account for elapsed cycles.
	 * 
	 * @return version information as described above.
	 */
	@Override
	public short exSID_hwversion() {
		try {
			xSoutb(XS_AD_IOCTHV, 0);
			xSoutb(XS_AD_IOCTFV, 1);

			byte inbuf[] = new byte[2];
			xSread(inbuf, 2);

			// ensure proper order regardless of endianness
			return (short) (inbuf[0] << 8 | inbuf[1]);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Private busy delay loop.
	 * 
	 * <B>Note:</B> will block every time a device write is triggered, blocking time
	 * will be equal to the number of bytes written times mindelCycles.
	 * 
	 * @param cycles how many SID clocks to loop for.
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	private void xSdelay(long cycles) throws FTD2XXException, InterruptedException {
		while (cycles >= hardwareSpecs.getMindelCycles()) {
			xSoutb(XS_AD_IOCTD1, 0);
			cycles -= hardwareSpecs.getMindelCycles();
			clkdrift -= hardwareSpecs.getMindelCycles();
		}
	}

	/**
	 * Private long delay loop. Calls to IOCTLD delay, for "very" long delays
	 * (thousands of SID clocks). Requested delay MUST be &gt; ldelayOffs, and for
	 * better performance, the requested delay time should ideally be several
	 * XS_LDMULT and be close to a multiple of XS_USBLAT milliseconds (on the
	 * exSID).
	 * 
	 * <B>Warning:</B> polling and NOT CYCLE ACCURATE on exSID
	 * 
	 * @param cycles how many SID clocks to wait for.
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unused")
	private void xSlongdelay(long cycles) throws FTD2XXException, InterruptedException {
		int multiple;
		int flush;
		long delta;
		byte dummy[] = new byte[1];

		flush = (XS_MODEL_STD == hardwareSpecs.getModel()) ? 1 : 0;

		multiple = (int) (cycles - hardwareSpecs.getLdelayOffs());
		delta = multiple % XS_LDMULT;
		multiple /= XS_LDMULT;

		if (multiple < 0) {
			return;
		}

		while (multiple >= 255) {
			exSID_write(XS_AD_IOCTLD, (byte) 255, flush);
			if (flush != 0)
				// wait for answer with blocking read
				xSread(dummy, 1);
			multiple -= 255;
		}

		if (multiple != 0) {
			exSID_write(XS_AD_IOCTLD, (byte) multiple, flush);
			if (flush != 0)
				// wait for answer with blocking read
				xSread(dummy, 1);
		}

		// deal with remainder
		xSdelay(delta);
	}

	/**
	 * Cycle accurate delay routine. Applies the most efficient strategy to delay
	 * for cycles SID clocks while leaving enough lead time for an I/O operation.
	 * 
	 * @param cycles how many SID clocks to loop for.
	 */
	@Override
	public void exSID_delay(long cycles) {
		try {
			long delay;

			clkdrift += cycles;

			// never delay for less than a full write would need
			if (clkdrift <= hardwareSpecs.getWriteCycles())
				// too short
				return;

			delay = clkdrift - hardwareSpecs.getWriteCycles();

			switch (hardwareSpecs.getModel()) {
			// currently breaks sidplayfp - REVIEW
//			case XS_MODEL_PLUS:
//				if (delay > XS_LDMULT) {
//					xSlongdelay(delay);
//					break;
//				}
			default:
				xSdelay(delay);
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Private write routine for a tuple address + data.
	 * 
	 * @param addr  target address to write to.
	 * @param data  data to write at that address.
	 * @param flush if non-zero, force immediate flush to device.
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	private void exSID_write(byte addr, byte data, int flush) throws FTD2XXException, InterruptedException {
		xSoutb(addr, 0);
		xSoutb(data, flush);
	}

	/**
	 * Timed write routine, attempts cycle-accurate writes. This function will be
	 * cycle-accurate provided that no two consecutive reads or writes are less than
	 * writeCycles apart and the leftover delay is &lt;= maxAdj SID clock cycles.
	 * 
	 * @param cycles how many SID clocks to wait before the actual data write.
	 * @param addr   target address.
	 * @param data   data to write at that address.
	 */
	@Override
	public void exSID_clkdwrite(long cycles, byte addr, byte data) {
		try {
			// actual write will cost writeCycles. Delay for cycles - write_cycles then
			// account for the write
			clkdrift += cycles;
			if (clkdrift > hardwareSpecs.getWriteCycles())
				xSdelay(clkdrift - hardwareSpecs.getWriteCycles());

			// write is going to consume write_cycles clock ticks
			clkdrift -= hardwareSpecs.getWriteCycles();

			/*
			 * if we are still going to be early, delay actual write by up to XS_MAXAD ticks
			 * At this point it is guaranted that clkdrift will be < mindelCycles.
			 */
			if (clkdrift >= 0) {
				long adj = clkdrift % (hardwareSpecs.getMaxAdj() + 1);
				/*
				 * if max_adj is >= clkdrift, modulo will give the same results as the correct
				 * test: adj = (clkdrift < max_adj ? clkdrift : maxAdj) but without an extra
				 * conditional branch. If is is < maxAdj, then it seems to provide better
				 * results by evening jitter accross writes. So it's the preferred solution for
				 * all cases.
				 */
				// final delay encoded in top 3 bits of address
				addr = (byte) (addr | (adj << 5));
			}

			exSID_write(addr, data, 0);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Private read routine for a given address.
	 * 
	 * @param addr  target address to read from.
	 * @param flush if non-zero, force immediate flush to device.
	 * @return data read from address.
	 * @throws FTD2XXException
	 * @throws InterruptedException
	 */
	private byte exSID_read(byte addr, int flush) throws FTD2XXException, InterruptedException {
		byte data[] = new byte[1];

		// XXX
		xSoutb(addr, flush);
		// blocking
		xSread(data, 1);

		return data[0];
	}

	/**
	 * BLOCKING Timed read routine, attempts cycle-accurate reads. The following
	 * description is based on exSID (standard). This function will be
	 * cycle-accurate provided that no two consecutive reads or writes are less than
	 * XS_CYCIO apart and leftover delay is &lt;= maxAdj SID clock cycles. Read
	 * result will only be available after a full XS_CYCIO, giving clkdread() the
	 * same run time as clkdwrite(). There's a 2-cycle negative adjustment in the
	 * code because that's the actual offset from the write calls ('/' denotes
	 * falling clock edge latch), which the following ASCII tries to illustrate:
	 * 
	 * <br>
	 * 
	 * Write looks like this in firmware:
	 * 
	 * <pre>
	 *  &gt; ...|_/_|...
	 * </pre>
	 * 
	 * ...end of data byte read | cycle during which write is enacted / next cycle |
	 * etc...
	 * 
	 * <br>
	 * 
	 * Read looks like this in firmware:
	 * 
	 * <pre>
	 * &gt; ...|_|_|_/_|_|...
	 * </pre>
	 * 
	 * ...end of address byte read | 2 cycles for address processing | cycle during
	 * which SID is read / then half a cycle later the CYCCHR-long data TX starts,
	 * cycle completes | another cycle | etc...
	 * 
	 * <br>
	 * 
	 * This explains why reads happen a relative 2-cycle later than then should with
	 * respect to writes.
	 * 
	 * <B>Note:</B> The actual time the read will take to complete depends on the
	 * USB bus activity and settings. It *should* complete in XS_USBLAT ms, but not
	 * less, meaning that read operations are bound to introduce timing inaccuracy.
	 * As such, this function is only really provided as a proof of concept but
	 * SHOULD BETTER BE AVOIDED.
	 * 
	 * @param cycles how many SID clocks to wait before the actual data read.
	 * @param addr   target address.
	 * @return data read from address.
	 */
	@Override
	public byte exSID_clkdread(long cycles, byte addr) {
		try {
			// actual read will happen after read_pre_cycles. Delay for cycles -
			// read_pre_cycles then account for the read
			// 2-cycle offset adjustement, see function documentation.
			clkdrift += hardwareSpecs.getReadOffsetCycles();
			clkdrift += cycles;
			if (clkdrift > hardwareSpecs.getReadPreCycles())
				xSdelay(clkdrift - hardwareSpecs.getReadPreCycles());

			// read request is going to consume read_pre_cycles clock ticks
			clkdrift -= hardwareSpecs.getReadPreCycles();

			// if we are still going to be early, delay actual read by up to max_adj ticks
			if (clkdrift >= 0) {
				// see clkdwrite()
				long adj = clkdrift % (hardwareSpecs.getMaxAdj() + 1);
				addr = (byte) (addr | (adj << 5)); // final delay encoded in top 3 bits of address
			}

			// after read has completed, at least another read_post_cycles will have been
			// spent
			clkdrift -= hardwareSpecs.getReadPostCycles();

			return exSID_read(addr, 1);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Setup FTDI chip to match exSID firmware. Defaults to 8N1, no flow control.
	 * 
	 * @param baudrate Target baudrate
	 * @param latency  Target latency
	 * @throws IllegalArgumentException
	 * @throws FTD2XXException
	 */
	private void xSfw_usb_setup(long baudrate, short latency) throws FTD2XXException, IllegalArgumentException {
		device.setBaudRate(baudrate);
		device.setDataCharacteristics(WordLength.BITS_8, StopBits.STOP_BITS_1, Parity.PARITY_NONE);
		device.setFlowControl(FlowControl.FLOW_NONE);
		device.setLatencyTimer(latency);
	}

	private void xSfw_usb_purge_buffers() throws FTD2XXException {
		device.purgeBuffer(true, true);
	}

	private void xSfw_usb_close() throws FTD2XXException {
		device.close();
	}

	private void usleep(int delayUs) {
		int delayNs = delayUs * 1000;
		long start = System.nanoTime();
		long end = 0;
		do {
			end = System.nanoTime();
		} while (start + delayNs >= end);
	}
}
