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
	private double volume;

	private Integer songNo;

	private String title;
	private String author;
	private String released;

	private String songName;
	private String basedOnTitle;
	private String basedOnArtist;
	private boolean andMore;

	public Integer getSongNo() {
		return songNo;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getReleased() {
		return released;
	}

	public String getName() {
		return songName;
	}

	public String getBasedOnTitle() {
		return basedOnTitle;
	}

	public String getBasedOnArtist() {
		return basedOnArtist;
	}

	public boolean isAndMore() {
		return andMore;
	}

	public Locale getTextToSpeechLocale() {
		return textToSpeechLocale;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public double getVolume() {
		return volume;
	}

	public TextToSpeechBean(File tuneFile, Player player, Locale textToSpeechLocale) {
		this.tuneFile = tuneFile;
		this.player = player;
		this.textToSpeechLocale = textToSpeechLocale;
	}

	protected void determineText2Speak(ResourceBundle resourceBundle) {
		if (player.getTune().getInfo().getSongs() > 1) {
			songNo = player.getTune().getInfo().getCurrentSong();
		}
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
		if ((basedOnTitle == null || basedOnTitle.isEmpty()) && (basedOnArtist == null || basedOnArtist.isEmpty())
				&& songNo != null) {
			Optional<TuneEntry> subTune = ofNullable(stilEntry).map(STILEntry::getSubTunes)
					.orElse(new ArrayList<TuneEntry>()).stream().filter(e -> songNo.equals(e.tuneNo)).findFirst();
			if (subTune.isPresent()) {
				Iterator<Info> subTuneInfoIt = subTune.get().infos.iterator();
				if (subTuneInfoIt.hasNext()) {
					Info nextSubTuneInfo = subTuneInfoIt.next();
					{
						String next = ofNullable(nextSubTuneInfo.name).map(name -> name.replace("<?>", "")).orElse("");
						songName = next.isEmpty() ? null : next;
					}
					{
						String next = ofNullable(nextSubTuneInfo.author).map(author -> author.replace("<?>", ""))
								.orElse("");
						author = next.isEmpty() ? author : next;
					}
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
					andMore = subTuneInfoIt.hasNext();
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
