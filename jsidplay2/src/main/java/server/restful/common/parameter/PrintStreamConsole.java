package server.restful.common.parameter;

import java.io.PrintStream;

import com.beust.jcommander.internal.Console;

public class PrintStreamConsole implements Console {

	private final PrintStream out;

	public PrintStreamConsole(PrintStream out) {
		this.out = out;
	}

	@Override
	public void print(String msg) {
		out.print(msg);
	}

	@Override
	public void println(String msg) {
		out.println(msg);
	}

	@Override
	public char[] readPassword(boolean echoInput) {
		throw new RuntimeException("readPassword not supported!");
	}

}
