package server.restful.common.text2speech;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import libsidutils.IOUtils;
import libsidutils.stil.STIL.Info;
import libsidutils.stil.STIL.STILEntry;
import libsidutils.stil.STIL.TuneEntry;
import sidplay.Player;

public class TextToSpeechBean {

	private File tuneFile;
	private Player player;
	private Locale textToSpeechLocale;

	private String title;
	private String author;
	private String released;

	private String basedOnTitle;
	private String basedOnArtist;

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
		this.tuneFile = tuneFile;
		this.player = player;
		this.textToSpeechLocale = textToSpeechLocale;
	}

	public void determineText2Speak(ResourceBundle resourceBundle) {
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
		Iterator<Info> infoIt = ofNullable(stilEntry).map(STILEntry::getInfos).orElse(new ArrayList<Info>()).iterator();
		if (infoIt.hasNext()) {
			Info next = infoIt.next();
			basedOnTitle = ofNullable(next.title).map(title -> title.replace("<?>", "")).orElse(null);
			basedOnArtist = ofNullable(next.artist).map(artist -> artist.replace("<?>", "")).orElse(null);
		}
		if ((basedOnTitle == null || basedOnTitle.isEmpty()) && (basedOnArtist == null || basedOnArtist.isEmpty())) {
			Optional<TuneEntry> subTune = ofNullable(stilEntry).map(STILEntry::getSubTunes)
					.orElse(new ArrayList<TuneEntry>()).stream().findFirst();
			if (subTune.isPresent()) {
				Iterator<Info> subTuneInfoIt = subTune.get().infos.iterator();
				if (subTuneInfoIt.hasNext()) {
					Info nextSubTuneInfo = subTuneInfoIt.next();
					{
						String next = ofNullable(nextSubTuneInfo.title).map(title -> title.replace("<?>", ""))
								.orElse("");
						basedOnTitle = next.isEmpty() ? null : next;
					}
					{
						String next = ofNullable(nextSubTuneInfo.artist).map(artist -> artist.replace("<?>", ""))
								.orElse("");
						basedOnArtist = next.isEmpty() ? null : next;
					}
				}
			}
		}
	}

	private String getCollectionName(Player player, File file) {
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
