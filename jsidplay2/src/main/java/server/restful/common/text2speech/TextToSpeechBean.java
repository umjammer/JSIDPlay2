package server.restful.common.text2speech;

import java.io.File;
import java.util.Locale;

import sidplay.Player;

public class TextToSpeechBean {

	private TextToSpeechType textToSpeechType;

	private Locale textToSpeechLocale;

	private File tuneFile;

	private Player player;

	public TextToSpeechType getTextToSpeechType() {
		return textToSpeechType;
	}

	public void setTextToSpeechType(TextToSpeechType textToSpeechType) {
		this.textToSpeechType = textToSpeechType;
	}

	public Locale getTextToSpeechLocale() {
		return textToSpeechLocale;
	}

	public void setTextToSpeechLocale(Locale textToSpeechLocale) {
		this.textToSpeechLocale = textToSpeechLocale;
	}

	public File getTuneFile() {
		return tuneFile;
	}

	public void setTuneFile(File tuneFile) {
		this.tuneFile = tuneFile;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
