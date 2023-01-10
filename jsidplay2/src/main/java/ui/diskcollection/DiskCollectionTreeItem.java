package ui.diskcollection;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import ui.common.comparator.FileComparator;

public class DiskCollectionTreeItem extends TreeItem<File> {

	private File rootFile;
	private FileFilter fileFilter;
	private boolean hasLoadedChildren;
	private boolean isLeaf;

	public DiskCollectionTreeItem(File file, File rootFile, FileFilter fileFilter) {
		super(file);
		this.rootFile = rootFile;
		this.isLeaf = file.isFile();
		this.fileFilter = fileFilter;
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

	private void loadChildren() {
		hasLoadedChildren = true;
		Collection<DiskCollectionTreeItem> children = new ArrayList<>();
		Arrays.stream(Optional.ofNullable(getValue().listFiles(fileFilter)).orElse(new File[0]))
				.sorted(new FileComparator()).forEach(file -> {
					children.add(new DiskCollectionTreeItem(file, rootFile, fileFilter));
				});
		super.getChildren().setAll(children);
	}
}
