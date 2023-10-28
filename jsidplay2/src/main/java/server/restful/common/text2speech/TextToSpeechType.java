package server.restful.common.text2speech;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidutils.IOUtils;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import net.gcardone.junidecode.Junidecode;
import sidplay.Player;

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
		ResourceBundle RESOURCE_BUNDLE = IOUtils.getResourceBundle(TextToSpeechType.class.getName(),
				textToSpeechBean.getTextToSpeechLocale());

		String title = null, author = null, released = null;
		Iterator<String> it = textToSpeechBean.getPlayer().getTune().getInfo().getInfoString().iterator();
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

		String basedOnTitle = null, basedOnAuthor = null;
		String collectionName = getCollectionName(textToSpeechBean.getPlayer(), textToSpeechBean.getTuneFile());
		STILEntry stilEntry = textToSpeechBean.getPlayer().getStilEntry(collectionName);
		Iterator<Info> infoIt = Optional.ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>())
				.iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = next.title;
			basedOnAuthor = next.artist;
		}

		String text = "<volume level=\"75\"> <pitch level=\"140\">" + "<p>"
				+ (title != null
						? "<s>" + RESOURCE_BUNDLE.getString("NOW_PLAYING") + ": " + replaceSpecials(title) + "</s>"
						: "")
				+ (author != null
						? "<s>" + RESOURCE_BUNDLE.getString("AUTHOR") + " "
								+ replaceSpecials(replaceAliasName(RESOURCE_BUNDLE, author)) + "</s>"
						: "")
				+ (released != null ? "<s>" + RESOURCE_BUNDLE.getString("RELEASED") + " "
						+ replaceSpecials(replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))) + "</s>"
						: "")
				+ (basedOnTitle != null
						? "<s>" + RESOURCE_BUNDLE.getString("ARTIST") + " "
								+ replaceSpecials(removeBraces(basedOnTitle)) + "</s>"
						: "")
				+ (basedOnAuthor != null
						? "<s>" + RESOURCE_BUNDLE.getString("AUTHOR") + " " + replaceSpecials(basedOnAuthor) + "</s>"
						: "")
				+ "  </p>" + "</pitch></volume>";
		return new String[] { "pico2wave", "-l", createPicoToWaveLanguage(textToSpeechBean),
				"-w=" + wavFile.getAbsolutePath(), text };
	}

	private static String createPicoToWaveLanguage(TextToSpeechBean textToSpeechBean) {
		if (Locale.ENGLISH.getLanguage().equals(textToSpeechBean.getTextToSpeechLocale().getLanguage())) {
			return Locale.ENGLISH.getLanguage() + "-GB";
		} else if (Locale.GERMAN.getLanguage().equals(textToSpeechBean.getTextToSpeechLocale().getLanguage())) {
			return Locale.GERMAN.getLanguage() + "-DE";
		}
		return Locale.ENGLISH.getLanguage() + "-GB";
	}

	private static String[] createEspeakArgumentsFunction(TextToSpeechBean textToSpeechBean, File wavFile) {
		ResourceBundle RESOURCE_BUNDLE = IOUtils.getResourceBundle(TextToSpeechType.class.getName(),
				textToSpeechBean.getTextToSpeechLocale());

		String title = null, author = null, released = null;
		Iterator<String> it = textToSpeechBean.getPlayer().getTune().getInfo().getInfoString().iterator();
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

		String basedOnTitle = null, basedOnAuthor = null;
		String collectionName = getCollectionName(textToSpeechBean.getPlayer(), textToSpeechBean.getTuneFile());
		STILEntry stilEntry = textToSpeechBean.getPlayer().getStilEntry(collectionName);
		Iterator<Info> infoIt = Optional.ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>())
				.iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = next.title;
			basedOnAuthor = next.artist;
		}

		String ssml = "<speak>" + "<voice language=\"" + textToSpeechBean.getTextToSpeechLocale().getLanguage()
				+ "\" gender=\"female\">" + "<p>"
				+ (title != null
						? "<s>" + RESOURCE_BUNDLE.getString("NOW_PLAYING") + ": " + toLower(replaceSpecials(title))
								+ "</s>"
						: "")
				+ (author != null
						? "<s>" + RESOURCE_BUNDLE.getString("AUTHOR") + " "
								+ toLower(replaceSpecials(replaceAliasName(RESOURCE_BUNDLE, author))) + "</s>"
						: "")
				+ (released != null
						? "<s>" + RESOURCE_BUNDLE.getString("RELEASED") + " "
								+ toLower(replaceSpecials(
										replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))))
								+ "</s>"
						: "")
				+ (basedOnTitle != null
						? "<s>" + RESOURCE_BUNDLE.getString("ARTIST") + " "
								+ replaceSpecials(removeBraces(basedOnTitle)) + "</s>"
						: "")
				+ (basedOnAuthor != null
						? "<s>" + RESOURCE_BUNDLE.getString("AUTHOR") + " " + replaceSpecials(basedOnAuthor) + "</s>"
						: "")
				+ "  </p>" + "<voice>" + "</speak>";
		return new String[] { "espeak", ssml, "-m", "-k40", "-a50", "-w", wavFile.getAbsolutePath() };
	}

	private static String getCollectionName(Player player, File file) {
		String result = "";

		File hvscRoot = player.getConfig().getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			// 1st Try from full path name...
			result = IOUtils.getCollectionName(hvscRoot, file);
			if (result.isEmpty()) {
				// ... then try from MD5
				result = player.getSidDatabaseInfo(sidDatabase -> sidDatabase.getPath(player.getTune()), "");
			}
		}
		return result;
	}

	private static String replaceSpecials(String string) {
		return Junidecode.unidecode(string).replaceAll("[/\\\\-_()]", "<break time=\"250ms\"/>");
	}

	private static String toLower(String string) {
		return string.toLowerCase(Locale.US);
	}

	private static String replaceAliasName(ResourceBundle resourceBundle, String string) {
		Pattern pattern = Pattern.compile("([^(]*)[(]([^)]*)[)]");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1) + "(" + resourceBundle.getString("ALIAS") + " " + matcher.group(2) + ")";
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
		Pattern pattern = Pattern.compile("([0-9]{4})-([0-9]{2})(?!-)(.*)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			return matcher.group(1) + " to " + matcher.group(1).substring(0, 2) + matcher.group(2) + matcher.group(3);
		}
		return string;
	}

	private static String removeBraces(String string) {
		return string.replaceAll("\\s*\\([^\\)]*\\)\\s*", "").replaceAll("\\[|\\]", "");
	}
}
