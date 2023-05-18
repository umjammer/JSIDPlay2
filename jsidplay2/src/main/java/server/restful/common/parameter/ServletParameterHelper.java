package server.restful.common.parameter;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import build.OnlineContent;
import server.restful.JSIDPlay2Server.JSIDPlay2ServerParameters;
import server.restful.servlets.ConvertServlet;
import server.restful.servlets.ConvertServlet.ConvertServletParameters;
import server.restful.servlets.DirectoryServlet.DirectoryServletParameters;
import server.restful.servlets.DiskDirectoryServlet.DiskDirectoryServletParameters;
import server.restful.servlets.DownloadServlet.DownloadServletParameters;
import server.restful.servlets.PhotoServlet.PhotoServletParameters;
import server.restful.servlets.STILServlet.STILServletParameters;
import server.restful.servlets.TuneInfoServlet.TuneInfoServletParameters;
import server.restful.servlets.hls.OnKeepAliveServlet.OnKeepAliveServletParameters;
import server.restful.servlets.hls.ProxyServlet.ProxyServletParameters;
import server.restful.servlets.rtmp.InsertNextDiskServlet.InsertNextDiskServletParameters;
import server.restful.servlets.rtmp.JoystickServlet.JoystickServletParameters;
import server.restful.servlets.rtmp.OnPlayDoneServlet.OnPlayDoneServletParameters;
import server.restful.servlets.rtmp.OnPlayServlet.OnPlayServletParameters;
import server.restful.servlets.rtmp.PressKeyServlet.PressKeyServletParameters;
import server.restful.servlets.rtmp.SetDefaultEmulationReSidFpServlet.SetDefaultEmulationReSidFpServletParameters;
import server.restful.servlets.rtmp.SetDefaultEmulationReSidServlet.SetDefaultEmulationReSidServletParameters;
import server.restful.servlets.rtmp.SetSidModel6581Servlet.SetSidModel6581ServletParameters;
import server.restful.servlets.rtmp.SetSidModel8580Servlet.SetSidModel8580ServletParameters;
import server.restful.servlets.sidmapping.ExSIDMappingServlet.ExSIDMappingServletParameters;
import server.restful.servlets.sidmapping.HardSIDMappingServlet.HardSIDMappingServletParameters;
import server.restful.servlets.sidmapping.SIDBlasterMappingServlet.SIDBlasterMappingServletParameters;
import sidplay.ConsolePlayer;
import sidplay.ini.IniAudioSection;
import sidplay.ini.IniC1541Section;
import sidplay.ini.IniConfig;
import sidplay.ini.IniConsoleSection;
import sidplay.ini.IniEmulationSection;
import sidplay.ini.IniFilterSection;
import sidplay.ini.IniPrinterSection;
import sidplay.ini.IniSidplay2Section;
import sidplay.ini.IniWhatsSidSection;
import ui.JSidPlay2Main.JSIDPlay2MainParameters;
import ui.tools.FingerPrintingCreator;
import ui.tools.SIDBlasterTool;

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

	public static void check(Class<?> servletParameterClass) {
		try {
			Optional.ofNullable(servletParameterClass.getAnnotation(Parameters.class))
					.orElseThrow(() -> new IllegalAccessException("Checked class must be annotated with @Parameters"));
			createObjectMapper(new BeanParameterChecker(
					servletParameterClass.getPackage().getName().startsWith("server.restful.servlets")))
					.writerWithDefaultPrettyPrinter()
					.writeValueAsString(servletParameterClass.getDeclaredConstructor().newInstance());
		} catch (JsonProcessingException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static ObjectMapper createObjectMapper(SimpleBeanPropertyFilter filter) {
		return new ObjectMapper().addMixIn(OnlineContent.class, OnlineContentMixIn.class)
				.addMixIn(JSIDPlay2ServerParameters.class, JSIDPlay2ServerParametersMixIn.class)
				.addMixIn(ConsolePlayer.class, ConsolePlayerMixIn.class)
				.addMixIn(FingerPrintingCreator.class, FingerPrintingCreatorMixIn.class)
				.addMixIn(SIDBlasterTool.class, SIDBlasterToolMixIn.class)
				.addMixIn(JSIDPlay2MainParameters.class, JSIDPlay2MainParametersMixIn.class)
				.addMixIn(DirectoryServletParameters.class, DirectoryServletParametersMixIn.class)
				.addMixIn(DiskDirectoryServletParameters.class, DiskDirectoryServletParametersMixIn.class)
				.addMixIn(TuneInfoServletParameters.class, TuneInfoServletParametersMixIn.class)
				.addMixIn(PhotoServletParameters.class, PhotoServletParametersMixIn.class)
				.addMixIn(ConvertServletParameters.class, ConvertServletParametersMixIn.class)
				.addMixIn(DownloadServletParameters.class, DownloadServletParametersMixIn.class)
				.addMixIn(OnKeepAliveServletParameters.class, OnKeepAliveServletParametersMixIn.class)
				.addMixIn(OnPlayDoneServletParameters.class, OnPlayDoneServletParametersMixIn.class)
				.addMixIn(OnPlayServletParameters.class, OnPlayServletParametersMixIn.class)
				.addMixIn(InsertNextDiskServletParameters.class, InsertNextDiskServletParametersMixIn.class)
				.addMixIn(SetSidModel6581ServletParameters.class, SetSidModel6581ServletParametersMixIn.class)
				.addMixIn(SetSidModel8580ServletParameters.class, SetSidModel8580ServletParametersMixIn.class)
				.addMixIn(SetDefaultEmulationReSidServletParameters.class,
						SetDefaultEmulationReSidServletParametersMixIn.class)
				.addMixIn(SetDefaultEmulationReSidFpServletParameters.class,
						SetDefaultEmulationReSidFpServletParametersMixIn.class)
				.addMixIn(PressKeyServletParameters.class, PressKeyServletParametersMixIn.class)
				.addMixIn(JoystickServletParameters.class, JoystickServletParametersMixIn.class)
				.addMixIn(ProxyServletParameters.class, ProxyServletParametersMixIn.class)
				.addMixIn(STILServletParameters.class, STILServletParametersMixIn.class)
				.addMixIn(HardSIDMappingServletParameters.class, HardSIDMappingServletParametersMixIn.class)
				.addMixIn(ExSIDMappingServletParameters.class, ExSIDMappingServletParametersMixIn.class)
				.addMixIn(SIDBlasterMappingServletParameters.class, SIDBlasterMappingServletParametersMixIn.class)
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

	private static final class BeanParameterLocalizer extends SimpleBeanPropertyFilter {

		private final Locale locale;

		private BeanParameterLocalizer(Locale locale) {
			this.locale = locale;
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameters != null && parameter != null) {
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

		private boolean serverParameter;

		private BeanParameterChecker(boolean serverParameter) {
			this.serverParameter = serverParameter;
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameters != null && parameter != null) {
				for (String name : parameter.names()) {
					// check parameter name length
					if (name.startsWith("--")) {
						if (name.length() <= 3) {
							throw JsonMappingException.from(prov,
									"name prefixed by '--' must be at least two characters long");
						}
					} else if (name.startsWith("-")) {
						if (name.length() > 2) {
							throw JsonMappingException.from(prov,
									"name prefixed by '-' must not be more than one character long");
						}
					} else {
						throw JsonMappingException.from(prov, "name must be prefixed by '-' or '--'");
					}
				}
				// check parameter order
				if (orders.contains(parameter.order())) {
					throw JsonMappingException.from(prov, "Ambigous order " + parameter.order());
				}
				orders.add(parameter.order());
				// check arity of boolean parameter
				if (serverParameter
						&& Stream.of(Boolean.class, boolean.class).anyMatch(writer.getType().getRawClass()::equals)
						&& parameter.arity() != 1) {
					throw JsonMappingException.from(prov, "Arity must be 1, but is " + parameter.arity());
				}
				// check missing localization
				ResourceBundle rootResBundle = ResourceBundle.getBundle(parameters.resourceBundle(), Locale.ROOT);
				if (!parameter.descriptionKey().isEmpty() && !rootResBundle.containsKey(parameter.descriptionKey())) {
					throw JsonMappingException.from(prov, "Localization missing in " + parameters.resourceBundle()
							+ ".properties (key=" + parameter.descriptionKey() + ")");
				}
				for (Locale locale : OTHER_LOCALES) {
					ResourceBundle resBundle = ResourceBundle.getBundle(parameters.resourceBundle(), locale);
					// Since ResourceBundle evaluates parent bundles as well, but we must know if
					// localization is only contained in that bundle, therefore ==
					if (!parameter.descriptionKey().isEmpty()
							&& resBundle.getString(parameter.descriptionKey()) == rootResBundle
									.getString(parameter.descriptionKey())) {
						throw JsonMappingException.from(prov, "Localization missing in " + parameters.resourceBundle()
								+ "_" + locale + ".properties (key=" + parameter.descriptionKey() + ")");
					}
				}
			} else if (writer.getAnnotation(ParametersDelegate.class) != null) {
				super.serializeAsField(pojo, jgen, prov, writer);
			}
		}

	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class OnlineContentMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class JSIDPlay2ServerParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class ConsolePlayerMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class FingerPrintingCreatorMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SIDBlasterToolMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class JSIDPlay2MainParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class DirectoryServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class DiskDirectoryServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class TuneInfoServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class PhotoServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class ConvertServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class DownloadServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class OnKeepAliveServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class OnPlayDoneServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class OnPlayServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class InsertNextDiskServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SetSidModel6581ServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SetSidModel8580ServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SetDefaultEmulationReSidServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SetDefaultEmulationReSidFpServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class PressKeyServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class JoystickServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class ProxyServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class STILServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class HardSIDMappingServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class ExSIDMappingServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class SIDBlasterMappingServletParametersMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniConfigMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniSidplay2SectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniAudioSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniEmulationSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniC1541SectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniPrinterSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniConsoleSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniWhatsSidSectionMixIn {
	}

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class IniFilterSectionMixIn {
	}

}
