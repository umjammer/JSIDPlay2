/**
 * exSID.c
 * A simple I/O library for exSID/exSID+ USB
 *
 * (C) 2015-2018 Thibaut VARENE
 * License: GPLv2 - http://www.gnu.org/licenses/gpl-2.0.html
 *
 * http://hacks.slashdirt.org/hw/exsid/
 
 * Javascript port by Ken Händel
 * 
 * This driver will control the first exSID device available. All public API
 * functions are only valid after a successful call to exSID_init(). To release
 * the device and resources, exSID_exit() must be called.
 * 
 * @author ken
 *
 */
 
/** Hardware model return values for exSID_hwmodel() */
const HardwareModel = {
	/**
	 * exSID USB
	 */
	XS_MD_STD: 0,
	/**
	 * exSID+ USB
	 */
	XS_MD_PLUS: 1
};

/** Audio output operations for exSID_audio_op() */
const AudioOp = {
	/**
	 * mix: 6581 L / 8580 R
	 */
	XS_AU_6581_8580: 0,
	/**
	 * mix: 8580 L / 6581 R
	 */
	XS_AU_8580_6581: 1,
	/**
	 * mix: 8580 L and R
	 */
	XS_AU_8580_8580: 2,
	/**
	 * mix: 6581 L and R
	 */
	XS_AU_6581_6581: 3,
	/**
	 * mute output
	 */
	XS_AU_MUTE: 4,
	/**
	 * unmute output
	 */
	XS_AU_UNMUTE: 5
};

/** Chip selection values for exSID_chipselect() */
const ChipSelect = {
	/**
	 * 6581
	 */
	XS_CS_CHIP0: 0,
	/**
	 * 8580
	 */
	XS_CS_CHIP1: 1,
	/**
	 * Both chips. XXX Invalid for reads: undefined behavior!
	 */
	XS_CS_BOTH: 2
};

/** Clock selection values for exSID_clockselect() */
const ClockSelect = {
	/**
	 * select PAL clock
	 */
	XS_CL_PAL: 0,
	/**
	 * select NTSC clock
	 */
	XS_CL_NTSC: 1,
	/**
	 * select 1MHz clock
	 */
	XS_CL_1MHZ: 2
};

//
// exSID hardware definitions
//

/**
 * 2Mpbs
 */
const XS_BDRATE = 2000000;
/**
 * write buffer size in milliseconds of playback.
 */
const XS_BUFFMS = 40;
/**
 * 1MHz (for computation only, currently hardcoded in firmware)
 */
const XS_SIDCLK = 1000000;
/**
 * RS232 byte clock. Each RS232 byte is 10 bits long due to start and stop bits
 */
const XS_RSBCLK = (XS_BDRATE / 10);
/**
 * SID cycles between two consecutive chars
 */
const XS_CYCCHR = (XS_SIDCLK / XS_RSBCLK);
/**
 * FTDI latency: 2-255ms in 1ms increments
 */
const XS_USBLAT = 2;
/**
 * Must be multiple of _62_ or USB won't be happy.
 */
const XS_BUFFSZ = ((((XS_RSBCLK / 1000) * XS_BUFFMS) / 62) * 62);
/**
 * long delay SID cycle loop multiplier
 */
const XS_LDMULT = 501;
/**
 * Smallest possible delay (with IOCTD1).
 */
const XS_MINDEL = (XS_CYCCHR);
/**
 * minimum cycles between two consecutive I/Os (addr + data)
 */
const XS_CYCIO = (2 * XS_CYCCHR);
/**
 * maximum encodable value for post write clock adjustment: must fit on 3 bits
 */
const XS_MAXADJ = 7;
/**
 * long delay loop SID cycles offset
 */
const XS_LDOFFS = (3 * XS_CYCCHR);

//
// exSID+ hardware definitions
//

/**
 * Smallest possible delay (with IOCTD1).
 */
const XSP_MINDEL = 2;
/**
 * minimum cycles between two consecutive I/Os (addr + data)
 */
const XSP_CYCIO = 3;
const XSP_PRE_RD = 2;
const XSP_POSTRD = 2;
/**
 * maximum encodable value for post write clock adjustment: must fit on 3 bits
 */
