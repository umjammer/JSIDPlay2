/**
 * Implements hardsid.dll api calls Written by Sandor Téli
 * 
 * Javascript port by Ken Händel
 * 
 * @author ken
 */

const USB_INTERFACE = 0x0;

const USB_PACKAGE_SIZE = 512;

const WRITEBUFF_SIZE = USB_PACKAGE_SIZE;
const WRITEBUFF_SIZE_SYNC = 0x800;

const MAX_DEVCOUNT = 4;
const READBUFF_SIZE = 64;
const HW_BUFFBEG = 0x2000;
const HW_BUFFSIZE = 0x2000;
const HW_FILLRATIO = 0x1000;

const VENDOR_ID = 0x6581;
const HS4U_PRODUCT_ID = 0x8580;
const HSUP_PRODUCT_ID = 0x8581;
const HSUNO_PRODUCT_ID = 0x8582;

const SHORTEST_DELAY = 4;

const WState = {
	OK: 1,
	BUSY: 2,
	ERROR: 3,
};

const SysMode = {
	UNDEF: 0,
	SIDPLAY: 1,
	VST: 2,
};

const DevType = {
	UNKNOWN: 0,
	/**
	 * HardSID4U
	 */
	HS4U: 1,
	/**
	 * HardSID UPlay
	 */
	HSUP: 2,
	/**
	 * HardSID Uno
	 */
	HSUNO: 3,
};

var devhandles = new Array(MAX_DEVCOUNT)

var deviceTypes = new Array(MAX_DEVCOUNT);
var writeBuffer = new Array(MAX_DEVCOUNT);
var lastaccsids = new Array(MAX_DEVCOUNT);

var initialized, error, sync, buffChk = true;

var deviceCount,
	bufferSize = WRITEBUFF_SIZE,
	pkgCount,
	playCursor = HW_BUFFBEG,
	circBuffCursor = HW_BUFFBEG;

var sysMode, lastRelaySwitch;

/**
 * Initializes the management library
 * 
 * @param  {boolean} syncmode
 *         synchronous or asynchronous mode
 * @param  {SysMode} sysmode
 *         SIDPLAY or VST
 * @return {boolean}
 *         init was ok or failed
 */
async function hardsid_usb_init(syncmode, sysmode) {
	if (sysmode !== SysMode.SIDPLAY) {
		throw new Error("Only SIDPLAY mode currently supported!");
	}
	if (!syncmode) {
		throw new Error("Only synchronous mode currently supported!");
	}
	let fnd = false;
	sync = syncmode;

	if (sync) {
		bufferSize = WRITEBUFF_SIZE_SYNC;
	} else {
		bufferSize = WRITEBUFF_SIZE;
	}

	if (initialized) {
		await hardsid_usb_close();
	}
	initialized = true;
	error = false;

	deviceCount = 0;
	await  addAllDevices();

	if (!error && deviceCount > 0) {
		sync = true;
		await hardsid_usb_setmode(0, sysmode); // incomplete device number handling...
		sync = syncmode;
	}
	return fnd;
}

/**
 * closes the management library
 */
async function hardsid_usb_close() {
	try {
		if (initialized) {
			// if (!sync)
			// IsoStream(devhandles[0], true);
	
			for (var d = 0; d < deviceCount; d++) {
				let deviceHandle = devhandles[d];
	
				if (deviceHandle === DevType.HSUP || deviceTypes[d] === DevType.HSUNO) {
					// wait 5ms
					while (await hardsid_usb_delay(d, 5000) == WState.BUSY) {
					}
					// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=1)
					while (await hardsid_usb_write_direct(d, 0xf0, 6) == WState.BUSY) {
					}
					// wait 60ms
					while (await hardsid_usb_delay(d, 60000) == WState.BUSY) {
					}
					// switch 5V off (POWER_DIS=1;RESET_DIS=1;MUTE_ENA=1)
					while (await hardsid_usb_write_direct(d, 0xf0, 7) == WState.BUSY) {
					}
					while (await hardsid_usb_flush(d) == WState.BUSY) {
					}
					lastaccsids[d] = 0xff;
				}
				
				await deviceHandle.releaseInterface(USB_INTERFACE);
			    await deviceHandle.close();
			}
			initialized = false;
		}
	} catch (error) {
		console.log(error);
		error = true;
	}
}

