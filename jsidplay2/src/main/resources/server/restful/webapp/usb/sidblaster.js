/**
 * 
 * A simple I/O library for SIDBlaster USB created by Stein Pedersen.
 * SIDBlaster-USB is based on a device created by Davey (Das Phantom)
 *
 * (C) 2021 Andreas Schumm
 * License: GPLv3 - https://www.gnu.org/licenses/gpl-3.0.html
 *
 * http://crazy-midi.de
 * 
 * Javascript port by Ken Händel
 * 
 * This driver will control the first SIDBlaster device available.
 * 
 * @author ken
 *
 */

/**
 * VID
 */
const SB_USBVID = 0x0403;
const SB_USBDESC = "SIDBlaster/USB";
const SB_USBDESC_6581 = "SIDBlaster/USB/6581";
const SB_USBDESC_8580 = "SIDBlaster/USB/8580";
const SB_BDRATE = 500000;
/**
 * Firmware version query
 */
const SB_AD_IOCTFV = 0xfd;
/**
 * write buffer size in milliseconds of playback.
 */
const SB_BUFFMS = 40;
/**
 * RS232 byte clock. Each RS232 byte is 10 bits long due to start and stop bits
 */
const SB_RSBCLK = SB_BDRATE / 10;
/**
 * Must be multiple of _62_ or USB won't be happy.
 */
const SB_BUFFSZ = (((SB_RSBCLK / 1000) * SB_BUFFMS) / 62) * 62;

var deviceType;

var bufferQueue = new Queue();

var backbuf = new Array(SB_BUFFSZ);
var backbufIdx = 0;

async function sidBlasterThreadOutput() {
	var bufferFrame;
	while (bufferQueue.isNotEmpty()) {
		bufferFrame = bufferQueue.dequeue();
		// exit condition
		if (bufferFrame.bufferIdx < 0) {
			timer = null;
			return;
		}
		// TODO how to delay cycles in us here !?
		if (bufferFrame.cycles && bufferFrame.cycles/1000 > 0) {
//			await realwrite(Uint8Array.of(reg | 0x60, data), 2);
			await delay(bufferFrame.cycles/1000);

		}
		await realwrite(bufferFrame.buffer, bufferFrame.bufferIdx);
	}
	timer = setTimeout(() => sidBlasterThreadOutput());
}

async function realwrite(buff, size) {
	try {
		await ftdi.write(new Uint8Array(buff.slice(0, size)));
	} catch (error) {}
}

async function sidblaster_init() {
	if (ftdi && ftdi.device && ftdi.isOpen()) {
		console.log("Device is already open!");
		return -1;
	}

	try {
		/* Attempt to open all supported devices until first success. */
		device = {};
		deviceType = undefined;
		ftdi = new FTDI();
		await ftdi.init(SB_USBVID);
		device.ftdi = ftdi;
		if (!device.ftdi.device.productName.startsWith(SB_USBDESC)) {
			device = {};
			console.log("Wrong device description, expected: " + SB_USBDESC);
			return -1;
		}
		if (device.ftdi.device.productName == SB_USBDESC_6581) {
			deviceType = "6581";
		}
		if (device.ftdi.device.productName == SB_USBDESC_8580) {
			deviceType = "8580";
		}
		await device.ftdi.ftdi_set_baudrate(500000);
		await device.ftdi.ftdi_set_line_property(8, StopBits.STOP_BIT_1, Parity.NONE, Break.BREAK_OFF);
		await device.ftdi.ftdi_setflowctrl_xonxoff(0, 0);
		await device.ftdi.ftdi_set_latency_timer(2);

		console.log("deviceType: " + deviceType);

		backbufIdx = 0;
		bufferQueue.clear();
		timer = setTimeout(() => sidBlasterThreadOutput());
	} catch (err) {
		console.log(err);
		return -1;
	}
}

async function sidblaster_exit() {
	xSoutb(SB_AD_IOCTFV, -1); // signal end of thread
	await device.ftdi.close();
	device = undefined;
	ftdi = undefined;
}

function sidblaster_reset() {
	bufferQueue.clear();

	bufferQueue.enqueue({
		buffer: Uint8Array.of(0xe0, 0, 0xe1, 0, 0xe7, 0, 0xe8, 0, 0xee, 0, 0xef, 0),
		bufferIdx: 12,
	});
	backbufIdx = 0;
}

function sidblaster_write(cycles, reg, data) {
	backbuf[backbufIdx++] = reg | 0xe0;
	backbuf[backbufIdx++] = data;
	bufferQueue.enqueue({
		buffer: [...backbuf],
		bufferIdx: backbufIdx,
		cycles: cycles
	});
	backbufIdx = 0;
}

function sBwrite(addr, data, flush) {
	sBoutb(addr, 0);
	sBoutb(data, flush);
}

function sBoutb(b, flush) {
	backbuf[backbufIdx++] = b;

	if (backbufIdx < SB_BUFFSZ && flush == 0) return;

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

function sidblaster_is_playing() {
	return bufferQueue.isNotEmpty();
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
