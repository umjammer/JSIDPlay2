package server.restful.servlets.whatssid;

import static java.lang.String.valueOf;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.MAX_WHATSIDS_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.WHATSID_LOW_PRIO;
import static server.restful.common.filters.CounterBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET;
import static server.restful.common.filters.PlayerBasedRateLimiterFilter.FILTER_PARAMETER_MAX_RTMP_PER_SERVLET;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.QueryTimeoutException;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.LRUCache;
import server.restful.common.filters.CounterBasedRateLimiterFilter;
import server.restful.common.filters.PlayerBasedRateLimiterFilter;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<Integer, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<Integer, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	public static final String WHATSSID_PATH = "/whatssid";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + WHATSSID_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new CounterBasedRateLimiterFilter(), new PlayerBasedRateLimiterFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET, String.valueOf(MAX_WHATSIDS_IN_PARALLEL));
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
		super.doPost(request);
		try {
			WAVBean wavBean = getInput(request, WAVBean.class);

			int hashCode = request.getRemoteAddr().hashCode() ^ wavBean.hashCode();
			MusicInfoWithConfidenceBean musicInfoWithConfidence = match(getEntityManager(), wavBean);
			MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(hashCode, musicInfoWithConfidence);
			info(valueOf(musicInfoWithConfidence));

			setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
		} catch (QueryTimeoutException qte) {
			warn(qte.getClass().getName());
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, qte.getClass().getName());
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}

	private MusicInfoWithConfidenceBean match(EntityManager entityManager, WAVBean wavBean) throws IOException {
		WhatsSidService whatsSidService = new WhatsSidService(entityManager);
		FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
		return fingerPrinting.match(wavBean);
	}

}
