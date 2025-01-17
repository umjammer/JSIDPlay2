package sidplay.audio;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import libsidplay.config.IAudioSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AACDriver.AACFileDriver;
import sidplay.audio.AVIDriver.AVIFileDriver;
import sidplay.audio.FLACDriver.FLACFileDriver;
import sidplay.audio.FLVDriver.FLVFileDriver;
import sidplay.audio.FLVDriver.FLVStreamDriver;
import sidplay.audio.JWAVDriver.JWAVStreamDriver;
import sidplay.audio.MP3Driver.MP3FileDriver;
import sidplay.audio.MP4Driver.MP4FileDriver;
import sidplay.audio.SIDDumpDriver.SIDDumpFileDriver;
import sidplay.audio.SIDRegDriver.SIDRegFileDriver;
import sidplay.audio.WAVDriver.WAVFileDriver;
import sidplay.audio.JWAVDriver.JWAVFileDriver;
import sidplay.audio.JMP3Driver.JMP3FileDriver;

/**
 * Audio output.
 *
 * @author ken
 *
 */
public enum Audio {
	/** Java Sound API. */
	SOUNDCARD(JavaSound.class),
	/** WAV file write. */
	WAV(WAVFileDriver.class),
	/** WAV file write (Java version). */
	JWAV(JWAVFileDriver.class),
	/** FLAC file write. */
	FLAC(FLACFileDriver.class),
	/** AAC file write. */
	AAC(AACFileDriver.class),
	/** MP3 file write. */
	MP3(MP3FileDriver.class),
	/** MP3 file write (Java version). */
	JMP3(JMP3FileDriver.class),
	/** FLV file write */
	FLV(FLVFileDriver.class),
	/** AVI file write. */
	AVI(AVIFileDriver.class),
	/** MP4 file write. */
	MP4(MP4FileDriver.class),
	/** SID register writes file write. */
	SID_REG(SIDRegFileDriver.class),
	/** SID dump file write. */
	SID_DUMP(SIDDumpFileDriver.class),
	/** Java Sound API plus WAV file write. */
	LIVE_WAV(ProxyDriver.class, JavaSound.class, WAVFileDriver.class),
	/** Java Sound API plus WAV file write (java version). */
	LIVE_JWAV(ProxyDriver.class, JavaSound.class, JWAVFileDriver.class),
	/** Java Sound API plus FLAC file write. */
	LIVE_FLAC(ProxyDriver.class, JavaSound.class, FLACFileDriver.class),
	/** Java Sound API plus AAC file write. */
	LIVE_AAC(ProxyDriver.class, JavaSound.class, AACFileDriver.class),
	/** Java Sound API plus MP3 file write. */
	LIVE_MP3(ProxyDriver.class, JavaSound.class, MP3FileDriver.class),
	/** Java Sound API plus MP3 file write (java version). */
	LIVE_JMP3(ProxyDriver.class, JavaSound.class, JMP3FileDriver.class),
	/** Java Sound API plus FLV file write. */
	LIVE_FLV(ProxyDriver.class, JavaSound.class, FLVFileDriver.class),
	/** Java Sound API plus AVI file write. */
	LIVE_AVI(ProxyDriver.class, JavaSound.class, AVIFileDriver.class),
	/** Java Sound API plus MP4 file write. */
	LIVE_MP4(ProxyDriver.class, JavaSound.class, MP4FileDriver.class),
	/** Java Sound API plus FLV live video upload. */
	LIVE_VIDEO_STREAMING(ProxyDriver.class, JavaSound.class, FLVStreamDriver.class),
	/** Java Sound API plus SID register writes file write. */
	LIVE_SID_REG(ProxyDriver.class, JavaSound.class, SIDRegFileDriver.class),
	/** Java Sound API plus SID dump file write. */
	LIVE_SID_DUMP(ProxyDriver.class, JavaSound.class, SIDDumpFileDriver.class),
	/** Java Sound API plus MP3 recording comparison. */
	COMPARE_MP3(CmpToMP3FileDriver.class),
	/** */
	STREAM(JWAVStreamDriver.class);

	private final Class<? extends AudioDriver> audioDriverClass, parameterClasses[];
	private AudioDriver audioDriver;

	/**
	 * Create audio output using the audio driver
	 *
	 * @param audioDriverClass audio driver class
	 * @param parameters       parameters of audio driver class
	 */
	@SafeVarargs
	Audio(Class<? extends AudioDriver> audioDriverClass, Class<? extends AudioDriver>... parameters) {
		this.audioDriverClass = audioDriverClass;
		this.parameterClasses = parameters;
	}

	/**
	 * Get audio driver.
	 *
	 * <B>Note:</B> Audio drivers are instantiated at runtime on demand. We do not
	 * want to load unused libraries like jump3r, if not required!<BR>
	 *
	 * @return audio driver
	 */
	public final AudioDriver getAudioDriver() {
		if (audioDriver == null) {
			audioDriver = newAudioDriver();
		}
		return audioDriver;
	}

	public final AudioDriver newAudioDriver() {
		try {
			Class<?> parameterTypes[] = Stream.<Class<?>>generate(() -> AudioDriver.class)
					.limit(parameterClasses.length).toArray(Class<?>[]::new);
			Collection<Object> initArgs = new ArrayList<>();
			for (Class<? extends AudioDriver> parameterClass : parameterClasses) {
				initArgs.add(Stream.of(values()).map(audio -> audio.audioDriver).filter(parameterClass::isInstance)
						.findFirst().orElse(parameterClass.getConstructor().newInstance()));
			}
			return audioDriverClass.getConstructor(parameterTypes).newInstance(initArgs.toArray());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Audiodriver cannot be instanciated: " + audioDriverClass.getName(), e);
		}
	}

	/**
	 * Get audio driver for tune.<BR>
	 * <B>Note:</B>Use MP3 comparison driver for MP3 play-back.
	 *
	 * @param audioSection configuration
	 * @param tune         SID tune
	 */
	public final AudioDriver getAudioDriver(final IAudioSection audioSection, final SidTune tune) {
		if (tune instanceof MP3Tune) {
			audioSection.setMp3(((MP3Tune) tune).getMP3());
			audioSection.setPlayOriginal(true);
			return COMPARE_MP3.getAudioDriver();
		}
		return getAudioDriver();
	}

	public final static List<Audio> getLiveAudio() {
		return Stream.of(values()).filter(audio -> asList(JavaSound.class, ProxyDriver.class, CmpToMP3FileDriver.class)
				.stream().anyMatch(audio.audioDriverClass::equals)).collect(toList());
	}
}