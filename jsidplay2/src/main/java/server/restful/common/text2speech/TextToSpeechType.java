package server.restful.common.text2speech;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
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

	private BiFunction<Entry<Player, File>, File, String[]> processArgumentsFunction;

	private TextToSpeechType(BiFunction<Entry<Player, File>, File, String[]> processArgumentsFunction) {
		this.processArgumentsFunction = processArgumentsFunction;
	}

	public BiFunction<Entry<Player, File>, File, String[]> getProcessArgumentsFunction() {
		return processArgumentsFunction;
	}

	private static String[] createNoArgumentsFunction(Entry<Player, File> playerAndTuneFile, File wavFile) {
		return new String[0];
	}

	private static String[] createPico2WaveArgumentsFunction(Entry<Player, File> playerAndTuneFile, File wavFile) {
		Player player = playerAndTuneFile.getKey();
		File file = playerAndTuneFile.getValue();

		String title = null, author = null, released = null;
		Iterator<String> it = player.getTune().getInfo().getInfoString().iterator();
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
		STILEntry stilEntry = player.getStilEntry(getCollectionName(player, file));
		Iterator<Info> infoIt = Optional.ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>())
				.iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = next.title;
			basedOnAuthor = next.artist;
		}

		String text = "<volume level=\"75\"> <pitch level=\"140\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + replaceSpecials(title) + "</s>" : "")
				+ (author != null ? "<s>by " + replaceSpecials(replaceAliasName(author)) + "</s>" : "")
				+ (released != null ? "<s>released in "
						+ replaceSpecials(replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))) + "</s>"
						: "")
				+ (basedOnTitle != null ? "<s>based on " + replaceSpecials(removeBraces(basedOnTitle)) + "</s>" : "")
				+ (basedOnAuthor != null ? "<s>by " + replaceSpecials(basedOnAuthor) + "</s>" : "") + "  </p>"
				+ "</pitch></volume>";
		return new String[] { "pico2wave", "-l", "en-US", "-w=" + wavFile.getAbsolutePath(), text };
	}

	private static String[] createEspeakArgumentsFunction(Entry<Player, File> playerAndTuneFile, File wavFile) {
		Player player = playerAndTuneFile.getKey();
		File file = playerAndTuneFile.getValue();

		String title = null, author = null, released = null;
		Iterator<String> it = player.getTune().getInfo().getInfoString().iterator();
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
		STILEntry stilEntry = player.getStilEntry(getCollectionName(player, file));
		Iterator<Info> infoIt = Optional.ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>())
				.iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = next.title;
			basedOnAuthor = next.artist;
		}

		String ssml = "<speak>" + "<voice language=\"en-GB\" gender=\"female\">" + "<p>"
				+ (title != null ? "<s>Now playing: " + toLower(replaceSpecials(title)) + "</s>" : "")
				+ (author != null ? "<s>by " + toLower(replaceSpecials(replaceAliasName(author))) + "</s>" : "")
				+ (released != null ? "<s>released in "
						+ toLower(replaceSpecials(replaceUnknownDecade(replaceUnknownDate(replaceDateRange(released)))))
						+ "</s>" : "")
				+ (basedOnTitle != null ? "<s>based on " + replaceSpecials(removeBraces(basedOnTitle)) + "</s>" : "")
				+ (basedOnAuthor != null ? "<s>by " + replaceSpecials(basedOnAuthor) + "</s>" : "") + "  </p>"
				+ "<voice>" + "</speak>";
		return new String[] { "espeak", ssml, "-m", "-w", wavFile.getAbsolutePath() };
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