/**
 * Returns the number of USB HardSID devices plugged into the computer.
 * 
 * @return {number}
 *         number of USB HardSID devices
 */
function hardsid_usb_getdevcount() {
	if (!initialized || error) {
		return 0;
	}
	return deviceCount;
}

/**
 * Returns the number of detected SID chips on the given device.
 * 
 * @return {number}
 *         number of detected SID chips on the given device
 */
function hardsid_usb_getsidcount(deviceId) {
	if (!initialized || error) {
		return 0;
	}
	switch (deviceTypes[deviceId]) {
		case DevType.HS4U:
			return 4;
		case DevType.HSUP:
			return 2;
		case DevType.HSUNO:
			return 1;
		default:
			return 0;
	}
}

/**
 * Add allcompatible USB devices.
 * 
 * @return {boolean}
 *         init was ok or failed
 */
async function addAllDevices() {
	try {
		await openAllDevices();
	
		if (deviceCount == 0) {
			console.log("No devices");
			error = true;
			return false;
		}
		return true;
	} catch (error) {
			console.log("No devices");
			error = true;
			return false;
	}
}

/**
 * Open USB devices.
 */
async function openAllDevices() {
	device = await navigator.usb.requestDevice({
		filters: [{
			vendorId: VENDOR_ID
		}]
	 })
	if (device !== undefined) {				 
		console.log(`Product name: ${device.productName}, Product Id: ${device.productId}`);

		let devType = getDevType(device);

		await device.open();

		await device.selectConfiguration(1);

		await device.claimInterface(USB_INTERFACE);

		devhandles[deviceCount] = device;
		deviceTypes[deviceCount] = devType;
		lastaccsids[deviceCount] = 0xff;
		writeBuffer[deviceCount] = new Uint8Array();
		deviceCount++;
	}
}

/**
 * Get USB device type.
 * 
 * @param  {Object} device
 *         USB device
 * @return {DevType}
 *         HS4U, HSUP or HSUNO
 */
function getDevType(device) {
	if (device.vendorId === VENDOR_ID) {
		switch (device.productId) {
			case HS4U_PRODUCT_ID:
			default:
				return DevType.HS4U;
			case HSUNO_PRODUCT_ID:
				return DevType.HSUNO;
			case HSUP_PRODUCT_ID:
				return DevType.HSUP;
		}
	}
	return DevType.UNKNOWN;
}

/**
 * Read state of USB device.
 * 
 * @param  {number} deviceId
 *         device ID
 * @return {WState}
 *         state
 */
async function hardsid_usb_readstate(deviceId) {
	if (!initialized || error || !sync) {
		return WState.ERROR;
	}
	let transferred = await devhandles[deviceId].transferIn(1, READBUFF_SIZE);

	if (transferred.status !== "ok") {
		error = true;
		return WState.ERROR;
	}
	// comm_reset_cnt = transferred.data.getUint8(0) & 0xffff;
	// error_shadow = transferred.data.getUint8(1) & 0xffff;
	// error_addr_shadow = transferred.data.getUint8(2) & 0xffff;
	pkgCount = transferred.data.getUint8(24) | (transferred.data.getUint8(25) << 8);
	playCursor = transferred.data.getUint8(26) | (transferred.data.getUint8(27) << 8);
	circBuffCursor = transferred.data.getUint8(28) | (transferred.data.getUint8(29) << 8);
	sysMode = transferred.data.getUint8(30) | (transferred.data.getUint8(31) << 8);
	return WState.OK;
}

/**
 * Sync with USB device.
 * 
 * @param  {number} deviceId
 *         device ID
 * @return {WState}
 *         state
 */
async function hardsid_usb_sync(deviceId) {
	if (!initialized || error || !sync) {
		return WState.ERROR;
	}

	if (await hardsid_usb_readstate(deviceId) != WState.OK) {
		error = true;
		return WState.ERROR;
	} else {
		var freespace;

		if (playCursor < circBuffCursor)
			freespace = playCursor + HW_BUFFSIZE - circBuffCursor;
		else if (playCursor > circBuffCursor)
			freespace = playCursor - circBuffCursor;
		else
			freespace = HW_BUFFSIZE;

		if (freespace < HW_FILLRATIO)
			return WState.BUSY;

		return WState.OK;
	}
}

