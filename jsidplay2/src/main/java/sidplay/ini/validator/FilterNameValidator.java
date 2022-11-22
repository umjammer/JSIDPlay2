package sidplay.ini.validator;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import libsidplay.config.IConfig;
import libsidplay.config.IFilterSection;
import sidplay.ini.IniConfig;

public abstract class FilterNameValidator implements IParameterValidator {

	private final static IConfig CONFIG = new IniConfig(false);

	public static final class ReSidFilter6581Validator extends FilterNameValidator {

		public ReSidFilter6581Validator() {
			super(IFilterSection::isReSIDFilter6581);
		}
	}

	public static final class ReSidFilter8580Validator extends FilterNameValidator {

		public ReSidFilter8580Validator() {
			super(IFilterSection::isReSIDFilter8580);
		}
	}

	public static final class ReSidFpFilter6581Validator extends FilterNameValidator {

		public ReSidFpFilter6581Validator() {
			super(IFilterSection::isReSIDfpFilter6581);
		}
	}

	public static final class ReSidFpFilter8580Validator extends FilterNameValidator {

		public ReSidFpFilter8580Validator() {
			super(IFilterSection::isReSIDfpFilter8580);
		}
	}

	private final List<String> allAvailableFilterNames;

	private FilterNameValidator(Predicate<IFilterSection> filterSectionTester) {
		allAvailableFilterNames = CONFIG.getFilterSection().stream().filter(filterSectionTester)
				.map(IFilterSection::getName).collect(Collectors.toList());
	}

	@Override
	public void validate(String name, String value) throws ParameterException {
		allAvailableFilterNames.stream().filter(value::equals).findFirst()
				.orElseThrow(() -> new ParameterException("Invalid Parameter " + name + " value " + value
						+ ", expected: " + allAvailableFilterNames.stream().collect(Collectors.joining(","))));
	}

}
