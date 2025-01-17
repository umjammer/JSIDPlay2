package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.IServletSystemProperties.MAX_SPEECH_TO_TEXT;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.filters.CounterBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.SamplingRate;
import libsidutils.AudioUtils;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.CounterBasedRateLimiterFilter;
import sidplay.audio.wav.WAVHeader;

@SuppressWarnings("serial")
@WebServlet(name = "SpeechToTextServlet", displayName = "SpeechToTextServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/speech2text", description = "Speech recognition")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class SpeechToTextServlet extends JSIDPlay2Servlet {

	@Override
	public Map<Class<? extends HttpFilter>, Map<String, String>> getServletFiltersParameterMap() {
		Map<Class<? extends HttpFilter>, Map<String, String>> result = new HashMap<>();

		Map<String, String> counterBasedRateLimiterFilterParameters = new HashMap<>();
		counterBasedRateLimiterFilterParameters.put(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET,
				String.valueOf(MAX_SPEECH_TO_TEXT));
		result.put(CounterBasedRateLimiterFilter.class, counterBasedRateLimiterFilterParameters);

		return result;
	}

	/**
	 * Speech recognition.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/speech2text
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		File wavFile = null;
		try {
			wavFile = File.createTempFile("speech2text", ".wav", configuration.getSidplay2Section().getTmpDir());

			info(getServletContext(), "Got: " + request.getContentLengthLong());

			SamplingRate targetSampleRate = SamplingRate.LOW;
			short[] samples = AudioUtils.convertToMonoWithSampleRate(request.getInputStream(), Integer.MAX_VALUE,
					targetSampleRate);

			ByteBuffer sampleBuffer = ByteBuffer.allocate(targetSampleRate.getFrequency() * Short.BYTES)
					.order(ByteOrder.LITTLE_ENDIAN);

			WAVHeader wavHeader = new WAVHeader(1, targetSampleRate.getFrequency());
			wavHeader.advance(samples.length << 1);

			try (OutputStream os = new FileOutputStream(wavFile)) {
				os.write(wavHeader.getBytes());

				for (short sample : samples) {
					if (!sampleBuffer.putShort(sample).hasRemaining()) {
						os.write(sampleBuffer.array(), 0, sampleBuffer.position());
						((Buffer) sampleBuffer).flip();
					}
				}
				os.write(sampleBuffer.array(), 0, sampleBuffer.position());
			}
			Process process = new ProcessBuilder("voice2json", "-p", "en", "transcribe-wav", "--open",
					wavFile.getAbsolutePath()).start();
			int waitFlag = process.waitFor();
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {
					response.setContentType(MIME_TYPE_JSON.toString());
					IOUtils.copy(process.getInputStream(), response.getOutputStream());
				} else {
					throw new IOException("Process failed with exit code: " + returnVal);
				}
			} else {
				throw new IOException("Process failed with waitFlag: " + waitFlag);
			}

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		} finally {
			if (wavFile != null) {
				wavFile.delete();
			}
		}
	}
}