/**
 * Perform the communication in async or sync mode.
 * 
 * @param  {number} deviceId
 *         device ID
 * @return {WState}
 *         state
 */
async function hardsid_usb_write_internal(deviceId) {
	if (!initialized || error || writeBuffer[deviceId].byteLength == 0) {
		return WState.ERROR;
	}

	let pkgstowrite = parseInt((writeBuffer[deviceId].byteLength - 1) / USB_PACKAGE_SIZE + 1, 10);
	let writesize = pkgstowrite * USB_PACKAGE_SIZE;

	if (!sync) {
		// stream based async Isoch stream feed
		//					DeviceIoControl(devhandles[0],
		//									IOCTL_ISOUSB_FEED_ISO_STREAM,
		//									writebuffs[0],
		//									writesize,
		//									&dummy, //&gpStreamObj, //pointer to stream object initted when stream was started
		//									sizeof( PVOID),
		//									&nBytesWrite,
		//									NULL);//&(ovls[oix]));
	} else {
		// sync mode direct file write

		var buffer = new Uint8Array(writesize);
		for (var i = 0; i < writeBuffer[deviceId].byteLength; i++) {
		    buffer[i] = writeBuffer[deviceId][i];
		}
		writeBuffer[deviceId] = new Uint8Array();
		let transferred = await devhandles[deviceId].transferOut(2, buffer);

		if (transferred.status !== "ok" || transferred.bytesWritten != writesize) {
			throw new Error("Sent error!");
		}
	}
	return WState.OK;
}

/**
 * Schedules a write command.
 * 
 * @param  {number} deviceId
 *         device ID
 * @param {number} reg
 *         register (chip select as high byte)
 * @param {number} data
 *         register value
 * @return {WState}
 *         state
 */
async function hardsid_usb_write_direct(deviceId, reg, data) {
	if (!initialized || error) {
		return WState.ERROR;
	}

	if (sync && (writeBuffer[deviceId].byteLength == (bufferSize - 2))) {
		let ws = await hardsid_usb_sync(deviceId);
		if (ws != WState.OK)
			return ws;
	}
	writeBuffer[deviceId] = concatenate(
		Uint8Array,
		writeBuffer[deviceId],
		Uint8Array.of(data & 0xff, reg & 0xff)
	);
	if (writeBuffer[deviceId].byteLength == bufferSize) {
		return await hardsid_usb_write_internal(deviceId);
	}

	return WState.OK;
}

/**
 * Write to USB device.
 * 
 * @param  {number} deviceId
 *         device ID
 * @param {number} reg
 *         register (chip select as high byte)
 * @param {number} data
 *         register value
 * @return {WState}
 *         state
 */
