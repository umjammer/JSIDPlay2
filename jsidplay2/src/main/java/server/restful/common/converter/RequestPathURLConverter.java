package server.restful.common.converter;

import java.net.URL;

import com.beust.jcommander.converters.URLConverter;

/**
 * URLs sent as a servlet request parameter are converted here.<BR>
 * <BR>
 * 
 * <pre>
 * e.g. the HTTP request with request path:
 * https://haendel.ddns.net:8443/jsidplay2service/JSIDPlay2REST/proxy<B>/http://haendel.ddns.net:90/hls/61f1b4f5-ce5c-4027-9bb8-5e1223069f2e-7.ts</B>
 * is received as:
 * /http:/haendel.ddns.net:90/hls/cdfbef9f-95b0-4fd2-9e7a-ee90f11f65fd.m3u8
 * and converted to a valid URL:
 * http://haendel.ddns.net:90/hls/cdfbef9f-95b0-4fd2-9e7a-ee90f11f65fd.m3u8
 * </pre>
 * 
 * @author ken
 *
 */
public final class RequestPathURLConverter extends URLConverter {

	public RequestPathURLConverter(String optionName) {
		super(optionName);
	}

	@Override
	public URL convert(String value) {
		if (value.startsWith("/")) {
			value = value.substring(1);
		}
		return super.convert(value.replaceFirst("/", "//"));
	}
}
