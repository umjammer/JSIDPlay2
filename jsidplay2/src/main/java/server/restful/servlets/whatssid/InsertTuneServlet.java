package server.restful.servlets.whatssid;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class InsertTuneServlet extends JSIDPlay2Servlet {

	public static final String INSERT_TUNE_PATH = "/insert-tune";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + INSERT_TUNE_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		return result;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			if (!request.isUserInRole(ROLE_ADMIN)) {
				response.sendError(SC_UNAUTHORIZED, "Only for admin user");
				return;
			}
			MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			IdBean idBean = whatsSidService.insertTune(musicInfoBean);

			setOutput(request, response, idBean, IdBean.class);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}
}
