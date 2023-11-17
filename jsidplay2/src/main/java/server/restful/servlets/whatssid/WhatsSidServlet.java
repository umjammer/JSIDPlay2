package server.restful.servlets.whatssid;

import static server.restful.common.IServletSystemProperties.*;
import static server.restful.common.IServletSystemProperties.WHATSSIDSERVLET_MAX_REQUEST_SIZE;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static java.lang.String.valueOf;
import static libsidplay.config.IWhatsSidSystemProperties.MAX_SECONDS;
import static libsidplay.config.IWhatsSidSystemProperties.UPLOAD_MAXIMUM_SECONDS;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.MAX_WHATSIDS_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.WHATSID_LOW_PRIO;
import static server.restful.common.IServletSystemProperties.WHATSSID_ASYNC_TIMEOUT;
import static server.restful.common.filters.RTMPBasedRateLimiterFilter.FILTER_PARAMETER_MAX_RTMP_PER_SERVLET;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.QueryTimeoutException;

import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.LRUCache;
import server.restful.common.async.DefaultThreadFactory;
import server.restful.common.async.HttpAsyncContextRunnable;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
@WebServlet(name = "WhatsSidServlet", asyncSupported = true, urlPatterns = CONTEXT_ROOT_SERVLET + "/whatssid")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
@MultipartConfig(maxFileSize = WHATSSIDSERVLET_MAX_FILE_SIZE, maxRequestSize = WHATSSIDSERVLET_MAX_REQUEST_SIZE, fileSizeThreshold = WHATSSIDSERVLET_FILE_SIZE_THRESHOLD)
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<Integer, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<Integer, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	private ExecutorService executorService;

	@Override
	public void init() throws ServletException {
		executorService = Executors.newFixedThreadPool(MAX_WHATSIDS_IN_PARALLEL, new DefaultThreadFactory("/whatssid"));
	}

	@Override
	public void destroy() {
		executorService.shutdown();
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

		AsyncContext asyncContext = request.startAsync(request, response);
		asyncContext.setTimeout(WHATSSID_ASYNC_TIMEOUT);

		executorService.execute(new HttpAsyncContextRunnable(asyncContext, this) {

			public void execute() throws IOException {
				try {
					WAVBean wavBean = getInput(getRequest(), WAVBean.class);
					wavBean.setMaxSeconds(
							ServletFileUpload.isMultipartContent(getRequest()) ? UPLOAD_MAXIMUM_SECONDS : MAX_SECONDS);

					int hashCode = getRequest().getRemoteAddr().hashCode() ^ wavBean.hashCode();
					MusicInfoWithConfidenceBean musicInfoWithConfidence;
					if (MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.containsKey(hashCode)) {
						musicInfoWithConfidence = MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.get(hashCode);
						info(valueOf(musicInfoWithConfidence) + " (cached)", parentThread);
					} else {
						musicInfoWithConfidence = match(getEntityManager(), wavBean);
						MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(hashCode, musicInfoWithConfidence);
						info(valueOf(musicInfoWithConfidence), parentThread);
					}
					if (getResponse() != null) {
						setOutput(getRequest(), getResponse(), musicInfoWithConfidence,
								MusicInfoWithConfidenceBean.class);
					}
				} catch (QueryTimeoutException qte) {
					warn(qte.getClass().getName(), parentThread);
					if (getResponse() != null) {
						getResponse().sendError(SC_SERVICE_UNAVAILABLE, qte.getClass().getName());
					}
				} catch (Throwable t) {
					warn(t.getMessage(), parentThread);
					if (getResponse() != null) {
						getResponse().setStatus(SC_INTERNAL_SERVER_ERROR);
						setOutput(getResponse(), MIME_TYPE_TEXT, t);
					}
				} finally {
					freeEntityManager();
				}
			}

			private MusicInfoWithConfidenceBean match(EntityManager entityManager, WAVBean wavBean) throws IOException {
				WhatsSidService whatsSidService = new WhatsSidService(entityManager);
				FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
				return fingerPrinting.match(wavBean);
			}

		});
	}

}
