package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
@WebServlet(name = "FindHashServlet", displayName = "FindHashServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/hash", description = "WhatsSID tune recognition. Find hashes")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class FindHashServlet extends JSIDPlay2Servlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			IntArrayBean intArrayBean = getInput(request, IntArrayBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			HashBeans result = whatsSidService.findHashes(intArrayBean);

			setOutput(request, response, result);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		} finally {
			freeEntityManager();
		}
	}
}
