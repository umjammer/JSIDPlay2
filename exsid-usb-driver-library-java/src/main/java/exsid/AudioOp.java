package exsid;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Audio output operations for exSID_audio_op() */
public enum AudioOp {
	/**
	 * mix: 6581 L / 8580 R
	 */
	XS_AU_6581_8580(0),
	/**
	 * mix: 8580 L / 6581 R
	 */
	XS_AU_8580_6581(1),
	/**
	 * mix: 8580 L and R
	 */
	XS_AU_8580_8580(2),
	/**
	 * mix: 6581 L and R
	 */
	XS_AU_6581_6581(3),
	/**
	 * mute output
	 */
	XS_AU_MUTE(4),
	/**
	 * unmute output
	 */
	XS_AU_UNMUTE(5),;

	private int audioOp;

	private AudioOp(int audioOp) {
		this.audioOp = audioOp;
	}

	public int getAudioOp() {
		return audioOp;
	}

	private static final Map<Integer, AudioOp> lookup = Collections.unmodifiableMap(
			Arrays.asList(AudioOp.values()).stream().collect(Collectors.toMap(AudioOp::getAudioOp, Function.identity())));

	public static AudioOp get(int audioOp) {
		return lookup.get(audioOp);
	}
}