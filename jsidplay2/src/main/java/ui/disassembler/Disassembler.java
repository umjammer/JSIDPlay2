package ui.disassembler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.disassembler.CPUCommand;
import libsidutils.disassembler.SimpleDisassembler;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.entities.config.SidPlay2Section;

public class Disassembler extends C64VBox implements UIPart {

	private final class DisassemblerRefresh implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getNewValue() == State.START) {
				Platform.runLater(() -> setTune());
			}
		}
	}

	public static final String ID = "DISASSEMBLER";

	@FXML
	private TextField address, startAddress, endAddress;
	@FXML
	private Button driverAddress, loadAddress, initAddress, playerAddress, save;
	@FXML
	private TableView<AssemblyLine> memoryTable;

	private ObservableList<AssemblyLine> assemblyLines;

	private DisassemblerRefresh disassemblerRefresh;

	private static final Map<Integer, CPUCommand> fCommands = SimpleDisassembler.getCpuCommands();

	public Disassembler() {
		super();
	}

	public Disassembler(final C64Window window, final Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		assemblyLines = FXCollections.<AssemblyLine>observableArrayList();
		SortedList<AssemblyLine> sortedList = new SortedList<>(assemblyLines);
		sortedList.comparatorProperty().bind(memoryTable.comparatorProperty());
		memoryTable.setItems(sortedList);
		disassemble(0);
		setTune();

		disassemblerRefresh = new DisassemblerRefresh();
		util.getPlayer().stateProperty().addListener(disassemblerRefresh);

	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(disassemblerRefresh);
	}

	protected void setTune() {
		if (util.getPlayer().getTune() == null) {
			return;
		}
		Platform.runLater(() -> {
			final SidTuneInfo tuneInfo = util.getPlayer().getTune().getInfo();
			driverAddress.setText(getDriverAddress(tuneInfo));
			loadAddress.setText(getLoadAddress(tuneInfo));
			initAddress.setText(getInitAddress(tuneInfo));
			playerAddress.setText(getPlayerAddress(tuneInfo));
		});
	}

	private String getPlayerAddress(final SidTuneInfo tuneInfo) {
		if (tuneInfo.getPlayAddr() == 0xffff) {
			return "N/A";
		} else {
			return String.format("0x%04x", tuneInfo.getPlayAddr());
		}
	}

	private String getInitAddress(final SidTuneInfo tuneInfo) {
		return String.format("0x%04x", tuneInfo.getInitAddr());
	}

	private String getLoadAddress(final SidTuneInfo tuneInfo) {
		return String.format("0x%04x - 0x%04x", tuneInfo.getLoadAddr(),
				tuneInfo.getLoadAddr() + tuneInfo.getC64dataLen() - 1);
	}

	private String getDriverAddress(final SidTuneInfo tuneInfo) {
		if (tuneInfo.getDeterminedDriverAddr() == 0) {
			return "N/A";
		} else {
			return String.format("0x%04x - 0x%04x", tuneInfo.getDeterminedDriverAddr(),
					tuneInfo.getDeterminedDriverAddr() + tuneInfo.getDeterminedDriverLength() - 1);
		}
	}

	private void disassemble(final int startAddr) {
		assemblyLines.clear();
		byte[] ram = util.getPlayer().getC64().getRAM();
		int offset = startAddr;
		do {
			CPUCommand cmd = fCommands.get(ram[offset & 0xffff] & 0xff);
			AssemblyLine assemblyLine = createAssemblyLine(ram, offset & 0xffff, cmd);
			assemblyLines.add(assemblyLine);
			offset += cmd.getByteCount();
		} while (offset <= 0xffff);
	}

	protected AssemblyLine createAssemblyLine(byte[] ram, int startAddr, CPUCommand cmd) {
		AssemblyLine assemblyLine = new AssemblyLine();
		assemblyLine.setAddress(String.format("%04X", startAddr & 0xffff));
		StringBuilder hexBytes = new StringBuilder();
		for (int i = 0; i < cmd.getByteCount(); i++) {
			hexBytes.append(String.format("%02X ", ram[startAddr + i & 0xffff]));
		}
		assemblyLine.setBytes(hexBytes.toString());
		String[] parts = SimpleDisassembler.getInstance().getDisassembly(ram, startAddr).split(":", 2);
		assemblyLine.setMnemonic(parts[0]);
		if (parts.length == 2) {
			assemblyLine.setOperands(parts[1]);
		}
		assemblyLine.setCycles(cmd.getCycles());
		return assemblyLine;
	}

	@FXML
	private void gotoMemStart() {
		disassemble(0);
	}

	@FXML
	private void gotoAddress() {
		gotoMem(address.getText());
	}

	@FXML
	private void gotoDriverAddress() {
		gotoMem(driverAddress.getText());
	}

	@FXML
	private void gotoLoadAddress() {
		gotoMem(loadAddress.getText());
	}

	@FXML
	private void gotoInitAddress() {
		gotoMem(initAddress.getText());
	}

	@FXML
	private void gotoPlayerAddress() {
		gotoMem(playerAddress.getText());
	}

	private void gotoMem(final String destination) {
		try {
			disassemble(Integer.decode(destination.substring(0, 6)) & 0xffff);
		} catch (final NumberFormatException | StringIndexOutOfBoundsException e) {
		}
	}

	@FXML
	private void saveMemory() {
		FileChooser fileDialog = new FileChooser();
		SidPlay2Section sidplay2 = util.getConfig().getSidplay2Section();
		fileDialog.setInitialDirectory(sidplay2.getLastDirectory());
		File file = fileDialog.showSaveDialog(save.getScene().getWindow());
		if (file != null) {
			sidplay2.setLastDirectory(file.getParentFile());
			try (FileOutputStream fos = new FileOutputStream(file)) {
				int start = Integer.decode(startAddress.getText());
				int end = Integer.decode(endAddress.getText()) + 1;
				start &= 0xffff;
				end &= 0xffff;
				fos.write(start & 0xff);
				fos.write(start >> 8);
				byte[] ram = util.getPlayer().getC64().getRAM();
				while (start != end) {
					fos.write(ram[start]);
					start = start + 1 & 0xffff;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
