package ui.entities.collection;

import static java.util.stream.Collectors.toList;
import static libsidplay.sidtune.SidTune.RESET;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Clock;
import libsidplay.sidtune.SidTune.Compatibility;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTune.Speed;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.converter.LocalDateTimeToStringDeserializer;
import ui.common.converter.LocalDateTimeToStringSerializer;
import ui.common.converter.LocalDateTimeXmlAdapter;
import ui.common.properties.LazyListField;

@Entity
@Access(AccessType.PROPERTY)
public class HVSCEntry {

	public static final List<StilEntry> DEFAULT_STIL = Collections.emptyList();

	public HVSCEntry() {
	}

	public HVSCEntry(final DoubleSupplier lengthFnct, final String path, final File tuneFile, SidTune tune) {
		this.name = tuneFile.getName();
		this.path = path.length() > 0 ? path : tuneFile.getPath();
		if (tune != RESET) {
			SidTuneInfo info = tune.getInfo();
			info.setSelectedSong(1);
			Iterator<String> descriptionIt = info.getInfoString().iterator();
			if (descriptionIt.hasNext()) {
				this.title = descriptionIt.next();
			}
			if (descriptionIt.hasNext()) {
				this.author = descriptionIt.next();
			}
			if (descriptionIt.hasNext()) {
				this.released = descriptionIt.next();
			}
			this.format = tune.getClass().getSimpleName();
			this.playerId = tune.identify().stream().collect(Collectors.joining(","));
			this.noOfSongs = info.getSongs();
			this.startSong = info.getStartSong();
			this.clockFreq = info.getClockSpeed();
			this.speed = tune.getSongSpeed(1);
			this.sidModel1 = info.getSIDModel(0);
			this.sidModel2 = info.getSIDModel(1);
			this.sidModel3 = info.getSIDModel(2);
			this.compatibility = info.getCompatibility();
			this.tuneLength = lengthFnct.getAsDouble();
			this.audio = info.getAudioTypeString();
			this.sidChipBase1 = info.getSIDChipBase(0);
			this.sidChipBase2 = info.getSIDChipBase(1);
			this.sidChipBase3 = info.getSIDChipBase(2);
			this.driverAddress = info.getDeterminedDriverAddr();
			this.loadAddress = info.getLoadAddr();
			this.loadLength = info.getC64dataLen();
			this.initAddress = info.getInitAddr();
			this.playerAddress = info.getPlayAddr();
			this.fileDate = Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			this.fileSizeKb = tuneFile.length() >> 10;
			this.tuneSizeB = tuneFile.length();
			this.relocStartPage = info.getRelocStartPage();
			this.relocNoPages = info.getRelocPages();
		}
	}

	private Integer id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@XmlTransient
	@JsonIgnore
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String title;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private String author;

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	private String released;

	public String getReleased() {
		return released;
	}

	public void setReleased(String released) {
		this.released = released;
	}

	private String format;

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	private String playerId;

	@Column(length = 2048)
	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	private Integer noOfSongs;

	public Integer getNoOfSongs() {
		return noOfSongs;
	}

	public void setNoOfSongs(Integer noOfSongs) {
		this.noOfSongs = noOfSongs;
	}

	private Integer startSong;

	public Integer getStartSong() {
		return startSong;
	}

	public void setStartSong(Integer startSong) {
		this.startSong = startSong;
	}

	private Clock clockFreq;

	@Enumerated(EnumType.STRING)
	public Clock getClockFreq() {
		return clockFreq;
	}

	public void setClockFreq(Clock clockFreq) {
		this.clockFreq = clockFreq;
	}

	private Speed speed;

	@Enumerated(EnumType.STRING)
	public Speed getSpeed() {
		return speed;
	}

	public void setSpeed(Speed speed) {
		this.speed = speed;
	}

	private Model sidModel1;

	@Enumerated(EnumType.STRING)
	public Model getSidModel1() {
		return sidModel1;
	}

	public void setSidModel1(Model sidModel1) {
		this.sidModel1 = sidModel1;
	}

	private Model sidModel2;

	@Enumerated(EnumType.STRING)
	public Model getSidModel2() {
		return sidModel2;
	}

	public void setSidModel2(Model sidModel2) {
		this.sidModel2 = sidModel2;
	}

	private Model sidModel3;

