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
	XS_MD_PLUS: 1,
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
	XS_AU_UNMUTE: 5,
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
	XS_CS_BOTH: 2,
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
	XS_CL_1MHZ: 2,
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
const XS_RSBCLK = XS_BDRATE / 10;
/**
 * SID cycles between two consecutive chars
 */
const XS_CYCCHR = XS_SIDCLK / XS_RSBCLK;
/**
 * FTDI latency: 2-255ms in 1ms increments
 */
const XS_USBLAT = 2;
/**
 * Must be multiple of _62_ or USB won't be happy.
 */
const XS_BUFFSZ = (((XS_RSBCLK / 1000) * XS_BUFFMS) / 62) * 62;
/**
 * long delay SID cycle loop multiplier
 */
const XS_LDMULT = 501;
/**
 * Smallest possible delay (with IOCTD1).
 */
const XS_MINDEL = XS_CYCCHR;
/**
 * minimum cycles between two consecutive I/Os (addr + data)
 */
const XS_CYCIO = 2 * XS_CYCCHR;
/**
 * maximum encodable value for post write clock adjustment: must fit on 3 bits
 */
const XS_MAXADJ = 7;
/**
 * long delay loop SID cycles offset
 */
const XS_LDOFFS = 3 * XS_CYCCHR;

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
const XSP_AD_IOCTCP = 0x3d;
/**
 * Select NTSC clock
 */
const XSP_AD_IOCTCN = 0x3e;
/**
 * Select 1MHz clock
 */
const XSP_AD_IOCTC1 = 0x3f;

/**
 * Audio Mix: 6581 L / 8580 R
 */
const XSP_AD_IOCTA0 = 0x5d;
/**
 * Audio Mix: 8580 L / 6581 R
 */
const XSP_AD_IOCTA1 = 0x5e;
/**
 * Audio Mix: 8580 L / 8580 R
 */
const XSP_AD_IOCTA2 = 0x5f;

/**
 * Audio Mix: 6581 L / 6581 R
 */
const XSP_AD_IOCTA3 = 0x7d;
/**
 * Audio Mute
 */
const XSP_AD_IOCTAM = 0x7e;
/**
 * Audio Unmute
 */
const XSP_AD_IOCTAU = 0x7f;

/**
 * shortest delay (XS_MINDEL SID cycles)
 */
const XS_AD_IOCTD1 = 0x9d;
/**
 * polled delay, amount of SID cycles to wait must be given in data
 */
const XS_AD_IOCTLD = 0x9e;

/**
 * select chip 0
 */
const XS_AD_IOCTS0 = 0xbd;
/**
 * select chip 1
 */
const XS_AD_IOCTS1 = 0xbe;
/**
 * select both chips. @warning Invalid for reads: unknown behaviour!
 */
const XS_AD_IOCTSB = 0xbf;

/**
 * Firmware version query
 */
const XS_AD_IOCTFV = 0xfd;
/**
 * Hardware version query
 */
const XS_AD_IOCTHV = 0xfe;
/**
 * SID reset
 */
const XS_AD_IOCTRS = 0xff;

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
	constructor(
		model,
		writeCycles,
		readPreCycles,
		readPostCycles,
		readOffsetCycles,
		csioctlCycles,
		mindelCycles,
		maxAdj,
		ldelayOffs
	) {
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
	constructor(
		description,
		pid,
		vid,
		model,
		writeCycles,
		readPreCycles,
		readPostCycles,
		readOffsetCycles,
		csioctlCycles,
		mindelCycles,
		maxAdj,
		ldelayOffs
	) {
		this.description = description;
		this.pid = pid;
		this.vid = vid;
		this.hardwareSpecs = new HardwareSpecs(
			model,
			writeCycles,
			readPreCycles,
			readPostCycles,
			readOffsetCycles,
			csioctlCycles,
			mindelCycles,
			maxAdj,
			ldelayOffs
		);
	}
}

/* exSID USB */
var exSID = new SupportedDevices(
	XS_USBDSC,
	XS_USBPID,
	XS_USBVID,
	XS_MODEL_STD,
	XS_CYCIO,
	XS_CYCCHR,
	XS_CYCCHR,
	-2,
	XS_CYCCHR,
	XS_MINDEL,
	XS_MAXADJ,
	XS_LDOFFS
);

/* exSID+ USB */
var exSIDPlus = new SupportedDevices(
	XSP_USBDSC,
	XSP_USBPID,
	XSP_USBVID,
	XS_MODEL_PLUS,
	XSP_CYCIO,
	XSP_PRE_RD,
	XSP_POSTRD,
	0,
	XSP_CYCCS,
	XSP_MINDEL,
	XSP_MAXADJ,
	XSP_LDOFFS
);

var xSsupported = [exSID, exSIDPlus];

var ftdi, clkdrift;

var hardwareSpecs;

var backbuf = new Array(XS_BUFFSZ);
var backbufIdx = 0;

var bufferQueue = new Queue();

