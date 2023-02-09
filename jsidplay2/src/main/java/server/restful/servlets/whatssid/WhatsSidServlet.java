package server.restful.servlets.whatssid;

import static java.lang.String.valueOf;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.MAX_WHATSIDS_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.WHATSID_LOW_PRIO;
import static server.restful.common.PlayerCleanupTimerTask.count;
import static server.restful.common.filters.CounterBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class WhatsSidServlet extends JSIDPlay2Servlet {

	private static final Map<Integer, MusicInfoWithConfidenceBean> MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP = Collections
			.synchronizedMap(new LRUCache<Integer, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	public static final String WHATSSID_PATH = "/whatssid";

	public WhatsSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + WHATSSID_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new CounterBasedRateLimiterFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET, String.valueOf(MAX_WHATSIDS_IN_PARALLEL));
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
			int hashCode = wavBean.hashCode();

			MusicInfoWithConfidenceBean musicInfoWithConfidence;
			if (!MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.containsKey(hashCode) && isWhatsSidEnabled()) {
				WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());
				FingerPrinting fingerPrinting = new FingerPrinting(new IniFingerprintConfig(), whatsSidService);
				musicInfoWithConfidence = fingerPrinting.match(wavBean);
				MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.put(hashCode, musicInfoWithConfidence);
				info(valueOf(musicInfoWithConfidence));
			} else {
				musicInfoWithConfidence = MUSIC_INFO_WITH_CONFIDENCE_BEAN_MAP.get(hashCode);
				info(valueOf(musicInfoWithConfidence) + " (cached)");
			}
			setOutput(request, response, musicInfoWithConfidence, MusicInfoWithConfidenceBean.class);
		} catch (QueryTimeoutException qte) {
			warn(qte);
			response.sendError(HttpServletResponse.SC_NOT_FOUND, qte.getMessage());
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}

	/**
	 * @return if live streams are not prioritized over WhatsSid
	 */
	private boolean isWhatsSidEnabled() {
		return !WHATSID_LOW_PRIO || count() == 0;
	}

}
