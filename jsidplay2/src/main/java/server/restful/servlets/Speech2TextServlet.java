package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.JSIDPlay2Server.freeEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.MAX_SPEECH_TO_TEXT;
import static server.restful.common.filters.CounterBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.SamplingRate;
import libsidutils.AudioUtils;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.CounterBasedRateLimiterFilter;
import server.restful.common.filters.RequestLogFilter;
import sidplay.audio.wav.WAVHeader;

@SuppressWarnings("serial")
@WebServlet(name = "Speech2TextServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/speech2text")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class Speech2TextServlet extends JSIDPlay2Servlet {

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter(), new CounterBasedRateLimiterFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET, String.valueOf(MAX_SPEECH_TO_TEXT));
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
		try {
			info("Got: " + request.getContentLengthLong());
			short[] result = AudioUtils.convertToMonoWithSampleRate(request.getInputStream(), Integer.MAX_VALUE,
					SamplingRate.MEDIUM);

			ByteBuffer sampleBuffer = ByteBuffer.allocate(1024 * Short.BYTES * 1).order(ByteOrder.LITTLE_ENDIAN);

			WAVHeader wavHeader = new WAVHeader(1, SamplingRate.MEDIUM.getFrequency());
			wavHeader.advance(result.length << 1);

			File wavFile = File.createTempFile("text2speech", ".wav", configuration.getSidplay2Section().getTmpDir());

			try (OutputStream os = new FileOutputStream(wavFile)) {
				os.write(wavHeader.getBytes());

				for (short shrt : result) {
					if (!sampleBuffer.putShort(shrt).hasRemaining()) {
						try {
							os.write(sampleBuffer.array(), 0, sampleBuffer.position());
							((Buffer) sampleBuffer).clear();
						} catch (final IOException e) {
							throw new RuntimeException("Error writing WAV audio stream", e);
						}
					}
				}
				try {
					os.write(sampleBuffer.array(), 0, sampleBuffer.position());
				} catch (final IOException e) {
					throw new RuntimeException("Error writing WAV audio stream", e);
				}

			}
			info("Result: " + result.length);

			Process process = new ProcessBuilder("voice2json", "transcribe-wav", "--open", wavFile.getAbsolutePath()).start();
			int waitFlag = process.waitFor();
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {
					IOUtils.copy(process.getInputStream(), System.err);
					System.err.println("OK!");
					info("Result: OK");
				} else {
					throw new IOException("Process failed with exit code: " + returnVal);
				}
			} else {
				throw new IOException("Process failed with waitFlag: " + waitFlag);
			}

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		} finally {
			freeEntityManager();
		}
	}

}