const XSP_MAXADJ = 4;
/**
 * long delay loop SID cycles offset
 */
const XSP_LDOFFS = 3;
/**
 * cycles lost in chipselect()
 */
const XSP_CYCCS = 2;

// IOCTLS
// IO controls 0x3D to 0x7F are only implemented on exSID+

/**
 * Select PAL clock
 */
const XSP_AD_IOCTCP = 0x3D;
/**
 * Select NTSC clock
 */
const XSP_AD_IOCTCN = 0x3E;
/**
 * Select 1MHz clock
 */
const XSP_AD_IOCTC1 = 0x3F;

/**
 * Audio Mix: 6581 L / 8580 R
 */
const XSP_AD_IOCTA0 = 0x5D;
/**
 * Audio Mix: 8580 L / 6581 R
 */
const XSP_AD_IOCTA1 = 0x5E;
/**
 * Audio Mix: 8580 L / 8580 R
 */
const XSP_AD_IOCTA2 = 0x5F;

/**
 * Audio Mix: 6581 L / 6581 R
 */
const XSP_AD_IOCTA3 = 0x7D;
/**
 * Audio Mute
 */
const XSP_AD_IOCTAM = 0x7E;
/**
 * Audio Unmute
 */
const XSP_AD_IOCTAU = 0x7F;

/**
 * shortest delay (XS_MINDEL SID cycles)
 */
const XS_AD_IOCTD1 = 0x9D;
/**
 * polled delay, amount of SID cycles to wait must be given in data
 */
const XS_AD_IOCTLD = 0x9E;

/**
 * select chip 0
 */
const XS_AD_IOCTS0 = 0xBD;
/**
 * select chip 1
 */
const XS_AD_IOCTS1 = 0xBE;
/**
 * select both chips. @warning Invalid for reads: unknown behaviour!
 */
const XS_AD_IOCTSB = 0xBF;

/**
 * Firmware version query
 */
const XS_AD_IOCTFV = 0xFD;
/**
 * Hardware version query
 */
const XS_AD_IOCTHV = 0xFE;
/**
 * SID reset
 */
const XS_AD_IOCTRS = 0xFF;

/**
 * Default FTDI VID
 */
const XS_USBVID = 0x0403;
/**
 * Default FTDI PID
 */
const XS_USBPID = 0x6001;
const XS_USBDSC = "exSID USB";

/**
 * Default FTDI VID
 */
const XSP_USBVID = 0x0403;
/**
 * Default FTDI PID
 */
const XSP_USBPID = 0x6015;
const XSP_USBDSC = "exSID+ USB";

const XS_MODEL_STD = 0;
const XS_MODEL_PLUS = 1;

class HardwareSpecs {

	constructor(model, writeCycles, readPreCycles, readPostCycles, readOffsetCycles, csioctlCycles, mindelCycles, maxAdj, ldelayOffs) {
		// exSID device model in use
		this.model = model;
		// number of SID clocks spent in write ops
		this.writeCycles = writeCycles;
		// number of SID clocks spent in read op before data is actually read
		this.readPreCycles = readPreCycles;
		// number of SID clocks spent in read op after data is actually read
		this.readPostCycles = readPostCycles;
		// read offset adjustment to align with writes (see function documentation)
		this.readOffsetCycles = readOffsetCycles;
		// number of SID clocks spent in chip select ioctl
		this.csioctlCycles = csioctlCycles;
		// lowest number of SID clocks that can be accounted for in delay
		this.mindelCycles = mindelCycles;
		// maximum number of SID clocks that can be encoded in final delay for  read()/write()
		this.maxAdj = maxAdj;
		// long delay SID clocks offset
		this.ldelayOffs = ldelayOffs;
	}
}

class SupportedDevices {
	constructor(description, pid, vid, model, writeCycles, readPreCycles, readPostCycles, readOffsetCycles, csioctlCycles, mindelCycles, maxAdj, ldelayOffs) {
		this.description = description;
		this.pid = pid;
		this.vid = vid;
		this.hardwareSpecs = new HardwareSpecs(model, writeCycles, readPreCycles, readPostCycles, readOffsetCycles, csioctlCycles, mindelCycles, maxAdj, ldelayOffs);
	}
}

