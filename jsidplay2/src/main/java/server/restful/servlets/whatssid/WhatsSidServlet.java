package server.restful.servlets.whatssid;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static libsidplay.config.IWhatsSidSystemProperties.FRAME_MAX_LENGTH;
import static libsidplay.config.IWhatsSidSystemProperties.UPLOAD_FRAME_MAXIMUM_LENGTH;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.WHATSID_LOW_PRIO;
import static server.restful.common.filters.RTMPBasedRateLimiterFilter.FILTER_PARAMETER_MAX_RTMP_PER_SERVLET;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.QueryTimeoutException;

import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.HttpAsyncContextRunnable;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.LRUCache;
import server.restful.common.filters.RTMPBasedRateLimiterFilter;
import server.restful.common.filters.RequestLogFilter;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
@WebServlet(name = "WhatsSidServlet", asyncSupported = true, urlPatterns = CONTEXT_ROOT_SERVLET + "/whatssid")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<Integer, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<Integer, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter(), new RTMPBasedRateLimiterFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_MAX_RTMP_PER_SERVLET, String.valueOf(WHATSID_LOW_PRIO ? 1 : Integer.MAX_VALUE));
		return result;
	}

	/**
	 * WhatsSID? (SID tune recognition).
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/whatssid
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		final Thread parentThread = currentThread();
		final AsyncContext asyncContext = request.startAsync();
		asyncContext.start(new HttpAsyncContextRunnable(asyncContext, this, parentThread) {

			public void execute() throws IOException {
				try {
					WAVBean wavBean = getInput(getRequest(), WAVBean.class);

					int hashCode = getRequest().getRemoteAddr().hashCode() ^ wavBean.hashCode();
					MusicInfoWithConfidenceBean musicInfoWithConfidence;
					if (MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.containsKey(hashCode)) {
						musicInfoWithConfidence = MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.get(hashCode);
						info(valueOf(musicInfoWithConfidence) + " (cached)", parentThread);
					} else {
						musicInfoWithConfidence = match(getRequest(), getEntityManager(), wavBean);
						MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(hashCode, musicInfoWithConfidence);
						info(valueOf(musicInfoWithConfidence), parentThread);
					}
					setOutput(getRequest(), getResponse(), musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
				} catch (QueryTimeoutException qte) {
					warn(qte.getClass().getName(), parentThread);
					getResponse().sendError(SC_SERVICE_UNAVAILABLE, qte.getClass().getName());
				} catch (Throwable t) {
					warn(t.getMessage(), parentThread);
					getResponse().setStatus(SC_INTERNAL_SERVER_ERROR);
					setOutput(getResponse(), MIME_TYPE_TEXT, t);
				} finally {
					freeEntityManager();
				}
			}

		});
	}

	private MusicInfoWithConfidenceBean match(HttpServletRequest request, EntityManager entityManager, WAVBean wavBean)
			throws IOException {
		WhatsSidService whatsSidService = new WhatsSidService(entityManager);
		FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
		wavBean.setFrameMaxLength(
				ServletFileUpload.isMultipartContent(request) ? UPLOAD_FRAME_MAXIMUM_LENGTH : FRAME_MAX_LENGTH);
		return fingerPrinting.match(wavBean);
	}

}
