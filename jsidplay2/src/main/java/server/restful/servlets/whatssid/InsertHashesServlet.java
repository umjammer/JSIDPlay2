package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.MAX_FILE_SIZE;
import static server.restful.common.IServletSystemProperties.MAX_REQUEST_SIZE;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
@WebServlet(name = "InsertHashesServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/insert-hashes")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_ADMIN }))
@MultipartConfig(maxFileSize = MAX_FILE_SIZE, maxRequestSize = MAX_REQUEST_SIZE)
public class InsertHashesServlet extends JSIDPlay2Servlet {

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
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
