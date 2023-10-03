package server.restful.common.text2speech;

import java.util.Iterator;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidplay.sidtune.SidTuneInfo;
import net.gcardone.junidecode.Junidecode;

public enum TextToSpeechType {
	NONE(TextToSpeechType::createNoArgumentsFunction), ESPEAK(TextToSpeechType::createEspeakArgumentsFunction);

	private BiFunction<SidTuneInfo, String, String[]> processArgumentsFunction;

	private TextToSpeechType(BiFunction<SidTuneInfo, String, String[]> processArgumentsFunction) {
		this.processArgumentsFunction = processArgumentsFunction;
	}

	public BiFunction<SidTuneInfo, String, String[]> getProcessArgumentsFunction() {
		return processArgumentsFunction;
	}

	private static String[] createNoArgumentsFunction(SidTuneInfo info, String wavFile) {
		return new String[0];
	}

	private static String[] createEspeakArgumentsFunction(SidTuneInfo info, String wavFile) {
		String title = null, author = null, released = null;
		Iterator<String> it = info.getInfoString().iterator();
		if (it.hasNext()) {
			String next = it.next();
			if (!next.isEmpty()) {
				title = next.replace("<?>", "Unknown Title");
			}
		}
		if (it.hasNext()) {
			String next = it.next();
			if (!next.isEmpty() && !"<?>".equals(next)) {
				author = next;
			}
		}
		if (it.hasNext()) {
			String next = it.next();
			if (!next.isEmpty() && !"<?>".equals(next)) {
				released = next;
			}
		}
		String ssml = "<speak>" + "<voice language=\"en-GB\" gender=\"female\" required=\"gender\"\n"
				+ "ordering=\"gender language\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + replaceSpecials(title) + "</s>" : "")
				+ (author != null ? "<s>By: " + replaceSpecials(author) + "</s>" : "")
				+ (released != null
						? "<s>Released in: " + replaceUnknownDate(replaceDateRange(replaceSpecials(released))) + "</s>"
						: "")
				+ "  </p>" + "<voice>" + "</speak>";
		return new String[] { "espeak", ssml, "-m", "-w", wavFile };
	}

	private static String replaceSpecials(String string) {
		return Junidecode.unidecode(string).replaceAll("[/\\\\()]", "<break time=\"500ms\"/>").toLowerCase(Locale.US);
	}

	private static String replaceUnknownDate(String string) {
		Pattern pattern = Pattern.compile("19([89])[?](.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return "the " + matcher.group(1) + "0s" + "<break time=\"250ms\"/>" + matcher.group(2);
		}
		return string;
	}

	private static String replaceDateRange(String string) {
		Pattern pattern = Pattern.compile("([0-9]{4})-([0-9]{2})(.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1) + " to " + matcher.group(1).substring(0, 2) + matcher.group(2) + matcher.group(3);
		}
		return string;
	}

}
