package sidplay.audio.processors.reverb;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import sidplay.audio.processors.AudioProcessor;

public class ReverbProcessor implements AudioProcessor {

	private SchroederReverb reverb;

	private IAudioSection audioSection;

	public ReverbProcessor(IConfig config) {
		this.audioSection = config.getAudioSection();
	}

	@Override
	public void process(ByteBuffer sampleBuffer) {
		if (!audioSection.getReverbBypass()) {
			if (reverb == null) {
				reverb = new SchroederReverb(audioSection.getSamplingRate().getFrequency(), 2, sampleBuffer.capacity());
			}
			if (reverb.comb1.getDelayInMs() != audioSection.getReverbComb1Delay()) {
				reverb.comb1.setDelayInMs(audioSection.getReverbComb1Delay());
			}
			if (reverb.comb2.getDelayInMs() != audioSection.getReverbComb2Delay()) {
				reverb.comb2.setDelayInMs(audioSection.getReverbComb2Delay());
			}
			if (reverb.comb3.getDelayInMs() != audioSection.getReverbComb3Delay()) {
				reverb.comb3.setDelayInMs(audioSection.getReverbComb3Delay());
			}
			if (reverb.comb4.getDelayInMs() != audioSection.getReverbComb4Delay()) {
				reverb.comb4.setDelayInMs(audioSection.getReverbComb4Delay());
			}
			if (reverb.allpass1.getDelayInMs() != audioSection.getReverbAllPass1Delay()) {
				reverb.allpass1.setDelayInMs(audioSection.getReverbAllPass1Delay());
			}
			if (reverb.allpass2.getDelayInMs() != audioSection.getReverbAllPass2Delay()) {
				reverb.allpass2.setDelayInMs(audioSection.getReverbAllPass2Delay());
			}
			if (reverb.comb1.getSustainTimeInMs() != audioSection.getReverbSustainDelay()) {
				reverb.comb1.setSustainTimeInMs(audioSection.getReverbSustainDelay());
				reverb.comb2.setSustainTimeInMs(audioSection.getReverbSustainDelay());
				reverb.comb3.setSustainTimeInMs(audioSection.getReverbSustainDelay());
				reverb.comb4.setSustainTimeInMs(audioSection.getReverbSustainDelay());
			}
			if (reverb.mix != audioSection.getReverbDryWetMix()) {
				reverb.mix = audioSection.getReverbDryWetMix();
			}
			short[] dest = new short[sampleBuffer.position() >> 1];
			((Buffer) sampleBuffer).flip();
			sampleBuffer.asShortBuffer().get(dest);
			int newLen = reverb.doReverb(dest, dest.length);
			sampleBuffer.asShortBuffer().put(dest, 0, newLen);
			((Buffer) sampleBuffer).position(newLen << 1);
		}
	}

}