/* exSID USB */
var exSID = new SupportedDevices(XS_USBDSC, XS_USBPID, XS_USBVID, XS_MODEL_STD, XS_CYCIO, XS_CYCCHR, XS_CYCCHR, -2,
	XS_CYCCHR, XS_MINDEL, XS_MAXADJ, XS_LDOFFS);

/* exSID+ USB */
var exSIDPlus = new SupportedDevices(XSP_USBDSC, XSP_USBPID, XSP_USBVID, XS_MODEL_PLUS, XSP_CYCIO, XSP_PRE_RD, XSP_POSTRD,
	0, XSP_CYCCS, XSP_MINDEL, XSP_MAXADJ, XSP_LDOFFS);

var xSsupported = [exSID, exSIDPlus];

var ftdi, clkdrift;

var hardwareSpecs;

var backbuf = new Array(XS_BUFFSZ);
var backbufIdx = 0;

/**
 * Write routine to send data to the device.
 * 
 * <b>Note:</b> BLOCKING.
 * 
 * @param {Array} buff
 *         pointer to a byte array of data to send
 * @param {number} size
 *         number of bytes to send
 */
async function xSwrite(buff, size) {
	try {
		const result = new Uint8Array(size);
		for (var i = 0; i < size; i++) {
			result[i] = buff[i];
		}
		await ftdi.writeAsync(result);
	} catch (error) {
	}
}

/**
 * Read routine to get data from the device.
 * 
 * <b>Note:</b> BLOCKING.
 * 
 * @param {Array} buff
 *         pointer to a byte array that will be filled with read data
 * @param size number of bytes to read
 */
async function xSread(buff, size) {
// XXX READ SUPPORT!
	var result = await ftdi.read();
	for (var i = 0; i < size; i++) {
		buff[i] = result;
	}
}

/**
 * Single byte output routine. ** producer ** Fills a static buffer with bytes
 * to send to the device until the buffer is full or a forced write is
 * triggered.
 * 
 * <b>Note:</b> No drift compensation is performed on read operations.
 * 
 * @param {byte} b
 *         byte to send
 * @param {boolean} flush
 *         force write flush if positive, trigger thread exit if negative
 */
async function xSoutb(b, flush) {
	backbuf[backbufIdx++] = b;

	if (backbufIdx < XS_BUFFSZ && flush == 0)
		return;

	xSwrite(backbuf, backbufIdx);
	backbufIdx = 0;
}

/**
 * Device init routine. Must be called once before any operation is attempted on
 * the device. Opens first available device, and sets various parameters:
 * baudrate, parity, flow control and USB latency, and finally clears the RX and
 * TX buffers.
 * 
 * @return {number}
 *         0 on success, !0 otherwise.
 */
async function exSID_init() {
	if (ftdi && ftdi.isOpen()) {
		console.log("Device is already open!");
		return -1;
	}

	try {
		/* Attempt to open all supported devices until first success. */
		device = {};
		ftdi = new FTDI();
		await ftdi.init(XS_USBVID, {baudRate: XS_BDRATE});
		
		for (xSsup of xSsupported) {
			console.log("Trying " + xSsup.description + "...");
			if (ftdi.device.productName === xSsup.description) {
				device.ftdi = ftdi;
				hardwareSpecs = xSsup.hardwareSpecs;
				break;
			}
		}
		if (device == null) {
			console.log("No device could be opened");
			return -1;
		}

		await xSfw_usb_setup(XS_BDRATE, XS_USBLAT);

		//)	#ifdef	EXSID_THREADED
		//		backbufIdx = frontbufIdx = 0;
		//		exSIDthreadOutput.setDaemon(true);
		//		exSIDthreadOutput.start();
		//	#endif

		await xSfw_usb_purge_buffers();
		clkdrift = 0;

		// Wait for device ready by trying to read FV and wait for the answer
		// XXX Broken with libftdi due to non-blocking read :-/
		await xSoutb(XS_AD_IOCTFV, 1);
		await xSread(new Uint8Array(1), 1);
		return 0;

	} catch (err) {
		console.log(err);
		return -1;
	}
}

