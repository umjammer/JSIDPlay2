package sidplay.ini.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import libsidplay.config.IConfig;
import libsidplay.config.IFilterSection;
import sidplay.ini.IniConfig;

public class ReSidFilter8580Validator implements IParameterValidator {

	private final IConfig config = new IniConfig(false);

	@Override
	public void validate(String name, String value) throws ParameterException {
		List<String> availableFilterNames = new ArrayList<>();
		for (IFilterSection filter : config.getFilterSection()) {
			if (filter.isReSIDFilter8580()) {
				availableFilterNames.add(filter.getName());
				if (filter.getName().equals(value)) {
					return;
				}
			}
		}
		throw new ParameterException("Invalid Parameter " + name + " value " + value + ", expected: "
				+ availableFilterNames.stream().collect(Collectors.joining(",")));
	}
}
