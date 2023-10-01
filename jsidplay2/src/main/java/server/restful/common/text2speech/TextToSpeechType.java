package server.restful.common.text2speech;

import java.util.Iterator;
import java.util.function.BiFunction;

import libsidplay.sidtune.SidTuneInfo;

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
			if (!next.isEmpty() && !"<?>".equals(next)) {
				title = next;
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
		String text = "<speak>" + "<voice language=\"en-GB\" gender=\"female\" required=\"gender\"\n"
				+ "ordering=\"gender language\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + replaceUmlauts(title) + "</s>" : "")
				+ (author != null ? "<s>By: " + replaceUmlauts(author) + "</s>" : "")
				+ (released != null ? "<s>Released at: " + released + "</s>" : "") + "  </p>" + "<voice>" + "</speak>";
		return new String[] { "espeak", text, "-m", "-w", wavFile };
	}

	private static String replaceUmlauts(String title) {
		return title.replace('ä', 'a').replace('Ä', 'A').replace('ö', 'o').replace('Ö', 'O').replace('ü', 'u')
				.replace('Ü', 'U').replace('ß', 's').replaceAll("[^\\x00-\\x7F]", "");
	}

}
