package libsidutils.fingerprinting.fingerprint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.common.SamplingRate;
import libsidutils.AudioUtils;
import libsidutils.fingerprinting.ini.IFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.WAVBean;

/**
 * Created by hsyecheng on 2015/6/13. Generate Fingerprints. The sampling rate
 * of the sample data should be 8000.
 */
public class FingerprintCreator {

	public Fingerprint createFingerprint(IFingerprintConfig config, WAVBean wavBean) throws IOException {
		try (InputStream is = new ByteArrayInputStream(wavBean.getWav())) {
			short[] samples = new AudioUtils().convertToMonoAndRate(is, wavBean.getFrameMaxLength(),
					SamplingRate.VERY_LOW);
			float[] data = new float[samples.length];
			for (int i = 0; i < samples.length; i++) {
				data[i] = samples[i] / 32768f;
			}
			return new Fingerprint(config, data);
		} catch (UnsupportedAudioFileException | IOException e) {
			throw new IOException(e);
		}
	}

}