async function hardsid_usb_write(deviceId, reg, data) {
	try {
		let newsidmask;

		switch (deviceTypes[deviceId]) {
		case DevType.HS4U:
			return await hardsid_usb_write_direct(deviceId, reg, data);
		case DevType.HSUP:
			if ((reg & 0xc0) != 0) {
				// invalid SID number
				return WState.ERROR;
			} else {
				if (lastaccsids[deviceId] != (reg & 0x20)) {
					// writing to a new SID
					lastaccsids[deviceId] = (reg & 0x20);
					if ((reg & 0x20) != 0) {
						newsidmask = 0xc0;
					} else {
						newsidmask = 0xa0;
					}

					while (lastRelaySwitch > 0 && (new Date().getMilliseconds() - lastRelaySwitch) < 250) {
					}
					// timediff = GetTickCount() - lastrelayswitch;
					lastRelaySwitch = new Date().getMilliseconds();

					// runtime = GetTickCount();

					// wait 4usecs (not a real delay, but an init. delay command)
					while (await hardsid_usb_delay(deviceId, 4) == WState.BUSY) {
					}
					// mute on (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=1)
					while (await hardsid_usb_write_direct(deviceId, 0xf0, 6) == WState.BUSY) {
					}
					// wait 60ms
					while (await hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
					}
					// switch 5V off (POWER_DIS=1;RESET_DIS=1;MUTE_ENA=1)
					while (await hardsid_usb_write_direct(deviceId, 0xf0, 7) == WState.BUSY) {
					}
					// wait 30ms
					while (await hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
					}
					// relay switch
					while (await hardsid_usb_write_direct(deviceId, newsidmask, 0) == WState.BUSY) {
					}
					// wait 30ms
					while (await hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
					}
					// turn off the relay
					while (await hardsid_usb_write_direct(deviceId, 0x80, 0) == WState.BUSY) {
					}
					// wait 30ms
					while (await hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
					}
					// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=0;MUTE_ENA=1)
					while (await hardsid_usb_write_direct(deviceId, 0xf0, 4) == WState.BUSY) {
					}
					// wait 60ms
					while (await hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
					}
					// end reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=0)
					while (await hardsid_usb_write_direct(deviceId, 0xf0, 2) == WState.BUSY) {
					}
					// security 10usec wait
					while (await hardsid_usb_delay(deviceId, 10) == WState.BUSY) {
					}

					// send this all down to the hardware
					while (await hardsid_usb_flush(deviceId) == WState.BUSY) {
					}

					/*
					 * timediff = GetTickCount() - runtime; if (timediff>=240) { 
					 * //for breakpoint purposes }
					 */

					// writing to the SID
					return await hardsid_usb_write_direct(deviceId, ((reg & 0x1f) | 0x80), data);
				} else
					// writing to the same SID as last time..
					return await hardsid_usb_write_direct(deviceId, ((reg & 0x1f) | 0x80), data);
			}
		case DevType.HSUNO:
			if (lastaccsids[deviceId] == 0xff) {

				// first write, we need the 5V

				// indicate that we've enabled the 5V
				lastaccsids[deviceId] = 0x01;

				// wait 4usecs (not a real delay, but an init. delay command)
				while (await hardsid_usb_delay(deviceId, 4) == WState.BUSY) {
				}
				// wait 5ms
				while (await hardsid_usb_delay(deviceId, 5000) == WState.BUSY) {
				}
				// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=0;MUTE_ENA=1)
				while (await hardsid_usb_write_direct(deviceId, 0xf0, 4) == WState.BUSY) {
				}
				// wait 60ms
				while (await hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
				}
				// end reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=0)
				while (await hardsid_usb_write_direct(deviceId, 0xf0, 2) == WState.BUSY) {
				}
				// security 10usec wait
				while (await hardsid_usb_delay(deviceId, 10) == WState.BUSY) {
				}

				// send this all down to the hardware
				while (await hardsid_usb_flush(deviceId) == WState.BUSY) {
				}

				// writing to the SID
				return await hardsid_usb_write_direct(deviceId, ((reg & 0x1f) | 0x80), data);
			} else
				// writing to the SID normally..
				return await hardsid_usb_write_direct(deviceId, ((reg & 0x1f) | 0x80), data);
		default:
			return WState.ERROR;
		}
	} catch (error) {
		console.log(error);
		error = true;
		return WState.ERROR;
	}
}

/**
 * Schedules a delay command.
 * 
 * @param  {number} deviceId
 *         device ID
 * @param {number} cycles
 *         cycles to delay
 * @return {WState}
 *         state
 */
async function hardsid_usb_delay(deviceId, cycles) {
	if (!initialized || error) {
		return WState.ERROR;
	}

	if (cycles == 0) {
		// no command for zero delay
	} else if (cycles < 0x100) {
		// short delay
		return await hardsid_usb_write_direct(deviceId, 0xee, (cycles & 0xff)); // short delay command
	} else if ((cycles & 0xff) == 0) {
		// long delay without low order byte
		return await hardsid_usb_write_direct(deviceId, 0xef, (cycles >> 8)); // long delay command
	} else {
		// long delay with low order byte
		if (sync && (writeBuffer[deviceId].byteLength == (bufferSize - 2))) {
			let ws = await hardsid_usb_write_direct(deviceId, 0xff, 0xff);
			if (ws != WState.OK)
				return ws;
		} else if (sync && (writeBuffer[deviceId].byteLength == (bufferSize - 4))) {
			let ws = await hardsid_usb_sync(deviceId);
			if (ws != WState.OK)
				return ws;
		}
		await hardsid_usb_write_direct(deviceId, 0xef, (cycles >> 8)); // long delay command
		await hardsid_usb_write_direct(deviceId, 0xee, (cycles & 0xff)); // short delay command
	}

	return WState.OK;
}

