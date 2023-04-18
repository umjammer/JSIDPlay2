package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

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
public class SFXSoundExpanderFmOPL extends Cartridge implements AudioProcessor {

	private FMOPL_072.FM_OPL opl3;

	public SFXSoundExpanderFmOPL(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		opl3 = FMOPL_072.init(FMOPL_072.OPL_TYPE_YM3812, 3579545, 48000);
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
		public byte read(int addr) {
			return (byte) ((addr & 0xff) == 0x60 ? FMOPL_072.read(opl3, 0) : 0xff);
		}

		@Override
		public void write(int addr, byte val) {
			FMOPL_072.write(opl3, (addr & 0xff) == 0x40 ? 0 : 1, val & 0xff);
		}
	};

	@Override
	public void process(ByteBuffer sampleBuffer) {
		int len = sampleBuffer.position();
		((Buffer) sampleBuffer).flip();
		ByteBuffer buffer = ByteBuffer.wrap(new byte[len]).order(sampleBuffer.order());

		int[] shortsLeft = new int[len >> 2];
		FMOPL_072.update_one(opl3, shortsLeft, len >> 2);
		for (int i = 0; i < len >> 1; i++) {
			int outputSample = shortsLeft[i >> 1] + sampleBuffer.getShort();

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
