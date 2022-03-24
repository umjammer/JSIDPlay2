package exsid;

public interface IExSID {

	//
	// exSID hardware definitions
	//

	/**
	 * 2Mpbs
	 */
	int XS_BDRATE = 2000000;
	/**
	 * write buffer size in milliseconds of playback.
	 */
	int XS_BUFFMS = 40;
	/**
	 * 1MHz (for computation only, currently hardcoded in firmware)
	 */
	int XS_SIDCLK = 1000000;
	/**
	 * RS232 byte clock. Each RS232 byte is 10 bits long due to start and stop bits
	 */
	int XS_RSBCLK = (XS_BDRATE / 10);
	/**
	 * SID cycles between two consecutive chars
	 */
	int XS_CYCCHR = (XS_SIDCLK / XS_RSBCLK);
	/**
	 * FTDI latency: 1-255ms in 1ms increments
	 */
	short XS_USBLAT = 1;
	/**
	 * Must be multiple of _62_ or USB won't be happy.
	 */
	int XS_BUFFSZ = ((((XS_RSBCLK / 1000) * XS_BUFFMS) / 62) * 62);
	/**
	 * long delay SID cycle loop multiplier
	 */
	int XS_LDMULT = 501;
	/**
	 * Smallest possible delay (with IOCTD1).
	 */
	int XS_MINDEL = (XS_CYCCHR);
	/**
	 * minimum cycles between two consecutive I/Os (addr + data)
	 */
	int XS_CYCIO = (2 * XS_CYCCHR);
	/**
	 * maximum encodable value for post write clock adjustment: must fit on 3 bits
	 */
	int XS_MAXADJ = 7;
	/**
	 * long delay loop SID cycles offset
	 */
	int XS_LDOFFS = (3 * XS_CYCCHR);

	//
	// exSID+ hardware definitions
	//

	/**
	 * Smallest possible delay (with IOCTD1).
	 */
	int XSP_MINDEL = 2;
	/**
	 * minimum cycles between two consecutive I/Os (addr + data)
	 */
	int XSP_CYCIO = 3;
	int XSP_PRE_RD = 2;
	int XSP_POSTRD = 2;
	/**
	 * maximum encodable value for post write clock adjustment: must fit on 3 bits
	 */
	int XSP_MAXADJ = 4;
	/**
	 * long delay loop SID cycles offset
	 */
	int XSP_LDOFFS = 3;
	/**
	 * cycles lost in chipselect()
	 */
	int XSP_CYCCS = 2;

	// IOCTLS
	// IO controls 0x3D to 0x7F are only implemented on exSID+

	/**
	 * Select PAL clock
	 */
	byte XSP_AD_IOCTCP = 0x3D;
	/**
	 * Select NTSC clock
	 */
	byte XSP_AD_IOCTCN = 0x3E;
	/**
	 * Select 1MHz clock
	 */
	byte XSP_AD_IOCTC1 = 0x3F;

	/**
	 * Audio Mix: 6581 L / 8580 R
	 */
	byte XSP_AD_IOCTA0 = 0x5D;
	/**
	 * Audio Mix: 8580 L / 6581 R
	 */
	byte XSP_AD_IOCTA1 = 0x5E;
	/**
	 * Audio Mix: 8580 L / 8580 R
	 */
	byte XSP_AD_IOCTA2 = 0x5F;

	/**
	 * Audio Mix: 6581 L / 6581 R
	 */
	byte XSP_AD_IOCTA3 = 0x7D;
	/**
	 * Audio Mute
	 */
	byte XSP_AD_IOCTAM = 0x7E;
	/**
	 * Audio Unmute
	 */
	byte XSP_AD_IOCTAU = 0x7F;

	/**
	 * shortest delay (XS_MINDEL SID cycles)
	 */
	byte XS_AD_IOCTD1 = (byte) 0x9D;
	/**
	 * polled delay, amount of SID cycles to wait must be given in data
	 */
	byte XS_AD_IOCTLD = (byte) 0x9E;

	/**
	 * select chip 0
	 */
	byte XS_AD_IOCTS0 = (byte) 0xBD;
	/**
	 * select chip 1
	 */
	byte XS_AD_IOCTS1 = (byte) 0xBE;
	/**
	 * select both chips. @warning Invalid for reads: unknown behaviour!
	 */
	byte XS_AD_IOCTSB = (byte) 0xBF;

	/**
	 * Firmware version query
	 */
	byte XS_AD_IOCTFV = (byte) 0xFD;
	/**
	 * Hardware version query
	 */
	byte XS_AD_IOCTHV = (byte) 0xFE;
	/**
	 * SID reset
	 */
	byte XS_AD_IOCTRS = (byte) 0xFF;

	/**
	 * Default FTDI VID
	 */
	int XS_USBVID = 0x0403;
	/**
	 * Default FTDI PID
	 */
	int XS_USBPID = 0x6001;
	String XS_USBDSC = "exSID USB";

	/**
	 * Default FTDI VID
	 */
	int XSP_USBVID = 0x0403;
	/**
	 * Default FTDI PID
	 */
	int XSP_USBPID = 0x6015;
	String XSP_USBDSC = "exSID+ USB";

	int XS_MODEL_STD = 0;
	int XS_MODEL_PLUS = 1;

	SupportedDevices xSsupported[] = new SupportedDevices[] {

			/* exSID USB */
			new SupportedDevices(XS_USBDSC, XS_USBPID, XS_USBVID, XS_MODEL_STD, XS_CYCIO, XS_CYCCHR, XS_CYCCHR, -2,
					XS_CYCCHR, XS_MINDEL, XS_MAXADJ, XS_LDOFFS),

			/* exSID+ USB */
			new SupportedDevices(XSP_USBDSC, XSP_USBPID, XSP_USBVID, XS_MODEL_PLUS, XSP_CYCIO, XSP_PRE_RD, XSP_POSTRD,
					0, XSP_CYCCS, XSP_MINDEL, XSP_MAXADJ, XSP_LDOFFS) };

	int exSID_init();

	void exSID_exit();

	void exSID_reset(byte volume);

	int exSID_hwmodel();

	short exSID_hwversion();

	int exSID_clockselect(int clock);

	int exSID_audio_op(int operation);

	void exSID_chipselect(int chip);

	void exSID_delay(long cycles);

	void exSID_clkdwrite(long cycles, byte addr, byte data);

	byte exSID_clkdread(long cycles, byte addr);

	String exSID_error_str();

}