/**
 * Device exit routine. Must be called to release the device. Resets the SIDs
 * and clears RX/TX buffers, releases all resources allocated in exSID_init().
 */
async function exSID_exit() {
	if (device) {
		await exSID_reset(0);
		await xSfw_usb_purge_buffers();

		await xSfw_usb_close();
		device = undefined;
	}
	clkdrift = 0;
}

/**
 * SID reset routine. Performs a hardware reset on the SIDs.
 * 
 * <b>Note:</b> since the reset procedure in firmware will stall the device,
 * reset forcefully waits for enough time before resuming execution via a call
 * to usleep();
 * 
 * @param {number} volume
 *         volume to set the SIDs to after reset.
 */
async function exSID_reset(volume) {
//	console.log(exSID_hwversion());
	await exSID_clockselect(ClockSelect.XS_CL_PAL);
	await exSID_chipselect(ChipSelect.XS_CS_CHIP0);
	await xSfw_usb_purge_buffers();
	await delay(250); // wait for send/receive to complete
	// this will stall
	await xSoutb(XS_AD_IOCTRS, 1);
	// sleep for 100us
	await delay(100); // wait for send/receive to complete
	// this only needs 2 bytes which matches the input buffer of the PIC so all is
	// well
	await exSID_write(0x18, volume, 1);
	clkdrift = 0;
	backbuf = new Array(XS_BUFFSZ);
	backbufIdx = 0;
	
}

/**
 * exSID+ clock selection routine. Selects between PAL, NTSC and 1MHz clocks.
 * 
 * <B>Note:</B> upon clock change the hardware resync itself and resets the
 * SIDs, which takes approximately 50us: this function waits for enough time
 * before resuming execution via a call to usleep(); Output should be muted
 * before execution
 * 
 * @param {Object} clock
 *         clock selector value
 * @return {number}
 *         0 on success, !0 otherwise.
 */
async function exSID_clockselect(clock) {
	if (XS_MODEL_PLUS != hardwareSpecs.model)
		return -1;

	switch (clock) {
		case ClockSelect.XS_CL_PAL:
			xSoutb(XSP_AD_IOCTCP, 1);
			break;
		case ClockSelect.XS_CL_NTSC:
			xSoutb(XSP_AD_IOCTCN, 1);
			break;
		case ClockSelect.XS_CL_1MHZ:
			xSoutb(XSP_AD_IOCTC1, 1);
			break;
		default:
			return -1;
	}
	await delay(1);

	clkdrift = 0; // reset drift

	return 0;
}

/**
 * exSID+ audio operations routine. Selects the audio mixing / muting option.
 * Only implemented in exSID+ devices.
 * 
 * <B>Warning:</B> all these operations (excepting unmuting obviously) will mute
 * the output by default. <B>Note:</B> no accounting for SID cycles consumed.
 * 
 * @param {Object} operation
 *         audio operation value
 * @return {number}
 *         0 on success, !0 otherwise.
 */
async function exSID_audio_op(operation) {
	if (XS_MODEL_PLUS != hardwareSpecs.model)
		return -1;

	switch (operation) {
		case AudioOp.XS_AU_6581_8580:
			await xSoutb(XSP_AD_IOCTA0, 0);
			break;
		case AudioOp.XS_AU_8580_6581:
			await xSoutb(XSP_AD_IOCTA1, 0);
			break;
		case AudioOp.XS_AU_8580_8580:
			await xSoutb(XSP_AD_IOCTA2, 0);
			break;
		case AudioOp.XS_AU_6581_6581:
			await xSoutb(XSP_AD_IOCTA3, 0);
			break;
		case AudioOp.XS_AU_MUTE:
			await xSoutb(XSP_AD_IOCTAM, 0);
			break;
		case AudioOp.XS_AU_UNMUTE:
			await xSoutb(XSP_AD_IOCTAU, 0);
			break;
		default:
			return -1;
	}

	return 0;
}

/**
 * SID chipselect routine. Selects which SID will play the tunes. If neither
 * CHIP0 or CHIP1 is chosen, both SIDs will operate together. Accounts for
 * elapsed cycles.
 * 
 * @param {Object} chip
 *         SID selector value
 */
