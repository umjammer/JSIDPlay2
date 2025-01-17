package sidplay.audio.processors.delay;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.processors.AudioProcessor;

public class DelayProcessor implements AudioProcessor {

	private short[] delayBuffer;
	private int readIndex, writeIndex, delayBufferSize, delayOffset;

	private Integer delayInMs;

	private IAudioSection audioSection;

	public DelayProcessor(IConfig config) {
		this.audioSection = config.getAudioSection();
	}

	@Override
	public void process(ByteBuffer sampleBuffer) {
		if (!audioSection.getDelayBypass() && audioSection.getDelay() > 0) {
			if (delayInMs == null || delayInMs != audioSection.getDelay()) {
				delayInMs = audioSection.getDelay();

				delayOffset = delayInMs * audioSection.getSamplingRate().getFrequency() * 2 / 1000;
				delayBufferSize = (sampleBuffer.capacity() >> 1) + delayOffset;
				delayBuffer = new short[delayBufferSize];
				writeIndex = 0;
				readIndex = sampleBuffer.capacity() >> 1;
			}
			int len = sampleBuffer.position();
			((Buffer) sampleBuffer).flip();
			ByteBuffer buffer = ByteBuffer.wrap(new byte[len]).order(sampleBuffer.order());

			for (int i = 0; i < len >> 1; i++) {
				int inputSample = sampleBuffer.getShort();
				int delaySample = delayBuffer[readIndex++];
				int outputSample = inputSample * audioSection.getDelayDryLevel() / 100
						+ delaySample * audioSection.getDelayWetLevel() / 100;

				outputSample = Math.max(Math.min(outputSample, Short.MAX_VALUE), Short.MIN_VALUE);

				buffer.putShort((short) outputSample);

				inputSample += delaySample * audioSection.getDelayFeedbackLevel() / 100;

				inputSample = Math.max(Math.min(inputSample, Short.MAX_VALUE), Short.MIN_VALUE);

				delayBuffer[writeIndex++] = (short) inputSample;

				if (readIndex == delayBufferSize) {
					readIndex = 0;
				}
				if (writeIndex == delayBufferSize) {
					writeIndex = 0;
				}
			}
			((Buffer) sampleBuffer).flip();
			((Buffer) buffer).flip();
			sampleBuffer.put(buffer);
		}
	}

}
