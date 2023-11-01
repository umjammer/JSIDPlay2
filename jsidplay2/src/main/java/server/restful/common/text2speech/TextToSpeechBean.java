package server.restful.common.text2speech;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import libsidutils.IOUtils;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import sidplay.Player;

public class TextToSpeechBean {

	private String title;
	private String author;
	private String released;

	private String basedOnTitle;
	private String basedOnArtist;

	private Locale textToSpeechLocale;

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getReleased() {
		return released;
	}

	public String getBasedOnTitle() {
		return basedOnTitle;
	}

	public String getBasedOnArtist() {
		return basedOnArtist;
	}

	public Locale getTextToSpeechLocale() {
		return textToSpeechLocale;
	}

	public TextToSpeechBean(File tuneFile, Player player, Locale textToSpeechLocale) {
		this.textToSpeechLocale = textToSpeechLocale;
		ResourceBundle resourceBundle = IOUtils.getResourceBundle(TextToSpeechType.class.getName(), textToSpeechLocale);

		Iterator<String> it = player.getTune().getInfo().getInfoString().iterator();
		if (it.hasNext()) {
			String next = it.next().replace("<?>", "");
			title = next.isEmpty() ? resourceBundle.getString("UNKNOWN_TITLE") : next;
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
		STILEntry stilEntry = player.getStilEntry(getCollectionName(player, tuneFile));
		Iterator<Info> infoIt = Optional.ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>())
				.iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = Optional.ofNullable(next.title).map(title -> title.replace("<?>", "")).orElse(null);
			basedOnArtist = Optional.ofNullable(next.artist).map(artist -> artist.replace("<?>", "")).orElse(null);
		}
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

}