/**
 * Sends a partial package to the hardware.
 * 
 * @param  {number} deviceId
 *         device ID
 * @return {WState}
 *         state
 */
async function hardsid_usb_flush(deviceId) {
	if (!initialized || error) {
		return WState.ERROR;
	}

	if (writeBuffer[deviceId].byteLength > 0) {
		if (sync && buffChk) {
			let ws = await hardsid_usb_sync(deviceId);
			if (ws != WState.OK)
				return ws;
		}
		if ((writeBuffer[deviceId].byteLength % bufferSize) > 0) {
			writeBuffer[deviceId] = concatenate(
				Uint8Array,
				writeBuffer[deviceId],
				Uint8Array.of(0xff, 0xff)
			);
		}
		await hardsid_usb_write_internal(deviceId);
	}

	return WState.OK;
}

/**
 * Aborts the playback ASAP.
 * 
 * @param  {number} deviceId
 *         device ID
 */
async function hardsid_usb_abortplay(deviceId) {
	// fixed: 2010.01.26 - after Wilfred's testcase
	// abort the software buffer anyway
	writeBuffer[deviceId] = new Uint8Array();

	if (!initialized || deviceCount == 0) {
		return;
	}

	if (await hardsid_usb_readstate(deviceId) != WState.OK) {
		error = true;
		return;
	}
	if (pkgCount == 0) {
		return;
	}
	await hardsid_usb_write_direct(deviceId, 0xff, 0xff);
	await hardsid_usb_write_direct(deviceId, 0xff, 0xff);
	await hardsid_usb_write_internal(deviceId);
	while (true) {
		if (await hardsid_usb_readstate(deviceId) != WState.OK) {
			error = true;
			break;
		} else {
			if (pkgCount == 0) {
				break;
			}
		}
	}
}

/**
 * Selects one of the sysmodes on the device.
 * 
 * @param  {number} deviceId
 *         device ID
 * @param  {SysMode} newsysmode
 *         SIDPLAY or VST
 * @return {WState}
 *         state
 */
async function hardsid_usb_setmode(deviceId, newsysmode) {
	if (newsysmode !== SysMode.SIDPLAY) {
		throw new Error("Only SIDPLAY mode currently supported!");
	}
	if (newsysmode === SysMode.VST) {
		error = true;
		return WState.ERROR;
	}
	if (await hardsid_usb_readstate(deviceId) != WState.OK) {
		error = true;
		return WState.ERROR;
	}
	if ((sysMode & 0x0f) == newsysmode) {
		return WState.OK;
	}
	await hardsid_usb_abortplay(deviceId);
	await hardsid_usb_write_direct(deviceId, 0xff, 0xff);
	await hardsid_usb_write_direct(deviceId, 0x00, newsysmode);
	await hardsid_usb_write_internal(deviceId);
	while (true) {
		if (await hardsid_usb_readstate(deviceId) != WState.OK) {
			error = true;
			break;
		} else {
			if (sysMode == (newsysmode | 0x80)) {
				break;
			}
		}
	}
	if (error)
		return WState.ERROR;
	else
		return WState.OK;
}

/**
 * Reset all registers.
 * 
 * @param  {number} deviceId
 *         device ID
 * @param  {SysMode} newsysmode
 *         SIDPLAY or VST
 * @return {WState}
 *         state
 */
async function reset(deviceId, chipNum, volume) {
	for (let reg = 0; reg < 32; reg++) {
		while (await hardsid_usb_write(deviceId, ((chipNum << 5) | reg), 0) == WState.BUSY) {
		}
		while (await hardsid_usb_delay(deviceId, SHORTEST_DELAY) == WState.BUSY) {
		}
	}

	while (await hardsid_usb_write(deviceId, ((chipNum << 5) | 0x18), volume) == WState.BUSY) {
	}
	while (await hardsid_usb_delay(deviceId, SHORTEST_DELAY) == WState.BUSY) {
	}
	await hardsid_usb_flush(deviceId);
}

function concatenate(resultConstructor, ...arrays) {
	let totalLength = 0;
	for (const arr of arrays) {
		totalLength += arr.length;
	}
	const result = new resultConstructor(totalLength);
	let offset = 0;
	for (const arr of arrays) {
		result.set(arr, offset);
		offset += arr.length;
	}
	return result;
}
