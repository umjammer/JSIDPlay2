package server.restful.common.text2speech;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidutils.IOUtils;
import net.gcardone.junidecode.Junidecode;

public enum TextToSpeechType {
	NONE(TextToSpeechType::createNoArgumentsFunction), PICO2WAVE(TextToSpeechType::createPico2WaveArgumentsFunction),
	ESPEAK(TextToSpeechType::createEspeakArgumentsFunction);

	private BiFunction<TextToSpeechBean, File, String[]> processArgumentsFunction;

	private TextToSpeechType(BiFunction<TextToSpeechBean, File, String[]> processArgumentsFunction) {
		this.processArgumentsFunction = processArgumentsFunction;
	}

	public BiFunction<TextToSpeechBean, File, String[]> getProcessArgumentsFunction() {
		return processArgumentsFunction;
	}

	private static String[] createNoArgumentsFunction(TextToSpeechBean textToSpeechBean, File wavFile) {
		return new String[0];
	}

	private static String[] createPico2WaveArgumentsFunction(TextToSpeechBean textToSpeechBean, File wavFile) {
		int volume = (int) (50 * (1 + textToSpeechBean.getVolume()));
		String ssml = String.format("<volume level=\"%d\"><pitch level=\"140\">%s</pitch></volume>", volume,
				createSsmlParagraph(textToSpeechBean));

		return new String[] { "pico2wave", "-l", createPicoToWaveLanguage(textToSpeechBean),
				"-w=" + wavFile.getAbsolutePath(), ssml };
	}

	private static String[] createEspeakArgumentsFunction(TextToSpeechBean textToSpeechBean, File wavFile) {
		double volume = 50 * (1 + textToSpeechBean.getVolume());
		String locale = textToSpeechBean.getTextToSpeechLocale().getLanguage();
		String ssml = String.format("<speak><voice language=\"%s\" gender=\"female\">%s</voice></speak>", locale,
				createSsmlParagraph(textToSpeechBean));

		return new String[] { "espeak", ssml, "-m", "-k20", "-a" + volume, "-w", wavFile.getAbsolutePath() };
	}

	private static String createSsmlParagraph(TextToSpeechBean textToSpeechBean) {
		ResourceBundle resourceBundle = IOUtils.getResourceBundle(TextToSpeechType.class.getName(),
				textToSpeechBean.getTextToSpeechLocale());
		textToSpeechBean.determineText2Speak(resourceBundle);

		String text = "<p>"
				+ (textToSpeechBean.getTitle() != null ? "<s>"
						+ resourceBundle.getString("NOW_PLAYING") + ": " + replaceSpecials(textToSpeechBean.getTitle(),
								resourceBundle)
						+ "</s>" : "")
				+ (textToSpeechBean
						.getAuthor() != null ? "<s>"
								+ resourceBundle.getString("AUTHOR") + " "
								+ replaceSpecials(replaceAliasName(textToSpeechBean.getAuthor(),
										resourceBundle), resourceBundle)
								+ "</s>" : "")
				+ (textToSpeechBean.getReleased() != null
						? "<s>" + resourceBundle.getString("RELEASED") + " "
								+ replaceSpecials(replaceUnknownDecade(replaceUnknownDate(
										replaceDateRange(textToSpeechBean.getReleased(), resourceBundle),
										resourceBundle), resourceBundle), resourceBundle)
								+ "</s>"
						: "")
				+ (textToSpeechBean.getBasedOnTitle() != null ? "<s>" + resourceBundle.getString("BASED_ON_TITLE") + " "
						+ replaceSpecials(removeBraces(textToSpeechBean.getBasedOnTitle()), resourceBundle) + "</s>"
						: "")
				+ (textToSpeechBean.getBasedOnArtist() != null ? "<s>" + resourceBundle.getString("BASED_ON_ARTIST")
						+ " " + replaceSpecials(textToSpeechBean.getBasedOnArtist(), resourceBundle) + "</s>" : "")
				+ "</p>";
		return text;
	}

	private static String createPicoToWaveLanguage(TextToSpeechBean textToSpeechBean) {
		if (Locale.ENGLISH.getLanguage().equals(textToSpeechBean.getTextToSpeechLocale().getLanguage())) {
			return Locale.ENGLISH.getLanguage() + "-GB";
		} else if (Locale.GERMAN.getLanguage().equals(textToSpeechBean.getTextToSpeechLocale().getLanguage())) {
			return Locale.GERMAN.getLanguage() + "-DE";
		}
		return Locale.ENGLISH.getLanguage() + "-GB";
	}

	private static String replaceSpecials(String string, ResourceBundle resourceBundle) {
		return Junidecode.unidecode(string).replaceAll("[/\\\\-_()]", "<break time=\"250ms\"/>").replace("&",
				" " + resourceBundle.getString("AND") + " ");
	}

	private static String replaceAliasName(String string, ResourceBundle resourceBundle) {
		Pattern pattern = Pattern.compile("([^(]*)[(]([^)]*)[)]");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1) + "(" + resourceBundle.getString("ALIAS") + " " + matcher.group(2) + ")";
		}
		return string;
	}

	private static String replaceUnknownDate(String string, ResourceBundle resourceBundle) {
		Pattern pattern = Pattern.compile("19[?][?](.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return resourceBundle.getString("IN_80_OR_90") + " / " + matcher.group(1);
		}
		return string;
	}

	private static String replaceUnknownDecade(String string, ResourceBundle resourceBundle) {
		Pattern pattern = Pattern.compile("19([89])[?](.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return String.format(resourceBundle.getString("IN_DECADE"), matcher.group(1) + "0") + " / "
					+ matcher.group(2);
		}
		return string;
	}

	private static String replaceDateRange(String string, ResourceBundle resourceBundle) {
		Pattern pattern = Pattern.compile("([0-9]{4})-([0-9]{2})(?!-)(.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return String.format(resourceBundle.getString("FROM_TO"), matcher.group(1),
					matcher.group(1).substring(0, 2) + matcher.group(2)) + " / " + matcher.group(3);
		}
		return string;
	}

	private static String removeBraces(String string) {
		return string.replaceAll("\\s*\\([^\\)]*\\)\\s*", "").replaceAll("\\[|\\]", "");
	}
}
