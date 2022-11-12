/***************************************************************************
							ftdi.c  -  description
							----------------------
	begin                : Fri Apr 4 2003
	copyright            : (C) 2003-2017 by Intra2net AG and the libftdi developers
	email                : opensource@intra2net.com
 ***************************************************************************
 * 
 * https://www.intra2net.com/en/developer/libftdi/index.php
 * 
 * Javascript port by Ken Händel
 * 
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Lesser General Public License           *
 *   version 2.1 as published by the Free Software Foundation;             *
 *                                                                         *
 ***************************************************************************/

const SIO_RESET = 0; /* Reset the port */
const SIO_RESET_SIO = 0;
const SIO_SET_BAUD_RATE = 3; /* Set baud rate */
const SIO_RESET_PURGE_RX = 1;
const SIO_RESET_PURGE_TX = 2;
const SIO_SET_DATA = 4; /* Set the data characteristics of the port */
const SIO_SET_FLOW_CTRL = 2; /* Set flow control register */
const SIO_XON_XOFF_HS = 0x4 << 8;
const SIO_MODEM_CTRL = 1; /* Set the modem control register */
const SIO_SET_DTR_MASK = 0x1;
const SIO_SET_DTR_HIGH = 1 | (SIO_SET_DTR_MASK << 8);
const SIO_SET_DTR_LOW = 0 | (SIO_SET_DTR_MASK << 8);
const SIO_SET_RTS_MASK = 0x2;
const SIO_SET_RTS_HIGH = 2 | (SIO_SET_RTS_MASK << 8);
const SIO_SET_RTS_LOW = 0 | (SIO_SET_RTS_MASK << 8);

const SIO_RESET_REQUEST = SIO_RESET;
const SIO_SET_BAUDRATE_REQUEST = SIO_SET_BAUD_RATE;
const SIO_SET_DATA_REQUEST = SIO_SET_DATA;
const SIO_SET_FLOW_CTRL_REQUEST = SIO_SET_FLOW_CTRL;
const SIO_SET_MODEM_CTRL_REQUEST = SIO_MODEM_CTRL;

const SIO_SET_EVENT_CHAR_REQUEST = 0x06;
const SIO_SET_ERROR_CHAR_REQUEST = 0x07;
const SIO_SET_LATENCY_TIMER_REQUEST = 0x09;
const SIO_SET_BITMODE_REQUEST = 0x0b;

const Parity = {
	NONE: 0,
	ODD: 1,
	EVEN: 2,
	MARK: 3,
	SPACE: 4,
};

const StopBits = {
	STOP_BIT_1: 0,
	STOP_BIT_15: 1,
	STOP_BIT_2: 2,
};

const Break = {
	BREAK_OFF: 0,
	BREAK_ON: 1,
};

const H_CLK = 120000000;
const C_CLK = 48000000;
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

class FTDI {
	constructor() { }
	async init(vendorId) {
		const self = this;

		const device = await navigator.usb.requestDevice({
			filters: [
				{
					vendorId,
				},
			],
		});

		if (device == null) {
			throw new Error("Could not find device");
		}

		await device.open();
		console.log("Opened:", device.opened);

		if (device.configuration === null) {
			console.log("selectConfiguration");
			await device.selectConfiguration(1);
		}
		await device.claimInterface(0);
		await device.selectConfiguration(1);
		await device.selectAlternateInterface(0, 0);

		self.device = device;
		self.isClosing = false;
		this.device.transferIn(1, 64); // flush buffer
	}