async function exSID_chipselect(chip) {
	clkdrift -= hardwareSpecs.csioctlCycles;
	switch (chip) {
		case ChipSelect.XS_CS_CHIP0:
			await xSoutb(XS_AD_IOCTS0, 0);
			break;
		case ChipSelect.XS_CS_CHIP1:
			await xSoutb(XS_AD_IOCTS1, 0);
			break;
		default:
			await xSoutb(ChipSelect.XS_AD_IOCTSB, 0);
			break;
	}
}

/**
 * Device hardware model. Queries the driver for the hardware model currently
 * identified.
 * 
 * @return {Object}
 *         hardware model, negative value on error.
 */
function exSID_hwmodel() {
	switch (hardwareSpecs.model) {
		case XS_MODEL_STD:
			return HardwareModel.XS_MD_STD.hardwareModel;
		case XS_MODEL_PLUS:
			return HardwareModel.XS_MD_PLUS.hardwareModel;
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
 * @return {Object}
 *         version information as described above.
 */
async function exSID_hwversion() {
	await xSoutb(XS_AD_IOCTHV, 0);
	await xSoutb(XS_AD_IOCTFV, 1);

	inbuf = new Uint8Array(2);
	await xSread(inbuf, 2);

	// ensure proper order regardless of endianness
	return (inbuf[0] << 8 | inbuf[1]);
}

/**
 * Private busy delay loop.
 * 
 * <B>Note:</B> will block every time a device write is triggered, blocking time
 * will be equal to the number of bytes written times mindelCycles.
 * 
 * @param {number} cycles
 *         how many SID clocks to loop for.
 */
async function xSdelay(cycles) {
	while (cycles >= hardwareSpecs.mindelCycles) {
		await xSoutb(XS_AD_IOCTD1, 0);
		cycles -= hardwareSpecs.mindelCycles;
		clkdrift -= hardwareSpecs.mindelCycles;
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
 * @param {number} cycles
 *         how many SID clocks to wait for.
 */
async function xSlongdelay(cycles) {
	var multiple;
	var flush;
	var delta;
	var dummy = new byte[1];

	flush = (XS_MODEL_STD == hardwareSpecsmodel) ? 1 : 0;

	multiple = cycles - hardwareSpecs.ldelayOffs;
	delta = multiple % XS_LDMULT;
	multiple /= XS_LDMULT;

	if (multiple < 0) {
		return;
	}

	while (multiple >= 255) {
		await exSID_write(XS_AD_IOCTLD, 255, flush);
		if (flush != 0)
			// wait for answer with blocking read
			xSread(dummy, 1);
		multiple -= 255;
	}

	if (multiple != 0) {
		await exSID_write(XS_AD_IOCTLD, multiple, flush);
		if (flush != 0)
			// wait for answer with blocking read
			xSread(dummy, 1);
	}

	// deal with remainder
	await xSdelay(delta);
}

/**
 * Cycle accurate delay routine. Applies the most efficient strategy to delay
 * for cycles SID clocks while leaving enough lead time for an I/O operation.
 * 
 * @param {number} cycles
 *         how many SID clocks to loop for.
 */
async function exSID_delay(cycles) {
	var delay;

	clkdrift += cycles;

	// never delay for less than a full write would need
	if (clkdrift <= hardwareSpecs.writeCycles)
		// too short
		return;

	delay = clkdrift - hardwareSpecs.writeCycles;

	switch (hardwareSpecs.model) {
		// currently breaks sidplayfp - REVIEW
		//	case XS_MODEL_PLUS:
		//		if (delay > XS_LDMULT) {
		//			xSlongdelay(delay);
		//			break;
		//		}
		default:
			await xSdelay(delay);
	}
}

/**
 * Private write routine for a tuple address + data.
 * 
 * @param {number} addr
 *         target address to write to.
 * @param {number} data
 *         data to write at that address.
 * @param {boolean} flush
 *         if non-zero, force immediate flush to device.
 */
async function exSID_write(addr, data, flush) {
	await xSoutb(addr, 0);
	await xSoutb(data, flush);
}

/**
 * Timed write routine, attempts cycle-accurate writes. This function will be
 * cycle-accurate provided that no two consecutive reads or writes are less than
 * writeCycles apart and the leftover delay is &lt;= maxAdj SID clock cycles.
 * 
 * @param {number} cycles
 *         how many SID clocks to wait before the actual data write.
 * @param {number} addr
 *         target address.
 * @param {number} data
 *         data to write at that address.
 */
async function exSID_clkdwrite(cycles, addr, data) {
	// actual write will cost writeCycles. Delay for cycles - write_cycles then
	// account for the write
	clkdrift += cycles;
	if (clkdrift > hardwareSpecs.writeCycles)
		xSdelay(clkdrift - hardwareSpecs.writeCycles);

	// write is going to consume write_cycles clock ticks
	clkdrift -= hardwareSpecs.writeCycles;

	/*
	 * if we are still going to be early, delay actual write by up to XS_MAXAD ticks
	 * At this point it is guaranted that clkdrift will be < mindelCycles.
	 */
	if (clkdrift >= 0) {
		adj = clkdrift % (hardwareSpecs.maxAdj + 1);
		/*
		 * if max_adj is >= clkdrift, modulo will give the same results as the correct
		 * test: adj = (clkdrift < max_adj ? clkdrift : maxAdj) but without an extra
		 * conditional branch. If is is < maxAdj, then it seems to provide better
		 * results by evening jitter accross writes. So it's the preferred solution for
		 * all cases.
		 */
		// final delay encoded in top 3 bits of address
		addr = (addr | (adj << 5));
	}

	exSID_write(addr, data, 0);
}

/**
 * Private read routine for a given address.
 * 
 * @param {number} addr
 *          target address to read from.
 * @param {boolean} flush
 *         if non-zero, force immediate flush to device.
 * @return {byte}
 *         data read from address.
 */
async function exSID_read(addr, flush) {
	data = new byte[1];

	// XXX read support
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
 * @param {number} cycles
 *         how many SID clocks to wait before the actual data read.
 * @param {number} addr
 *         target address.
 * @return {byte}
 *         data read from address.
 */
async function exSID_clkdread(cycles, addr) {
	// actual read will happen after read_pre_cycles. Delay for cycles -
	// read_pre_cycles then account for the read
	// 2-cycle offset adjustement, see function documentation.
	clkdrift += hardwareSpecs.readOffsetCycles;
	clkdrift += cycles;
	if (clkdrift > hardwareSpecs.readPreCycles)
		xSdelay(clkdrift - hardwareSpecs.readPreCycles);

	// read request is going to consume read_pre_cycles clock ticks
	clkdrift -= hardwareSpecs.readPreCycles;

	// if we are still going to be early, delay actual read by up to max_adj ticks
	if (clkdrift >= 0) {
		// see clkdwrite()
		adj = clkdrift % (hardwareSpecs.maxAdj + 1);
		addr = (addr | (adj << 5)); // final delay encoded in top 3 bits of address
	}

	// after read has completed, at least another read_post_cycles will have been
	// spent
	clkdrift -= hardwareSpecs.readPostCycles;

	return exSID_read(addr, 1);
}

/**
 * Setup FTDI chip to match exSID firmware. Defaults to 8N1, no flow control.
 * 
 * @param {number} baudrate
 *         Target baudrate.
 * @param {number} latency
 *         Target latency
 */
async function xSfw_usb_setup(baudrate, latency) {
//	device.setBaudRate(baudrate);
//	device.setDataCharacteristics(WordLength.BITS_8, StopBits.STOP_BITS_1, Parity.PARITY_NONE);
//	device.setFlowControl(FlowControl.FLOW_NONE);
//	device.setLatencyTimer(latency);
}

async function xSfw_usb_purge_buffers() {
  console.log('Setting RESET');
  await ftdi.device.controlTransferOut({
      requestType: 'vendor',
      recipient: 'device',
      request: 0,
      value: 0x0000,
      index: 0,
  });
}

async function xSfw_usb_close() {
	await device.ftdi.closeAsync();
}


/*
* == BSD2 LICENSE ==
* Copyright (c) 2020, Tidepool Project
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the associated License, which is identical to the BSD 2-Clause
* License as published by the Open Source Initiative at opensource.org.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the License for more details.
*
* You should have received a copy of the License along with this program; if
* not, you can obtain one from Tidepool Project at tidepool.org.
* == BSD2 LICENSE ==
*/

const H_CLK = 120000000;
const C_CLK = 48000000;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

function FTDIToClkbits(baud, clk, clkDiv) {
    const fracCode = [0, 3, 2, 4, 1, 5, 6, 7];
    let bestBaud = 0;
    let divisor;
    let bestDivisor;
    let encodedDivisor;

    if (baud >= clk / clkDiv) {
        encodedDivisor = 0;
        bestBaud = clk / dlkDiv;
    } else if (baud >= clk / (clkDiv + clkDiv / 2)) {
        encodedDivisor = 1;
        bestBaud = clk / (clkDiv + clkDiv / 2);
    } else if (baud >= clk / (2 * clkDiv)) {
        encodedDivisor = 2;
        bestBaud = clk / (2 * clkDiv);
    } else {
        divisor = clk * 16 / clkDiv / baud;
        if (divisor & 1) {
            bestDivisor = divisor / 2 + 1;
        } else {
            bestDivisor = divisor / 2;
        }

        if (bestDivisor > 0x20000) {
            bestDivisor = 0x1ffff;
        }

        bestBaud = clk * 16 / clkDiv / bestDivisor;

        if (bestBaud & 1) {
            bestBaud = bestBaud / 2 + 1;
        } else {
            bestBaud = bestBaud / 2;
        }

        encodedDivisor = (bestDivisor >> 3) | (fracCode[bestDivisor & 0x7] << 14);
    }

    return [bestBaud, encodedDivisor];
}

function FTDIConvertBaudrate(baud) {
    let bestBaud;
    let encodedDivisor;
    let value;
    let index;

    if (baud <= 0) {
        throw new Error('Baud rate must be > 0');
    }

    [bestBaud, encodedDivisor] = FTDIToClkbits(baud, C_CLK, 16);

    value = encodedDivisor & 0xffff;
    index = encodedDivisor >> 16;

    return [bestBaud, value, index];
}

class FTDI {
  constructor() {
  }
  async init(vendorId, options) {
    const self = this;

      const device = await navigator.usb.requestDevice({
        filters: [
          {
            vendorId,
          }
        ]
      });

      if (device == null) {
        throw new Error('Could not find device');
      }

      await device.open();
      console.log('Opened:', device.opened);

      if (device.configuration === null) {
        console.log('selectConfiguration');
        await device.selectConfiguration(1);
      }
      await device.claimInterface(0);
      await device.selectConfiguration(1);
      await device.selectAlternateInterface(0, 0);

      console.log('Setting Modem control RTS enable');
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 1,
          value: 0x0202,
          index: 0,
      });

      console.log('Setting flow control XON/XOFF to zero');
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 2,
          value: 0x0000,
          index: 0,
      });

      const [baud, value, index] = FTDIConvertBaudrate(options.baudRate);
      console.log('Setting baud rate to', baud);
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 3,
          value ,
          index,
      });

      console.log('Setting modem control DTR enable');
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 1,
          value: 0x0101,
          index: 0,
      });

      console.log('Setting data 8 bit, no parity');
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 4,
          value: 0x0008,
          index: 0,
      });

      console.log('Setting event characteristics');
      await device.controlTransferOut({
          requestType: 'vendor',
          recipient: 'device',
          request: 6,
          value: 0x0000,
          index: 0,
      });
      
      self.device = device;
      self.isClosing = false;
      this.device.transferIn(1, 64); // flush buffer
  }

  async read() {
	let transferred = await this.device.transferIn(1, 64);

	if (transferred.status !== "ok") {
		return;
	}
	return transferred.data;
  }

  async writeAsync(buffer) {
    return await this.device.transferOut(2, buffer);
  }

  async closeAsync() {
    this.isClosing = true;
    try {
      console.log('Sending EOT');

      const result = new Uint8Array(1);
      result[0] = 0x04;
      await this.writeAsync(result);
      await delay(2000); // wait for send/receive to complete
      await this.device.releaseInterface(0);
      await this.device.close();
      console.log('Closed device');
    } catch(err) {
      console.log('Error:', err);
    }
  }

  isOpen() {
  	return this.device.opened;
  }
  
}
