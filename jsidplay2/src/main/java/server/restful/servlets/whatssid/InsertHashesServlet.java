package server.restful.servlets.whatssid;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class InsertHashesServlet extends JSIDPlay2Servlet {

	public static final String INSERT_HASHES_PATH = "/insert-hashes";

	public InsertHashesServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + INSERT_HASHES_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPut(request);
		try {
			if (!request.isUserInRole(ROLE_ADMIN)) {
				response.sendError(SC_UNAUTHORIZED, "Only for admin user");
				return;
			}
			HashBeans hashes = getInput(request, HashBeans.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			whatsSidService.insertHashes(hashes);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}
}
