package server.restful.common.text2speech;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidplay.sidtune.SidTune;
import net.gcardone.junidecode.Junidecode;

public enum TextToSpeechType {
	NONE(TextToSpeechType::createNoArgumentsFunction), PICO2WAVE(TextToSpeechType::createPico2WaveArgumentsFunction),
	ESPEAK(TextToSpeechType::createEspeakArgumentsFunction);

	private BiFunction<SidTune, File, String[]> processArgumentsFunction;

	private TextToSpeechType(BiFunction<SidTune, File, String[]> processArgumentsFunction) {
		this.processArgumentsFunction = processArgumentsFunction;
	}

	public BiFunction<SidTune, File, String[]> getProcessArgumentsFunction() {
		return processArgumentsFunction;
	}

	private static String[] createNoArgumentsFunction(SidTune tune, File wavFile) {
		return new String[0];
	}

	private static String[] createPico2WaveArgumentsFunction(SidTune tune, File wavFile) {
		String title = null, author = null, released = null;
		Iterator<String> it = tune.getInfo().getInfoString().iterator();
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			title = next.isEmpty() ? "Unknown Title" : next;
		}
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			if (!next.isEmpty()) {
				author = next;
			}
		}
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			if (!next.isEmpty()) {
				released = next;
			}
		}
		String text = "<volume level=\"75\"> <pitch level=\"140\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + replaceSpecials(title) + "</s>" : "")
				+ (author != null ? "<s>by " + replaceSpecials(replaceAliasName(author)) + "</s>" : "")
				+ (released != null ? "<s>released in "
						+ replaceSpecials(replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))) + "</s>"
						: "")
				+ "  </p>" + "</pitch></volume>";
		return new String[] { "pico2wave", "-l", "en-US", "-w=" + wavFile.getAbsolutePath(), text };
	}

	private static String[] createEspeakArgumentsFunction(SidTune tune, File wavFile) {
		String title = null, author = null, released = null;
		Iterator<String> it = tune.getInfo().getInfoString().iterator();
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			title = next.isEmpty() ? "Unknown Title" : next;
		}
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			if (!next.isEmpty()) {
				author = next;
			}
		}
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			if (!next.isEmpty()) {
				released = next;
			}
		}
		String ssml = "<speak>" + "<voice language=\"en-GB\" gender=\"female\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + toLower(replaceSpecials(title)) + "</s>" : "")
				+ (author != null ? "<s>by " + toLower(replaceSpecials(replaceAliasName(author))) + "</s>" : "")
				+ (released != null ? "<s>released in "
						+ toLower(replaceSpecials(replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))))
						+ "</s>" : "")
				+ "  </p>" + "<voice>" + "</speak>";
		return new String[] { "espeak", ssml, "-m", "-w", wavFile.getAbsolutePath() };
	}

	private static String replaceSpecials(String string) {
		return Junidecode.unidecode(string).replaceAll("[/\\\\()-_]", "<break time=\"250ms\"/>");
	}

	private static String toLower(String string) {
		return string.toLowerCase(Locale.US);
	}

	private static String replaceAliasName(String string) {
		Pattern pattern = Pattern.compile("([^(]*)[(]([^)]*)[)]");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1) + "(Alias " + matcher.group(2) + ")";
		}
		return string;
	}

	private static String replaceUnknownDate(String string) {
		Pattern pattern = Pattern.compile("19[?][?](.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return "the 80s or 90" + " / " + matcher.group(1);
		}
		return string;
	}

	private static String replaceUnknownDecade(String string) {
		Pattern pattern = Pattern.compile("19([89])[?](.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return "the " + matcher.group(1) + "0s" + " / " + matcher.group(2);
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
