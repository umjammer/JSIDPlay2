package sidplay;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.ini.IniConfig;

public class Test {

	public static void main(String[] args) throws IOException, SidTuneError {
		IConfig config = new IniConfig();
		Player player = new Player(config);
		player.setAudioDriver(new AudioDriver() {

			private ByteBuffer sampleBuffer;

			@Override
			public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock,
					EventScheduler context) throws IOException, LineUnavailableException, InterruptedException {
				AudioConfig cfg = new AudioConfig(audioSection);

				sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
						.order(ByteOrder.LITTLE_ENDIAN);
			}

			@Override
			public void write() throws InterruptedException {
				for (int i = 0; i < sampleBuffer.position(); i++) {
					System.out.printf("%02X, ", sampleBuffer.array()[i]);
				}
				System.out.println();
			}

			@Override
			public void close() {
			}

			@Override
			public boolean isRecording() {
				return false;
			}

			@Override
			public ByteBuffer buffer() {
				return sampleBuffer;
			}
		});
		player.setTune(SidTune.load(new File(args[0])));
		player.startC64();
		player.stopC64(false);
	}

}
