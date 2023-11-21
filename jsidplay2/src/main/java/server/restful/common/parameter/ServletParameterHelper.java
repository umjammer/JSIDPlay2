package server.restful.common.parameter;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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

import libsidutils.IOUtils;
import server.restful.servlets.ConvertServlet;

/**
 * Provides parameter default values and localization of ConvertServlet as well
 * as parameter checks to spot development errors.
 * 
 * @author ken
 *
 */
public class ServletParameterHelper {

	private static final Logger LOG = Logger.getLogger(ServletParameterHelper.class.getName());

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

	public static void check(Class<?> servletParameterClass, boolean servletParameter)
			throws ExceptionInInitializerError {
		try {
			LOG.info(servletParameterClass.getName());
			Optional.ofNullable(servletParameterClass.getAnnotation(Parameters.class))
					.orElseThrow(() -> new IllegalAccessException("Checked class must be annotated with @Parameters"));
			Constructor<?> declaredConstructor = servletParameterClass.getDeclaredConstructor();
			declaredConstructor.setAccessible(true);
			createObjectMapper(new BeanParameterChecker(servletParameter)).writerWithDefaultPrettyPrinter()
					.writeValueAsString(declaredConstructor.newInstance());
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

		private static final CharsetDecoder US_ASCII_DECODER = StandardCharsets.US_ASCII.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT);

		private static final String ILLEGAL_CHARACTERS_IN_RESOURCE_NAME = "Illegal characters in resource=%s (Expected US_ASCII or unicode escape sequences prefixed by \\u)\nlineNo=%d,colNo=%d";

		private static final Locale[] OTHER_LOCALES = new Locale[] { Locale.GERMAN };

		private final Set<Integer> orders = new HashSet<>();

		private final Set<String> checkedResourceBundles = new HashSet<>();

		private final boolean servletParameter;

		private BeanParameterChecker(boolean servletParameter) {
			this.servletParameter = servletParameter;
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
				throws Exception {
			Parameters parameters = pojo.getClass().getAnnotation(Parameters.class);
			Parameter parameter = writer.getAnnotation(Parameter.class);
			if (parameter != null) {
				if (servletParameter) {
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
				}
				// check parameter order
				if (!orders.add(parameter.order())) {
					throw JsonMappingException.from(prov, "Ambigous order " + parameter.order());
				}
				// check arity of boolean parameter
				if (servletParameter
						&& Stream.of(Boolean.class, boolean.class).anyMatch(writer.getType().getRawClass()::equals)
						&& parameter.arity() != 1) {
					throw JsonMappingException.from(prov, "Arity must be 1, but is " + parameter.arity());
				}
				// Check encoding
				checkEncoding(parameters.resourceBundle(), Locale.ROOT);
				// check missing localization
				ResourceBundle rootResBundle = ResourceBundle.getBundle(parameters.resourceBundle(), Locale.ROOT);
				if (!parameter.descriptionKey().isEmpty() && !rootResBundle.containsKey(parameter.descriptionKey())) {
					throw JsonMappingException.from(prov, "Localization missing in " + parameters.resourceBundle()
							+ ".properties (key=" + parameter.descriptionKey() + ")");
				}
				if (parameter.names().length == 0) {
					if (servletParameter && !rootResBundle.containsKey("EXAMPLE_REQUEST_PATH")) {
						throw JsonMappingException.from(prov, "Localization missing in " + parameters.resourceBundle()
								+ ".properties (key=EXAMPLE_REQUEST_PATH)");
					}
				}
				for (Locale locale : OTHER_LOCALES) {
					// Check encoding
					checkEncoding(parameters.resourceBundle(), locale);
					// check missing localization
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
						if (servletParameter && resBundle.getString("EXAMPLE_REQUEST_PATH") == rootResBundle
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

		private void checkEncoding(String resourceBundleName, Locale locale) throws IOException, Exception {
			String resourceName = "/" + resourceBundleName.replace('.', '/')
					+ (Locale.ROOT.equals(locale) ? "" : "_" + locale) + ".properties";
			if (checkedResourceBundles.add(resourceName)) {
				LOG.fine(String.format("Check encoding of %s", resourceName));

				ByteBuffer byteBuffer = null;
				byte[] byteArray = null;
				try {
					byteArray = IOUtils.readAllBytes(getClass().getResourceAsStream(resourceName));
					byteBuffer = ByteBuffer.wrap(byteArray);
					US_ASCII_DECODER.decode(byteBuffer);
				} catch (CharacterCodingException e) {
					Entry<Integer, Integer> lineColumn = getLineColumn(byteArray, byteBuffer.position());
					LOG.log(Level.SEVERE, String.format(ILLEGAL_CHARACTERS_IN_RESOURCE_NAME, resourceName,
							lineColumn.getKey(), lineColumn.getValue()), e);
					throw e;
				} catch (IOException e) {
					LOG.log(Level.SEVERE, String.format("checkEncoding failed for %s", resourceName), e);
					throw e;
				}
			}
		}

		private Map.Entry<Integer, Integer> getLineColumn(byte[] byteArray, int bytePosition) {
			int currentPos = 0, colNo = 1, lineNo = 1;
			for (byte b : byteArray) {
				if (currentPos == bytePosition) {
					break;
				}
				if (b == '\n') {
					lineNo++;
					colNo = 0;
				}
				colNo++;
				currentPos++;
			}
			return new SimpleEntry<>(lineNo, colNo);
		}

	}

}
