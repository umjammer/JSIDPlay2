package server.restful.servlets.whatssid;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class TuneExistsServlet extends JSIDPlay2Servlet {

	public static final String TUNE_EXISTS_PATH = "/tune-exists";

	public TuneExistsServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + TUNE_EXISTS_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

			final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
			Boolean exists = whatsSidService.tuneExists(musicInfoBean);

			setOutput(request, response, exists, Boolean.class);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}
}
