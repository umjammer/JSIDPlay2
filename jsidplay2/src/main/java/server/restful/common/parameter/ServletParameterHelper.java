package server.restful.common.parameter;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static java.util.Arrays.asList;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
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
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
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
import server.restful.servlets.StaticServlet.StaticServletParameters;
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
import ui.JSidPlay2Main.JSIDPlay2MainParameters;
import ui.tools.FingerPrintingCreator;
import ui.tools.SIDBlasterTool;

/**
 * Provides parameter default values and localization of ConvertServlet as well
 * as parameter checks to spot development errors.
 * 
 * @author ken
 *
 */
public class ServletParameterHelper {

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

	private static final List<Class<?>> MAIN_PARAMETER_CLASSES = asList(OnlineContent.class,
			JSIDPlay2ServerParameters.class, JSIDPlay2MainParameters.class, ConsolePlayer.class,
			FingerPrintingCreator.class, SIDBlasterTool.class);

	private static final List<Class<?>> SERVLET_PARAMETER_CLASSES = asList(OnKeepAliveServletParameters.class,
			ProxyServletParameters.class,
			//
			InsertNextDiskServletParameters.class, JoystickServletParameters.class, OnPlayDoneServletParameters.class,
			OnPlayServletParameters.class, PressKeyServletParameters.class,
			SetDefaultEmulationReSidFpServletParameters.class, SetDefaultEmulationReSidServletParameters.class,
			SetSidModel6581ServletParameters.class, SetSidModel8580ServletParameters.class,
			//
			ExSIDMappingServletParameters.class, HardSIDMappingServletParameters.class,
			SIDBlasterMappingServletParameters.class,
			//
			ConvertServletParameters.class, DirectoryServletParameters.class, DiskDirectoryServletParameters.class,
			DownloadServletParameters.class, PhotoServletParameters.class, StaticServletParameters.class,
			STILServletParameters.class, TuneInfoServletParameters.class);

	public static void check() {
		MAIN_PARAMETER_CLASSES.forEach(clz -> check(clz, new BeanParameterChecker(false)));
		SERVLET_PARAMETER_CLASSES.forEach(clz -> check(clz, new BeanParameterChecker(true)));
	}

	private static void check(Class<?> servletParameterClass, SimpleBeanPropertyFilter filter)
			throws ExceptionInInitializerError {
		try {
			Optional.ofNullable(servletParameterClass.getAnnotation(Parameters.class))
					.orElseThrow(() -> new IllegalAccessException("Checked class must be annotated with @Parameters"));
			createObjectMapper(filter).writerWithDefaultPrettyPrinter()
					.writeValueAsString(servletParameterClass.getDeclaredConstructor().newInstance());
		} catch (JsonProcessingException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final String FILTER_NAME = "localizer";

	@JsonFilter(FILTER_NAME)
	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = ANY, setterVisibility = ANY)
	private final class FilteredMixIn {
	}

	private static ObjectMapper createObjectMapper(SimpleBeanPropertyFilter filter) {
		return new ObjectMapper().setMixInResolver(new MixInResolver() {
			@Override
			public Class<?> findMixInClassFor(Class<?> cls) {
				return FilteredMixIn.class;
			}

			@Override
			public MixInResolver copy() {
				return this;
			}
		}).setFilterProvider(new SimpleFilterProvider().addFilter(FILTER_NAME, filter));
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
				if (parameter.names().length == 0) {
					if (serverParameter && !rootResBundle.containsKey("EXAMPLE_REQUEST_PATH")) {
						throw JsonMappingException.from(prov, "Localization missing in " + parameters.resourceBundle()
								+ ".properties (key=EXAMPLE_REQUEST_PATH)");
					}
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
					if (parameter.names().length == 0) {
						// Since ResourceBundle evaluates parent bundles as well, but we must know if
						// localization is only contained in that bundle, therefore ==
						if (serverParameter && resBundle.getString("EXAMPLE_REQUEST_PATH") == rootResBundle
								.getString("EXAMPLE_REQUEST_PATH")) {
							throw JsonMappingException.from(prov,
									"Localization missing in " + parameters.resourceBundle() + "_" + locale
											+ ".properties (key=EXAMPLE_REQUEST_PATH)");
						}
					}
				}
			} else if (writer.getAnnotation(ParametersDelegate.class) != null) {
				super.serializeAsField(pojo, jgen, prov, writer);
			}
		}

	}

}
