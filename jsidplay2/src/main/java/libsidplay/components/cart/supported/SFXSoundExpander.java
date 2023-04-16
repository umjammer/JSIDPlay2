package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.common.CPUClock;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.FMOPL_072;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import sidplay.audio.processors.AudioProcessor;

/**
 * 
 * @author ken
 *
 */
public class SFXSoundExpander extends Cartridge implements AudioProcessor {

	private FMOPL_072.FM_OPL opl3;

	public SFXSoundExpander(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		int clock = (int) CPUClock.PAL.getCpuFrequency();
		opl3 = FMOPL_072.init(FMOPL_072.OPL_TYPE_YM3526, clock, 48000);
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		FMOPL_072.reset_chip(opl3);
		pla.setGameExrom(true, true);
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int addr, byte val) {
			addr = (addr & 0xff);
			if (addr == 0x40) {
				FMOPL_072.write(opl3, 0, val & 0xff);
			} else if (addr == 0x50) {
				FMOPL_072.write(opl3, 1, val & 0xff);
			}
		}
	};

	@Override
	public void process(ByteBuffer sampleBuffer) {
		int len = sampleBuffer.position();
		((Buffer) sampleBuffer).flip();
		ByteBuffer buffer = ByteBuffer.wrap(new byte[len]).order(sampleBuffer.order());

		boolean isLeft = false;
		int[] shortsLeft = new int[1];
		for (int i = 0; i < len >> 1; i++) {
			if (!isLeft) {
				FMOPL_072.update_one(opl3, shortsLeft, 1);
			}
			int outputSample = shortsLeft[0] + sampleBuffer.getShort();
			isLeft ^= true;

			buffer.putShort((short) outputSample);
		}
		((Buffer) sampleBuffer).flip();
		((Buffer) buffer).flip();
		sampleBuffer.put(buffer);
	}

	@Override
	public boolean isMultiPurpose() {
		return false;
	}
}
