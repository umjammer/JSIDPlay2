package ui.asm;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Scanner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import kickass.common.diagnostics.IDiagnostic;
import kickass.common.exceptions.AsmErrorException;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.assembler.KickAssembler;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.util.DesktopUtil;

public class Asm extends C64VBox implements UIPart {

	public static final String ID = "ASM";

	private static final String HOMEPAGE_URL = "http://www.theweb.dk/KickAssembler/";
	private static final String ASM_RESOURCE = "asm";
	private static final String ASM_EXAMPLE = "/ui/asm/Asm.asm";

	@FXML
	private TextArea contents;

	@FXML
	private TableView<Variable> variablesTable;
	@FXML
	private TableColumn<Variable, String> varNameColumn, varValueColumn;
	@FXML
	private Label status;

	private ObservableList<Variable> variables;

	public Asm() {
		super();
	}

	public Asm(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		status.setPrefHeight(Double.MAX_VALUE);
		InputStream is = Asm.class.getResourceAsStream(ASM_EXAMPLE);
		contents.setText(IOUtils.convertStreamToString(is, "ISO-8859-1"));
		varNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		varNameColumn.setOnEditCommit(
				evt -> evt.getTableView().getItems().get(evt.getTablePosition().getRow()).setName(evt.getNewValue()));
		varValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		varValueColumn.setOnEditCommit(
				evt -> evt.getTableView().getItems().get(evt.getTablePosition().getRow()).setValue(evt.getNewValue()));
		variables = FXCollections.<Variable>observableArrayList();
		SortedList<Variable> sortedList = new SortedList<>(variables);
		sortedList.comparatorProperty().bind(variablesTable.comparatorProperty());
		variablesTable.setItems(sortedList);
		varNameColumn.prefWidthProperty().bind(variablesTable.widthProperty().multiply(0.4));
		varValueColumn.prefWidthProperty().bind(variablesTable.widthProperty().multiply(0.6));

		contents.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.Z && event.isControlDown()) {
				contents.undo();
				event.consume();
			}
			if (event.getCode() == KeyCode.Y && event.isControlDown()) {
				contents.redo();
				event.consume();
			}

		});
	}

	@FXML
	private void gotoHomepage() {
		DesktopUtil.browse(HOMEPAGE_URL);
	}

	@FXML
	private void addVariable() {
		final Variable variable = new Variable();
		variable.setName("test");
		variable.setValue("1");
		variables.add(variable);
	}

	@FXML
	private void removeVariable() {
		final Variable selectedItem = variablesTable.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			variables.remove(selectedItem);
		}
	}

	@FXML
	private void compile() {
		try {
			HashMap<String, String> globals = new HashMap<>();
			variables.stream().forEach(var -> globals.put(var.getName(), var.getValue()));
			InputStream asm = new ByteArrayInputStream(contents.getText().getBytes(UTF_8.name()));
			byte[] assembly = KickAssembler.assemble(ASM_RESOURCE, asm, globals).getData();
			InputStream is = new ByteArrayInputStream(assembly);
			SidTune tune = SidTune.load("assembly.prg", is);
			status.setText("");
			util.setPlayingTab(this);
			util.getPlayer().play(tune);
		} catch (AsmErrorException e) {
			if (e.getError() != null) {
				highlightError(e.getError());
			}
			status.setText(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	private void highlightError(IDiagnostic e) {
		int pos = 0;
		int line = 0;
		if (e.getRange() != null) {
			try (Scanner s = new Scanner(contents.getText())) {
				s.useDelimiter("\n");
				while (s.hasNext() && line < e.getRange().getStartLineNo() - 1) {
					pos += s.next().length() + 1;
					line++;
				}
			}
			contents.positionCaret(pos + e.getRange().getStartLinePos() - 1);
		}
		contents.selectNextWord();
	}
}
