package server.restful.common;

import java.util.Locale;
import java.util.ResourceBundle;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import server.restful.servlets.ConvertServlet;
import sidplay.ini.IniAudioSection;
import sidplay.ini.IniC1541Section;
import sidplay.ini.IniConsoleSection;
import sidplay.ini.IniEmulationSection;
import sidplay.ini.IniFilterSection;
import sidplay.ini.IniPrinterSection;
import sidplay.ini.IniSidplay2Section;
import sidplay.ini.IniWhatsSidSection;

public class ServletParameterHelper {

	private static final class LocalizedBeanParameters extends SimpleBeanPropertyFilter {

		private final Locale locale;

		public LocalizedBeanParameters(Locale locale) {
			this.locale = locale;
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameter != null) {
				Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
				if (parameters != null) {
					ResourceBundle resBundle = ResourceBundle.getBundle(parameters.resourceBundle(), locale);
					jgen.writeStringField(writer.getName(), resBundle.getString(parameter.descriptionKey()));
				}
			}
		}
	}

	@JsonFilter("localized")
	public class IniSidplay2SectionMixIn {
	}

	@JsonFilter("localized")
	public class IniAudioSectionMixIn {
	}

	@JsonFilter("localized")
	public class IniEmulationSectionMixIn {
	}

	@JsonFilter("localized")
	public class IniC1541SectionMixIn {
	}

	@JsonFilter("localized")
	public class IniPrinterSectionMixIn {
	}

	@JsonFilter("localized")
	public class IniConsoleSectionMixIn {
	}

	@JsonFilter("localized")
	public class IniWhatsSidSectionMixIn {
	}

	@JsonFilter("localized")
	public class IniFilterSectionMixIn {
	}

	public static final String CONVERT_MESSAGES_EN;
	public static final String CONVERT_MESSAGES_DE;
	public static final String CONVERT_OPTIONS;
	static {
		try {
			CONVERT_OPTIONS = new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(new ConvertServlet.ServletParameters().getConfig());
			CONVERT_MESSAGES_EN = createObjectMapper(Locale.ROOT).writerWithDefaultPrettyPrinter()
					.writeValueAsString(new ConvertServlet.ServletParameters().getConfig());
			CONVERT_MESSAGES_DE = createObjectMapper(Locale.GERMAN).writerWithDefaultPrettyPrinter()
					.writeValueAsString(new ConvertServlet.ServletParameters().getConfig());
		} catch (JsonProcessingException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static ObjectMapper createObjectMapper(Locale locale) {
		return new ObjectMapper().addMixIn(IniSidplay2Section.class, IniSidplay2SectionMixIn.class)
				.addMixIn(IniAudioSection.class, IniAudioSectionMixIn.class)
				.addMixIn(IniEmulationSection.class, IniEmulationSectionMixIn.class)
				.addMixIn(IniC1541Section.class, IniC1541SectionMixIn.class)
				.addMixIn(IniPrinterSection.class, IniPrinterSectionMixIn.class)
				.addMixIn(IniConsoleSection.class, IniConsoleSectionMixIn.class)
				.addMixIn(IniWhatsSidSection.class, IniWhatsSidSectionMixIn.class)
				.addMixIn(IniFilterSection.class, IniFilterSectionMixIn.class).setFilterProvider(
						new SimpleFilterProvider().addFilter("localized", new LocalizedBeanParameters(locale)));
	}

}