async function exSIDthreadOutput() {
	var bufferFrame;
	while (bufferQueue.isNotEmpty()) {
		bufferFrame = bufferQueue.dequeue();
		// exit condition
		if (bufferFrame.bufferIdx < 0) {
			timer = null;
			return;
		}
		await xSwrite(bufferFrame.buffer, bufferFrame.bufferIdx);
	}
	timer = setTimeout(() => exSIDthreadOutput());
}

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
		await ftdi.write(new Uint8Array(buff.slice(0, size)));
	} catch (error) {}
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

	if (backbufIdx < XS_BUFFSZ && flush == 0) return;

	if (flush < 0)
		// indicate exit request
		bufferQueue.enqueue({
			buffer: [...backbuf],
			bufferIdx: -1,
		});
	else {
		bufferQueue.enqueue({
			buffer: [...backbuf],
			bufferIdx: backbufIdx,
		});
		backbufIdx = 0;
	}
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
		await ftdi.init(XS_USBVID);

		for (xSsup of xSsupported) {
			console.log("Trying " + xSsup.description + "...");
			if (ftdi.device.productName === xSsup.description) {
				device.ftdi = ftdi;
				hardwareSpecs = xSsup.hardwareSpecs;
				break;
			}
		}
		if (device === {}) {
			console.log("No device could be opened");
			return -1;
		}

		await xSfw_usb_setup(XS_BDRATE, XS_USBLAT);

		backbufIdx = 0;
		bufferQueue.clear();
		timer = setTimeout(() => exSIDthreadOutput());

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

		await xSoutb(XS_AD_IOCTFV, -1); // signal end of thread

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
	bufferQueue.clear();
	// this will stall
	await xSoutb(XS_AD_IOCTRS, 1);
	// sleep for 100us
	await delay(50); // wait for send/receive to complete
	// this only needs 2 bytes which matches the input buffer of the PIC so all is
	// well
	await exSID_write(0x18, volume, 1);

	clkdrift = 0;
	await delay(250); // wait for send/receive to complete

	backbufIdx = 0;
}

function exSID_is_playing() {
	return bufferQueue.isNotEmpty();
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
	if (XS_MODEL_PLUS != hardwareSpecs.model) return -1;

	switch (clock) {
		case ClockSelect.XS_CL_PAL:
			await xSoutb(XSP_AD_IOCTCP, 1);
			break;
		case ClockSelect.XS_CL_NTSC:
			await xSoutb(XSP_AD_IOCTCN, 1);
			break;
		case ClockSelect.XS_CL_1MHZ:
			await xSoutb(XSP_AD_IOCTC1, 1);
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
	if (XS_MODEL_PLUS != hardwareSpecs.model) return -1;

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
			await xSoutb(XS_AD_IOCTSB, 0);
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
	return (inbuf[0] << 8) | inbuf[1];
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
	var dummy = new byte[1]();

	flush = XS_MODEL_STD == hardwareSpecsmodel ? 1 : 0;

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
			await xSread(dummy, 1);
		multiple -= 255;
	}

	if (multiple != 0) {
		await exSID_write(XS_AD_IOCTLD, multiple, flush);
		if (flush != 0)
			// wait for answer with blocking read
			await xSread(dummy, 1);
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
	if (clkdrift > hardwareSpecs.writeCycles) await xSdelay(clkdrift - hardwareSpecs.writeCycles);

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
		addr = addr | (adj << 5);
	}

	await exSID_write(addr, data, 0);
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
	data = new byte[1]();

	// XXX read support
	await xSoutb(addr, flush);
	// blocking
	await xSread(data, 1);

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
	if (clkdrift > hardwareSpecs.readPreCycles) await xSdelay(clkdrift - hardwareSpecs.readPreCycles);

	// read request is going to consume read_pre_cycles clock ticks
	clkdrift -= hardwareSpecs.readPreCycles;

	// if we are still going to be early, delay actual read by up to max_adj ticks
	if (clkdrift >= 0) {
		// see clkdwrite()
		adj = clkdrift % (hardwareSpecs.maxAdj + 1);
		addr = addr | (adj << 5); // final delay encoded in top 3 bits of address
	}

	// after read has completed, at least another read_post_cycles will have been
	// spent
	clkdrift -= hardwareSpecs.readPostCycles;

	return await exSID_read(addr, 1);
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
	await device.ftdi.ftdi_set_baudrate(baudrate);
	await device.ftdi.ftdi_set_line_property(8, StopBits.STOP_BIT_1, Parity.NONE, Break.BREAK_OFF);
	await device.ftdi.ftdi_setflowctrl_xonxoff(0, 0);
	await device.ftdi.ftdi_set_latency_timer(latency);
}

async function xSfw_usb_purge_buffers() {
	await device.ftdi.ftdi_usb_purge_buffers();
}

async function xSfw_usb_close() {
	await device.ftdi.close();
}

function Queue() {
	var head, tail;
	return Object.freeze({
		enqueue(value) {
			const link = { value, next: undefined };
			tail = head ? (tail.next = link) : (head = link);
		},
		dequeue() {
			if (head) {
				const value = head.value;
				head = head.next;
				return value;
			}
		},
		peek() {
			return head?.value;
		},
		clear() {
			tail = head = undefined;
		},
		isNotEmpty() {
			return head;
		},
	});
}
