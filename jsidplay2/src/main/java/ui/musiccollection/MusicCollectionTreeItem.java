package ui.musiccollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import libsidutils.PathUtils;
import libsidutils.stil.STIL.STILEntry;
import sidplay.Player;
import ui.JSidPlay2Main;
import ui.common.comparator.FileComparator;
import ui.common.filefilter.TuneFileFilter;
import ui.entities.config.SidPlay2Section;

public class MusicCollectionTreeItem extends TreeItem<File> {

	private static final Image stilIcon = new Image(JSidPlay2Main.class.getResource("icons/stil.png").toString());

	private static final Image noStilIcon = new Image(JSidPlay2Main.class.getResource("icons/stil_no.png").toString());

	private final FileFilter fileFilter = new TuneFileFilter();
	private boolean hasLoadedChildren;
	private boolean isLeaf;
	private boolean hasSTIL;

	private Player player;
	private STILEntry stilEntry;

	public MusicCollectionTreeItem(Player player, File file) {
		super(file);
		this.player = player;
		this.isLeaf = file.isFile();
		if (isLeaf && player != null) {
			SidPlay2Section sidPlay2Section = (SidPlay2Section) player.getConfig().getSidplay2Section();
			String collectionName = PathUtils.getCollectionName(sidPlay2Section.getHvsc(), file);
			this.stilEntry = player.getStilEntry(collectionName);
			this.hasSTIL = stilEntry != null;
		}
	}

	@Override
	public boolean isLeaf() {
		return isLeaf;
	}

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (hasLoadedChildren == false) {
			loadChildren();
		}
		return super.getChildren();
	}

	public boolean hasSTIL() {
		return hasSTIL;
	}

	public STILEntry getStilEntry() {
		return stilEntry;
	}

	private void loadChildren() {
		hasLoadedChildren = true;
		Collection<MusicCollectionTreeItem> children = new ArrayList<>();
		Arrays.stream(Optional.ofNullable(getValue().listFiles(fileFilter)).orElse(new File[0]))
				.sorted(new FileComparator()).forEach(file -> {
					MusicCollectionTreeItem childItem = new MusicCollectionTreeItem(player, file);
					children.add(childItem);
					if (childItem.hasSTIL()) {
						childItem.setGraphic(new ImageView(stilIcon));
					} else {
						childItem.setGraphic(new ImageView(noStilIcon));
					}
				});
		super.getChildren().setAll(children);
	}

}
