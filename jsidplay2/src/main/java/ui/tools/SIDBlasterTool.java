package ui.tools;

import static builder.jsidblaster.JSIDBlasterBuilder.getSerialNumbers;
import static builder.jsidblaster.JSIDBlasterBuilder.getSidType;
import static builder.jsidblaster.JSIDBlasterBuilder.setSerial;
import static builder.jsidblaster.JSIDBlasterBuilder.setSidType;
import static builder.jsidblaster.JSIDBlasterBuilder.uninitialize;
import static server.restful.common.parameter.ServletParameterHelper.check;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import builder.jsidblaster.JSIDBlasterBuilder;
import builder.jsidblaster.SIDType;
import sidplay.ini.IniConfig;
import sidplay.player.DebugUtil;

@Parameters(resourceBundle = "ui.tools.SIDBlasterTool")
public class SIDBlasterTool {

	static {
		DebugUtil.init();
		check(SIDBlasterTool.class, false);
	}

	private final static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("ui.tools.SIDBlasterTool");

	private static final String PATTERN_SERIAL_NO = "[A-Z0-9]{8}";

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--command", "-c" }, descriptionKey = "COMMAND", order = 1)
	private Command command;

	@Parameter(names = { "--deviceId", "-d" }, descriptionKey = "DEVICE_ID", order = 2)
	private int deviceId;

	@Parameter(names = { "--sidType", "-t" }, descriptionKey = "SID_TYPE", order = 3)
	private SIDType sidType = SIDType.NONE;

	@Parameter(names = { "--serial", "-s" }, descriptionKey = "SERIAL", order = 4)
	private String serialNo;

	private IniConfig config = new IniConfig();

	private String[] serialNumbers;

	private void create(String[] args) throws Exception {
		try {
			JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
			commander.parse(args);

			System.out.println(credits());

			if (command == null || help) {
				commander.usage();
				exit(0);
			}

			triggerFetchSerialNumbers();

			serialNumbers = getSerialNumbers();

			if (serialNumbers.length == 0) {
				System.out.println(RESOURCE_BUNDLE.getString("NO_SIDBLASTER_DEVICES_DETECTED"));
				JSIDBlasterBuilder.printInstallationHint();
				exit(1);
			}
			if (deviceId >= serialNumbers.length) {
				System.out.printf(RESOURCE_BUNDLE.getString("ILLEGAL_DEVICE_NUMBER"), deviceId);
				System.out.printf(RESOURCE_BUNDLE.getString("POSSIBLE_VALUE_RANGE"), serialNumbers.length - 1);
				exit(1);
			}

			switch (command) {
			case GET_SID_TYPE:
				printCommand("command=" + command, deviceId, getSidType(deviceId));
				break;

			case SET_SID_TYPE:
				printCommand("command=" + command, deviceId, sidType);
				switch (proceed()) {
				case 'y':
				case 'Y':
					System.out.printf("RC=%d\n", setSidType(deviceId, sidType));
					System.out.println(RESOURCE_BUNDLE.getString("DONE"));
					break;

				default:
					System.out.println(RESOURCE_BUNDLE.getString("ABORTED"));
					break;

				}
				break;

			case INFO:
			default:
				System.out.println(RESOURCE_BUNDLE.getString("DETECTED_SIDBLASTER_DEVICES"));
				for (int i = 0; i < serialNumbers.length; i++) {
					printCommand("\t", i, getSidType(i));
				}
				break;

			case SET_SERIAL:
				if (!Pattern.matches(PATTERN_SERIAL_NO, serialNo)) {
					System.out.println(RESOURCE_BUNDLE.getString("ILLEGAL_SERIAL_NUMBER"));
					break;
				}
				printCommand("command=" + command, deviceId, getSidType(deviceId));
				System.out.printf("newSerial=%s\n", serialNo);

				switch (proceed()) {
				case 'y':
				case 'Y':
					System.out.printf("RC=%d\n", setSerial(deviceId, serialNo));
					System.out.println(RESOURCE_BUNDLE.getString("DONE"));
					break;

				default:
					System.out.println(RESOURCE_BUNDLE.getString("ABORTED"));
					break;

				}
				break;
			}
		} finally {
			uninitialize();
		}
	}

	private void triggerFetchSerialNumbers() {
		new JSIDBlasterBuilder(null, config, null);
	}

	private String credits() {
		StringBuilder result = new StringBuilder();
		result.append(RESOURCE_BUNDLE.getString("CREDITS_PROLOG"));
		result.append(RESOURCE_BUNDLE.getString("CREDITS1"));
		result.append(RESOURCE_BUNDLE.getString("CREDITS2"));
		result.append(RESOURCE_BUNDLE.getString("CREDITS3"));
		result.append(RESOURCE_BUNDLE.getString("CREDITS4"));
		result.append(RESOURCE_BUNDLE.getString("CREDITS_EPILOG"));
		return result.toString();
	}

	private void printCommand(String prolog, int deviceId, SIDType sidType) {
		final String serialNumber = serialNumbers[deviceId];
		System.out.printf("%s - deviceId=%d, serial=%s, type=%s\n", prolog, deviceId, serialNumber, sidType);
	}

	private int proceed() throws IOException {
		System.out.println(RESOURCE_BUNDLE.getString("ARE_YOU_SURE"));
		return System.in.read();
	}

	private void exit(int rc) throws IOException {
		System.out.println(RESOURCE_BUNDLE.getString("EXIT"));
		System.in.read();
		System.exit(rc);
	}

	public static void main(String[] args) throws Exception {
		new SIDBlasterTool().create(args);
	}

}
