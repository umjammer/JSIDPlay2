package sidplay.ini.validator;

import static sidplay.ini.IniDefaults.FILTER_SECTIONS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import libsidplay.config.IFilterSection;

public class ReSidFpFilter6581Validator implements IParameterValidator {

	@Override
	public void validate(String name, String value) throws ParameterException {
		List<String> availableFilterNames = new ArrayList<>();
		for (IFilterSection filter : FILTER_SECTIONS) {
			if (filter.isReSIDfpFilter6581()) {
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
