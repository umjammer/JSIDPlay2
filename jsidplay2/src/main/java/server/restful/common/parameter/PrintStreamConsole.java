package server.restful.common.parameter;

import java.io.PrintStream;

import com.beust.jcommander.internal.DefaultConsole;

public class PrintStreamConsole extends DefaultConsole {

	public PrintStreamConsole(PrintStream out) {
		super(out);
	}

	@Override
	public char[] readPassword(boolean echoInput) {
		throw new RuntimeException("readPassword not supported!");
	}

}
