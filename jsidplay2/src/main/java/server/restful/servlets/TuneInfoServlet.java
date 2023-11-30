package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.util.Pair;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import ui.entities.collection.HVSCEntry;
import ui.musiccollection.SearchCriteria;

@SuppressWarnings("serial")
@WebServlet(name = "TuneInfoServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/info/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class TuneInfoServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.TuneInfoServletParameters")
	public static class TuneInfoServletParameters extends FileRequestPathServletParameters {

		@Parameter(names = "--list", arity = 1, descriptionKey = "LIST", order = -2)
		private Boolean list = Boolean.FALSE;

	}

	/**
	 * Get SID tune infos.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/info/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final TuneInfoServletParameters servletParameters = new TuneInfoServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final File file = servletParameters.fetchFile(configuration, directoryProperties, parser, servletSecurity,
					request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			Object tuneInfos = getTuneInfos(file, servletParameters.list);

			setOutput(MIME_TYPE_JSON, response, tuneInfos);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private Object getTuneInfos(final File file, final boolean asList) throws Exception {
		HVSCEntry hvscEntry = createHVSCEntry(file);

		return asList ? hvscEntry2SortedList(hvscEntry) : hvscEntry2SortedMap(hvscEntry);
	}

	private HVSCEntry createHVSCEntry(File tuneFile) throws Exception {
		SidTune tune = SidTune.load(tuneFile);

		return new HVSCEntry(() -> sidDatabase != null ? sidDatabase.getTuneLength(tune) : 0,
				getCollectionName(tuneFile), tuneFile, tune);
	}

	private List<Map<String, String>> hvscEntry2SortedList(HVSCEntry hvscEntry) {
		return hvscEntry2SortedMap(hvscEntry).entrySet().stream().map(entry -> {
			Map<String, String> map = new HashMap<>();
			map.put("Name", entry.getKey());
			map.put("Value", entry.getValue());
			return map;
		}).collect(Collectors.toList());

	}

	private TreeMap<String, String> hvscEntry2SortedMap(HVSCEntry hvscEntry) {
		List<Pair<String, String>> attributeValues = SearchCriteria.getAttributeValues(hvscEntry,
				field -> field.getAttribute().getDeclaringType().getJavaType().getSimpleName() + "."
						+ field.getAttribute().getName());
		// same order of keys as in the list
		return attributeValues.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue, (o1, o2) -> {
			throw new RuntimeException(String.format("Duplicate key for values %s and %s, I will not merge!", o1, o2));
		}, () -> new TreeMap<>((o1, o2) -> index(attributeValues, o1) - index(attributeValues, o2))));
	}

	private int index(List<Pair<String, String>> attributeValues, String o) {
		return IntStream.range(0, attributeValues.size())
				.filter(index -> Objects.equals(attributeValues.get(index).getKey(), o)).findFirst().getAsInt();
	}
}