	async ftdi_usb_reset() {
		console.log("ftdi_usb_reset");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_RESET_REQUEST,
			value: SIO_RESET_SIO,
			index: 0,
		});
	}

	async ftdi_usb_purge_rx_buffer() {
		console.log("ftdi_usb_purge_rx_buffer");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_RESET_REQUEST,
			value: SIO_RESET_PURGE_RX,
			index: 0,
		});
	}

	async ftdi_usb_purge_tx_buffer() {
		console.log("ftdi_usb_purge_tx_buffer");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_RESET_REQUEST,
			value: SIO_RESET_PURGE_TX,
			index: 0,
		});
	}

	async ftdi_usb_purge_buffers() {
		await this.ftdi_usb_purge_rx_buffer();
		await this.ftdi_usb_purge_tx_buffer();
	}

	async ftdi_set_baudrate(baud) {
		let bestBaud;
		let encodedDivisor;
		let value;
		let index;

		if (baud <= 0) {
			throw new Error("Baud rate must be > 0");
		}

		[bestBaud, encodedDivisor] = ftdi_to_clkbits(baud, C_CLK, 16);

		value = encodedDivisor & 0xffff;
		index = encodedDivisor >> 16;

		console.log("ftdi_set_baudrate", baud);
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_BAUDRATE_REQUEST,
			value,
			index,
		});

		return [bestBaud, value, index];
	}

	async ftdi_set_line_property(bits, sbit, parity, break_type) {
		var value = bits;

		switch (parity) {
			case Parity.NONE:
				value |= 0x00 << 8;
				break;
			case Parity.ODD:
				value |= 0x01 << 8;
				break;
			case Parity.EVEN:
				value |= 0x02 << 8;
				break;
			case Parity.MARK:
				value |= 0x03 << 8;
				break;
			case Parity.SPACE:
				value |= 0x04 << 8;
				break;
		}

		switch (sbit) {
			case StopBits.STOP_BIT_1:
				value |= 0x00 << 11;
				break;
			case StopBits.STOP_BIT_15:
				value |= 0x01 << 11;
				break;
			case StopBits.STOP_BIT_2:
				value |= 0x02 << 11;
				break;
		}

		switch (break_type) {
			case Break.BREAK_OFF:
				value |= 0x00 << 14;
				break;
			case Break.BREAK_ON:
				value |= 0x01 << 14;
				break;
		}

		console.log("ftdi_set_line_property");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_DATA_REQUEST,
			value: value,
			index: 0,
		});
	}

	async ftdi_set_bitmode(bitmask, mode) {
		var usb_val;
		usb_val = bitmask; // low byte: bitmask
		usb_val |= mode << 8;

		console.log("ftdi_set_bitmode");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_BITMODE_REQUEST,
			value: usb_val,
			index: 0,
		});
	}

	async ftdi_disable_bitbang() {
		console.log("ftdi_disable_bitbang");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_BITMODE_REQUEST,
			value: 0,
			index: 0,
		});
	}

	async ftdi_set_latency_timer(latency) {
		console.log("ftdi_set_latency_timer");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_LATENCY_TIMER_REQUEST,
			value: latency,
			index: 0,
		});
	}

	async ftdi_setflowctrl(flowctrl) {
		console.log("ftdi_setflowctrl");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_FLOW_CTRL_REQUEST,
			value: 0,
			index: flowctrl,
		});
	}

	async ftdi_setflowctrl_xonxoff(xon, xoff) {
		var xonxoff = xon | (xoff << 8);
		console.log("ftdi_setflowctrl_xonxoff");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_FLOW_CTRL_REQUEST,
			value: xonxoff,
			index: SIO_XON_XOFF_HS,
		});
	}

	async ftdi_setdtr(state) {
		var usb_val;
		if (state) usb_val = SIO_SET_DTR_HIGH;
		else usb_val = SIO_SET_DTR_LOW;

		console.log("ftdi_setdtr");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_MODEM_CTRL_REQUEST,
			value: usb_val,
			index: 0,
		});
	}

	async ftdi_setdtr_rts(dtr, rts) {
		var usb_val;
		if (dtr) usb_val = SIO_SET_DTR_HIGH;
		else usb_val = SIO_SET_DTR_LOW;

		if (rts) usb_val |= SIO_SET_RTS_HIGH;
		else usb_val |= SIO_SET_RTS_LOW;

		console.log("ftdi_setdtr_rts");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_MODEM_CTRL_REQUEST,
			value: usb_val,
			index: 0,
		});
	}
	async ftdi_set_event_char(eventch, enable) {
		var usb_val;
		usb_val = eventch;
		if (enable) usb_val |= 1 << 8;

		console.log("ftdi_set_event_char");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_EVENT_CHAR_REQUEST,
			value: usb_val,
			index: 0,
		});
	}

	async ftdi_set_error_char(errorch, enable) {
		var usb_val;
		usb_val = errorch;
		if (enable) usb_val |= 1 << 8;

		console.log("ftdi_set_error_char");
		await this.device.controlTransferOut({
			requestType: "vendor",
			recipient: "device",
			request: SIO_SET_ERROR_CHAR_REQUEST,
			value: usb_val,
			index: 0,
		});
	}

	async read() {
		let transferred = await this.device.transferIn(1, 64);

		if (transferred.status !== "ok") {
			return;
		}
		return transferred.data;
	}

	async write(buffer) {
		return await this.device.transferOut(2, buffer);
	}

	async close() {
		this.isClosing = true;
		try {
			console.log("Sending EOT");

			const result = new Uint8Array(1);
			result[0] = 0x04;
			await this.write(result);
			await delay(1000); // wait for send/receive to complete
			await this.device.releaseInterface(0);
			await this.device.close();
			console.log("Closed device");
		} catch (err) {
			console.log("Error:", err);
		}
	}

	isOpen() {
		return this.device.opened;
	}
}

function ftdi_to_clkbits(baud, clk, clkDiv) {
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
		divisor = (clk * 16) / clkDiv / baud;
		if (divisor & 1) {
			bestDivisor = divisor / 2 + 1;
		} else {
			bestDivisor = divisor / 2;
		}

		if (bestDivisor > 0x20000) {
			bestDivisor = 0x1ffff;
		}

		bestBaud = (clk * 16) / clkDiv / bestDivisor;

		if (bestBaud & 1) {
			bestBaud = bestBaud / 2 + 1;
		} else {
			bestBaud = bestBaud / 2;
		}

		encodedDivisor = (bestDivisor >> 3) | (fracCode[bestDivisor & 0x7] << 14);
	}

	return [bestBaud, encodedDivisor];
}
