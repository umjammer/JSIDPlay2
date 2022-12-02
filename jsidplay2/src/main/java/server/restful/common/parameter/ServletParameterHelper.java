package server.restful.common.parameter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import server.restful.servlets.ConvertServlet;
import server.restful.servlets.ConvertServlet.ConvertServletParameters;
import sidplay.ini.IniAudioSection;
import sidplay.ini.IniC1541Section;
import sidplay.ini.IniConfig;
import sidplay.ini.IniConsoleSection;
import sidplay.ini.IniEmulationSection;
import sidplay.ini.IniFilterSection;
import sidplay.ini.IniPrinterSection;
import sidplay.ini.IniSidplay2Section;
import sidplay.ini.IniWhatsSidSection;

public class ServletParameterHelper {

	private static final String FILTER_NAME = "localizer";

	public static final String CONVERT_MESSAGES_EN;
	public static final String CONVERT_MESSAGES_DE;
	public static final String CONVERT_OPTIONS;
	static {
		try {
			CONVERT_OPTIONS = new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(new ConvertServlet.ConvertServletParameters());
			CONVERT_MESSAGES_EN = createObjectMapper(new BeanParameterLocalizer(Locale.ROOT))
					.writerWithDefaultPrettyPrinter().writeValueAsString(new ConvertServlet.ConvertServletParameters());
			CONVERT_MESSAGES_DE = createObjectMapper(new BeanParameterLocalizer(Locale.GERMAN))
					.writerWithDefaultPrettyPrinter().writeValueAsString(new ConvertServlet.ConvertServletParameters());
		} catch (JsonProcessingException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final class BeanParameterLocalizer extends SimpleBeanPropertyFilter {

		private final Locale locale;

		public BeanParameterLocalizer(Locale locale) {
			this.locale = locale;
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameters != null && parameter != null && parameter.descriptionKey() != null) {
				ResourceBundle resBundle = ResourceBundle.getBundle(parameters.resourceBundle(), locale);
				jgen.writeStringField(writer.getName(), resBundle.getString(parameter.descriptionKey()));
			} else if (writer.getAnnotation(ParametersDelegate.class) != null) {
				super.serializeAsField(pojo, jgen, prov, writer);
			}
		}
	}

	private static final class BeanParameterChecker extends SimpleBeanPropertyFilter {

		private static final Locale[] OTHER_LOCALES = new Locale[] { Locale.GERMAN };

		private final Set<Integer> orders = new HashSet<>();

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameters != null && parameter != null && parameter.descriptionKey() != null) {
				if (orders.contains(parameter.order())) {
					throw new Exception("Ambigous order attribute on parameter: " + parameter.order());
				}
				orders.add(parameter.order());

				ResourceBundle rootResBundle = ResourceBundle.getBundle(parameters.resourceBundle(), Locale.ROOT);
				if (!rootResBundle.containsKey(parameter.descriptionKey())) {
					throw new Exception("Localization missing on parameter: " + parameter.descriptionKey());
				}
				for (Locale locale : OTHER_LOCALES) {
					ResourceBundle resBundle = ResourceBundle.getBundle(parameters.resourceBundle(), locale);
					// Since ResourceBundle evaluates parent bundles as well, but we must know if
					// localization is only contained in that bundle, therefore ==
					if (resBundle.getString(parameter.descriptionKey()) == rootResBundle
							.getString(parameter.descriptionKey())) {
						throw new Exception("Localization missing on parameter: " + parameter.descriptionKey());
					}
				}

			} else if (writer.getAnnotation(ParametersDelegate.class) != null) {
				super.serializeAsField(pojo, jgen, prov, writer);
			}
		}
	}

	public static void check(Class<?> servletParameterClass) {
		try {
			createObjectMapper(new BeanParameterChecker()).writerWithDefaultPrettyPrinter()
					.writeValueAsString(servletParameterClass.getDeclaredConstructor().newInstance());
		} catch (JsonProcessingException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@JsonFilter(FILTER_NAME)
	private class ServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniConfigMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniSidplay2SectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniAudioSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniEmulationSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniC1541SectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniPrinterSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniConsoleSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniWhatsSidSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	private class IniFilterSectionMixIn {
	}

	private static ObjectMapper createObjectMapper(SimpleBeanPropertyFilter filter) {
		return new ObjectMapper().addMixIn(ConvertServletParameters.class, ServletParametersMixIn.class)
				.addMixIn(IniConfig.class, IniConfigMixIn.class)
				.addMixIn(IniSidplay2Section.class, IniSidplay2SectionMixIn.class)
				.addMixIn(IniAudioSection.class, IniAudioSectionMixIn.class)
				.addMixIn(IniEmulationSection.class, IniEmulationSectionMixIn.class)
				.addMixIn(IniC1541Section.class, IniC1541SectionMixIn.class)
				.addMixIn(IniPrinterSection.class, IniPrinterSectionMixIn.class)
				.addMixIn(IniConsoleSection.class, IniConsoleSectionMixIn.class)
				.addMixIn(IniWhatsSidSection.class, IniWhatsSidSectionMixIn.class)
				.addMixIn(IniFilterSection.class, IniFilterSectionMixIn.class)
				.setFilterProvider(new SimpleFilterProvider().addFilter(FILTER_NAME, filter));
	}

}
