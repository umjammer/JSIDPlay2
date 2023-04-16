package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.OPL3;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import sidplay.audio.processors.AudioProcessor;

/**
 * 
 * @author ken
 *
 */
public class SFXSoundExpanderOPL3 extends Cartridge implements AudioProcessor {

	private OPL3 opl3;

	public SFXSoundExpanderOPL3(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(true, true);
		opl3 = new OPL3();
	}

	private final Bank io2Bank = new Bank() {

		private int address;

		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int addr, byte val) {
			addr = (addr & 0xff);
			if (addr == 0x40) {
				this.address = val & 0xff;
			} else if (addr == 0x50) {
				opl3.write(0, this.address, val & 0xff);
			}
		}
	};

	@Override
	public void process(ByteBuffer sampleBuffer) {
		int len = sampleBuffer.position();
		((Buffer) sampleBuffer).flip();
		ByteBuffer buffer = ByteBuffer.wrap(new byte[len]).order(sampleBuffer.order());

		int[] shortsLeft = new int[len];
		opl3.read(shortsLeft, len >> 2);
		for (int i = 0; i < len >> 1; i++) {
			int idx = i >> 1;
			int outputSample = shortsLeft[idx + 0] + shortsLeft[idx + 1] + shortsLeft[idx + 2] + shortsLeft[idx + 3]
					+ sampleBuffer.getShort();

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