	@Enumerated(EnumType.STRING)
	public Model getSidModel3() {
		return sidModel3;
	}

	public void setSidModel3(Model sidModel3) {
		this.sidModel3 = sidModel3;
	}

	private Compatibility compatibility;

	@Enumerated(EnumType.STRING)
	public Compatibility getCompatibility() {
		return compatibility;
	}

	public void setCompatibility(Compatibility compatibility) {
		this.compatibility = compatibility;
	}

	private Double tuneLength;

	public Double getTuneLength() {
		return tuneLength;
	}

	public void setTuneLength(Double tuneLength) {
		BigDecimal bd = BigDecimal.valueOf(tuneLength);
		bd = bd.setScale(3, RoundingMode.HALF_UP);
		this.tuneLength = bd.doubleValue();
	}

	private String audio;

	public String getAudio() {
		return audio;
	}

	public void setAudio(String audio) {
		this.audio = audio;
	}

	private Integer sidChipBase1;

	public Integer getSidChipBase1() {
		return sidChipBase1;
	}

	public void setSidChipBase1(Integer sidChipBase1) {
		this.sidChipBase1 = sidChipBase1;
	}

	private Integer sidChipBase2;

	public Integer getSidChipBase2() {
		return sidChipBase2;
	}

	public void setSidChipBase2(Integer sidChipBase2) {
		this.sidChipBase2 = sidChipBase2;
	}

	private Integer sidChipBase3;

	public Integer getSidChipBase3() {
		return sidChipBase3;
	}

	public void setSidChipBase3(Integer sidChipBase3) {
		this.sidChipBase3 = sidChipBase3;
	}

	private Integer driverAddress;

	public Integer getDriverAddress() {
		return driverAddress;
	}

	public void setDriverAddress(Integer driverAddress) {
		this.driverAddress = driverAddress;
	}

	private Integer loadAddress;

	public Integer getLoadAddress() {
		return loadAddress;
	}

	public void setLoadAddress(Integer loadAddress) {
		this.loadAddress = loadAddress;
	}

	private Integer loadLength;

	public Integer getLoadLength() {
		return loadLength;
	}

	public void setLoadLength(Integer loadLength) {
		this.loadLength = loadLength;
	}

	private Integer initAddress;

	public Integer getInitAddress() {
		return initAddress;
	}

	public void setInitAddress(Integer initAddress) {
		this.initAddress = initAddress;
	}

	private Integer playerAddress;

	public Integer getPlayerAddress() {
		return playerAddress;
	}

	public void setPlayerAddress(Integer playerAddress) {
		this.playerAddress = playerAddress;
	}

	private LocalDateTime fileDate;

	@XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
	@JsonSerialize(using = LocalDateTimeToStringSerializer.class)
	@JsonDeserialize(using = LocalDateTimeToStringDeserializer.class)
	public LocalDateTime getFileDate() {
		return fileDate;
	}

	public void setFileDate(LocalDateTime fileDate) {
		this.fileDate = fileDate;
	}

	private Long fileSizeKb;

	public Long getFileSizeKb() {
		return fileSizeKb;
	}

	public void setFileSizeKb(Long fileSizeInKb) {
		this.fileSizeKb = fileSizeInKb;
	}

	private Long tuneSizeB;

	public Long getTuneSizeB() {
		return tuneSizeB;
	}

	public void setTuneSizeB(Long tuneSizeInB) {
		this.tuneSizeB = tuneSizeInB;
	}

	private Short relocStartPage;

	public Short getRelocStartPage() {
		return relocStartPage;
	}

	public void setRelocStartPage(Short relocStartPage) {
		this.relocStartPage = relocStartPage;
	}

	private Short relocNoPages;

	public Short getRelocNoPages() {
		return relocNoPages;
	}

	public void setRelocNoPages(Short relocNoPages) {
		this.relocNoPages = relocNoPages;
	}

	private LazyListField<StilEntry> stil = new LazyListField<StilEntry>();

	@OneToMany(mappedBy = "hvscEntry", fetch = FetchType.LAZY)
	public List<StilEntry> getStil() {
		return stil.get(() -> DEFAULT_STIL.stream().map(StilEntry::new).collect(toList()));
	}

	public void setStil(List<StilEntry> stil) {
		this.stil.set(stil);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
