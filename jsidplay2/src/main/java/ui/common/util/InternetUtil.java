package ui.common.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ui.entities.config.SidPlay2Section;

public class InternetUtil {

	static {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}

			} };
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static URLConnection openConnection(URL currentURL, SidPlay2Section sidplay2Section) throws IOException {
		while (true) {
			URLConnection openConnection = currentURL.openConnection(getProxy(sidplay2Section));
			if (openConnection instanceof HttpURLConnection) {
				HttpURLConnection connection = (HttpURLConnection) openConnection;
				connection.setInstanceFollowRedirects(false);
				int responseCode = connection.getResponseCode();

				switch (responseCode) {
				case HttpURLConnection.HTTP_MOVED_PERM:
				case HttpURLConnection.HTTP_MOVED_TEMP:
				case HttpURLConnection.HTTP_SEE_OTHER:
					String location = connection.getHeaderField("Location");
					if (location != null) {
						try {
							// absolute URI
							currentURL = new URL(location);
						} catch (MalformedURLException e) {
							try {
								// Deal with relative URLs where location must be encoded properly
								URI uri = new URI(currentURL.getProtocol(), currentURL.getUserInfo(),
										currentURL.getHost(), currentURL.getPort(), location, currentURL.getQuery(),
										currentURL.getRef());
								currentURL = new URL(uri.toASCIIString());
							} catch (URISyntaxException e2) {
								throw new IOException("Redirection failed for location: " + location);
							}
						}
						continue;
					}
				case HttpURLConnection.HTTP_OK:
					break;
				default:
					throw new IOException("Unexpected response: " + responseCode);
				}
			}
			return openConnection;
		}
	}

	private static Proxy getProxy(SidPlay2Section sidplay2Section) {
		if (sidplay2Section.isProxyEnable()) {
			return new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(sidplay2Section.getProxyHostname(), sidplay2Section.getProxyPort()));
		} else {
			return Proxy.NO_PROXY;
		}
	}

}
