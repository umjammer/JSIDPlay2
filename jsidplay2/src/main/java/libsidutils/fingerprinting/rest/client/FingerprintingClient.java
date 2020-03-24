package libsidutils.fingerprinting.rest.client;

import static javax.servlet.http.HttpServletRequest.BASIC_AUTH;
import static server.restful.servlets.whatssid.FindHashServlet.FIND_HASH_PATH;
import static server.restful.servlets.whatssid.FindTuneServlet.FIND_TUNE_PATH;
import static server.restful.servlets.whatssid.WhatsSidServlet.IDENTIFY_PATH;
import static server.restful.servlets.whatssid.InsertHashesServlet.INSERT_HASHES_PATH;
import static server.restful.servlets.whatssid.InsertTuneServlet.INSERT_TUNE_PATH;
import static server.restful.servlets.whatssid.TuneExistsServlet.TUNE_EXISTS_PATH;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import libsidplay.config.IConfig;
import libsidutils.fingerprinting.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;
import server.restful.common.MimeType;

public class FingerprintingClient implements FingerPrintingDataSource {

	// TODO configuration
	private static final String PATH = "http://127.0.0.1:8080/jsidplay2service/JSIDPlay2REST";
	private static final String USERNAME = "jsidplay2";
	private static final String PASSWORD = "jsidplay2!";

	@SuppressWarnings("unused")
	private IConfig configuration;

	public FingerprintingClient(IConfig configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		try {
			HttpURLConnection connection = send(musicInfoBean, MusicInfoBean.class, INSERT_TUNE_PATH, HttpMethod.PUT);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(IdBean.class, connection);
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void insertHashes(HashBeans hashBeans) {
		try {
			HttpURLConnection connection = send(hashBeans, HashBeans.class, INSERT_HASHES_PATH, HttpMethod.PUT);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return;
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public HashBeans findHashes(IntArrayBean intArray) {
		try {
			HttpURLConnection connection = send(intArray, IntArrayBean.class, FIND_HASH_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(HashBeans.class, connection);
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MusicInfoBean findTune(SongNoBean songNoBean) {
		try {
			HttpURLConnection connection = send(songNoBean, SongNoBean.class, FIND_TUNE_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(MusicInfoBean.class, connection);
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean tuneExists(MusicInfoBean musicInfoBean) {
		try {
			HttpURLConnection connection = send(musicInfoBean, MusicInfoBean.class, TUNE_EXISTS_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(Boolean.class, connection);
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MusicInfoWithConfidenceBean whatsSid(WavBean wavBean) {
		try {
			HttpURLConnection connection = send(wavBean, WavBean.class, IDENTIFY_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(MusicInfoWithConfidenceBean.class, connection);
			}
			throw new RuntimeException(connection.getResponseCode() + "\n" + connection.getResponseMessage());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> HttpURLConnection send(T parameter, Class<T> tClass, String requestPath, String requestMethod)
			throws MalformedURLException, IOException, ProtocolException, JAXBException {

		HttpURLConnection connection = (HttpURLConnection) new URL(PATH + requestPath).openConnection();
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTH + " "
				+ Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)));
		connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MimeType.MIME_TYPE_XML.getContentType());
		connection.setRequestProperty(HttpHeaders.ACCEPT, MimeType.MIME_TYPE_XML.getContentType());

		JAXBContext.newInstance(tClass).createMarshaller().marshal(parameter, connection.getOutputStream());
		connection.getOutputStream().flush();

		return connection;
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Class<T> theClass, HttpURLConnection connection) {
		try {
			if (connection.getContentLength() == 0) {
				return null;
			}
			Object obj = JAXBContext.newInstance(theClass).createUnmarshaller().unmarshal(connection.getInputStream());
			if (theClass.isInstance(obj)) {
				return (T) obj;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			connection.disconnect();
		}
	}

}
