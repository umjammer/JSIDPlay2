package server.restful;

import static jakarta.servlet.http.HttpServletRequest.BASIC_AUTH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.catalina.startup.Tomcat.addServlet;
import static server.restful.common.IServletSystemProperties.COMPRESSION;
import static server.restful.common.IServletSystemProperties.CONNECTION_TIMEOUT;
import static server.restful.common.IServletSystemProperties.HTTP2_KEEP_ALIVE_TIMEOUT;
import static server.restful.common.IServletSystemProperties.HTTP2_OVERHEAD_COUNT_FACTOR;
import static server.restful.common.IServletSystemProperties.HTTP2_OVERHEAD_DATA_THRESHOLD;
import static server.restful.common.IServletSystemProperties.HTTP2_OVERHEAD_WINDOW_UPDATE_THRESHOLD;
import static server.restful.common.IServletSystemProperties.HTTP2_READ_TIMEOUT;
import static server.restful.common.IServletSystemProperties.HTTP2_USE_SENDFILE;
import static server.restful.common.IServletSystemProperties.HTTP2_WRITE_TIMEOUT;
import static server.restful.common.IServletSystemProperties.USE_HTTP2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import server.restful.common.Connectors;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.PlayerCleanupTimerTask;
import server.restful.servlets.ConvertServlet;
import server.restful.servlets.DirectoryServlet;
import server.restful.servlets.DiskDirectoryServlet;
import server.restful.servlets.DownloadServlet;
import server.restful.servlets.FavoritesNamesServlet;
import server.restful.servlets.FavoritesServlet;
import server.restful.servlets.FiltersServlet;
import server.restful.servlets.PhotoServlet;
import server.restful.servlets.RandomHVSCServlet;
import server.restful.servlets.STILServlet;
import server.restful.servlets.SpeechToTextServlet;
import server.restful.servlets.StartPageServlet;
import server.restful.servlets.StaticServlet;
import server.restful.servlets.TuneInfoServlet;
import server.restful.servlets.WebJarsServlet;
import server.restful.servlets.hls.OnKeepAliveServlet;
import server.restful.servlets.hls.ProxyServlet;
import server.restful.servlets.rtmp.InsertNextCartServlet;
import server.restful.servlets.rtmp.InsertNextDiskServlet;
import server.restful.servlets.rtmp.JoystickServlet;
import server.restful.servlets.rtmp.OnPlayDoneServlet;
import server.restful.servlets.rtmp.OnPlayServlet;
import server.restful.servlets.rtmp.PressKeyServlet;
import server.restful.servlets.rtmp.SetDefaultEmulationReSidFpServlet;
import server.restful.servlets.rtmp.SetDefaultEmulationReSidServlet;
import server.restful.servlets.rtmp.SetSidModel6581Servlet;
import server.restful.servlets.rtmp.SetSidModel8580Servlet;
import server.restful.servlets.sidmapping.ExSIDMappingServlet;
import server.restful.servlets.sidmapping.HardSIDMappingServlet;
import server.restful.servlets.sidmapping.SIDBlasterMappingServlet;
import server.restful.servlets.whatssid.FindHashServlet;
import server.restful.servlets.whatssid.FindTuneServlet;
import server.restful.servlets.whatssid.InsertHashesServlet;
import server.restful.servlets.whatssid.InsertTuneServlet;
import server.restful.servlets.whatssid.TuneExistsServlet;
import server.restful.servlets.whatssid.WhatsSidServlet;
import sidplay.Player;
import sidplay.player.DebugUtil;
import ui.entities.PersistenceProperties;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

/**
 * 
 * Use this class to start JSIDPlay2 in server mode!
 * 
 * Server part of JSIDPlay2 to answer server requests like:
 * 
 * 1st) get a stream with SID music as MP3 for the mobile version or
 * 
 * 2nd) get a stream of a C64 demo as RTMP stream for the mobile version or
 * 
 * 3rd) WhatsSID? Which tune is currently played?
 * 
 * @author ken
 *
 */
public final class JSIDPlay2Server {

	static {
		DebugUtil.init();
	}

	@Parameters(resourceBundle = "server.restful.JSIDPlay2ServerParameters")
	public static class JSIDPlay2ServerParameters {

		@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true, order = 10000)
		private Boolean help = Boolean.FALSE;

		@Parameter(names = { "--whatsSIDDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", order = 10001)
		private String whatsSidDatabaseDriver;

		@Parameter(names = { "--whatsSIDDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", order = 10002)
		private String whatsSidDatabaseUrl;

		@Parameter(names = {
				"--whatsSIDDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", order = 10003)
		private String whatsSidDatabaseUsername;

		@Parameter(names = {
				"--whatsSIDDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", order = 10004)
		private String whatsSidDatabasePassword;

		@Parameter(names = { "--whatsSIDDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", order = 10005)
		private String whatsSidDatabaseDialect;

		@ParametersDelegate
		private Configuration configuration;

	}

	/**
	 * Context root of web app
	 */
	public static final String CONTEXT_ROOT = "";

	/**
	 * Context root of start page
	 */
	public static final String CONTEXT_ROOT_START_PAGE = "/";

	/**
	 * Context root of static pages
	 */
	public static final String CONTEXT_ROOT_STATIC = "/static";

	/**
	 * Context root of webjars
	 */
	public static final String CONTEXT_ROOT_WEBJARS = "/webjars";

	/**
	 * Context root of all servlets
	 */
	public static final String CONTEXT_ROOT_SERVLET = "/jsidplay2service/JSIDPlay2REST";

	/**
	 * User role
	 */
	public static final String ROLE_USER = "user";

	/**
	 * Admin role
	 */
	public static final String ROLE_ADMIN = "admin";

	/**
	 * Filename of the configuration file to access additional directories.
	 *
	 * e.g. "/MP3=/media/nas1/mp3,true" (top-level logical directory name=real
	 * directory name, admin role required?)
	 */
	public static final String SERVLET_UTIL_CONFIG_FILE = "directoryServlet.properties";

	/**
	 * Realm name
	 */
	public static final String REALM_NAME = "jsidplay2-realm";

	/**
	 * Filename of the configuration containing username, password and role. For an
	 * example please refer to the internal resource tomcat-users.xml
	 */
	public static final String REALM_CONFIG = "tomcat-users.xml";

	/**
	 * Configuration of usernames, passwords and roles
	 */
	private static final URL INTERNAL_REALM_CONFIG = JSIDPlay2Server.class.getResource("/" + REALM_CONFIG);

	/**
	 * Our servlets to serve
	 */
	private static final List<Class<? extends JSIDPlay2Servlet>> SERVLETS = asList(FiltersServlet.class,
			DirectoryServlet.class, DiskDirectoryServlet.class, TuneInfoServlet.class, PhotoServlet.class,
			ConvertServlet.class, DownloadServlet.class, FavoritesServlet.class, FavoritesNamesServlet.class,
			WebJarsServlet.class, StaticServlet.class, StartPageServlet.class, InsertTuneServlet.class,
			InsertHashesServlet.class, FindTuneServlet.class, FindHashServlet.class, WhatsSidServlet.class,
			TuneExistsServlet.class, OnKeepAliveServlet.class, OnPlayDoneServlet.class, OnPlayServlet.class,
			InsertNextDiskServlet.class, InsertNextCartServlet.class, SetSidModel6581Servlet.class,
			SetSidModel8580Servlet.class, SetDefaultEmulationReSidServlet.class,
			SetDefaultEmulationReSidFpServlet.class, PressKeyServlet.class, JoystickServlet.class, ProxyServlet.class,
			STILServlet.class, HardSIDMappingServlet.class, ExSIDMappingServlet.class, SIDBlasterMappingServlet.class,
			RandomHVSCServlet.class, SpeechToTextServlet.class);

	private static JSIDPlay2Server INSTANCE;
	
	private static EntityManagerFactory ENTITY_MANAGER_FACTORY;

	private static final ThreadLocal<EntityManager> THREAD_LOCAL_ENTITY_MANAGER = new ThreadLocal<>();

	private static final ConfigurationType CONFIGURATION_TYPE = ConfigurationType.XML;

	private JSIDPlay2ServerParameters parameters = new JSIDPlay2ServerParameters();

	private Tomcat tomcat;

	private Properties servletUtilProperties;

	private SidDatabase sidDatabase;

	private STIL stil;

	private JSIDPlay2Server() {
	}

	public static synchronized JSIDPlay2Server getInstance(Configuration configuration) {
		if (INSTANCE == null) {
			INSTANCE = create(configuration);
		}
		return INSTANCE;
	}

	private static JSIDPlay2Server create(Configuration configuration) {
		JSIDPlay2Server result = new JSIDPlay2Server();
		result.parameters.configuration = configuration;
		result.servletUtilProperties = result.getServletUtilProperties();
		File hvsc = configuration.getSidplay2Section().getHvsc();
		if (hvsc != null) {
			try {
				result.sidDatabase = new SidDatabase(hvsc);
				result.stil = new STIL(hvsc);
			} catch (IOException | NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		Player.initializeTmpDir(configuration);
		return result;
	}

	public synchronized void start()
			throws MalformedURLException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, LifecycleException {

		if (tomcat == null) {
			tomcat = createTomcat();
			tomcat.start();
		}
	}

	public synchronized void stop() throws LifecycleException {
		if (tomcat != null && tomcat.getServer().getState() != LifecycleState.STOPPING_PREP
				&& tomcat.getServer().getState() != LifecycleState.STOPPING
				&& tomcat.getServer().getState() != LifecycleState.STOPPED
				&& tomcat.getServer().getState() != LifecycleState.DESTROYING
				&& tomcat.getServer().getState() != LifecycleState.DESTROYED) {
			try {
				tomcat.stop();
				tomcat.getServer().await();
				tomcat.getServer().destroy();
			} finally {
				tomcat = null;
			}
		}
	}

	/**
	 * Search for configuration of additional accessible directories. Search in CWD
	 * and in the HOME folder.
	 */
	private Properties getServletUtilProperties() {
		Properties result = new Properties();
		for (String dir : new String[] { System.getProperty("user.dir"), System.getProperty("user.home") }) {
			try (InputStream is = new FileInputStream(new File(dir, SERVLET_UTIL_CONFIG_FILE))) {
				result.load(is);
			} catch (IOException e) {
				// ignore non-existing properties
			}
		}
		return result;
	}

	private Tomcat createTomcat() throws MalformedURLException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(parameters.configuration.getSidplay2Section().getTmpDir().getAbsolutePath());

		setRealm(tomcat);
		setConnectors(tomcat);

		Context context = addContext(tomcat);

		List<JSIDPlay2Servlet> servlets = addServlets(context);

		addServletFilters(context, servlets);
		addSecurity(context, servlets);

		new Timer().schedule(new PlayerCleanupTimerTask(context), 0, 1000L);

		return tomcat;
	}

	private void setRealm(Tomcat tomcat) throws MalformedURLException {
		MemoryRealm realm = new MemoryRealm();
		realm.setPathname(getRealmConfigURL().toExternalForm());

		tomcat.getEngine().setRealm(realm);
	}

	/**
	 * Search for user, password and role configuration file.<BR>
	 * <B>Note:</B>If no configuration file is found internal configuration is used
	 *
	 * @return user, password and role configuration file
	 * @throws MalformedURLException error locating the realm configuration
	 */
	private URL getRealmConfigURL() throws MalformedURLException {
		for (String dir : new String[] { System.getProperty("user.dir"), System.getProperty("user.home") }) {
			File configPlace = new File(dir, REALM_CONFIG);
			if (configPlace.exists()) {
				return configPlace.toURI().toURL();
			}
		}
		// built-in default configuration
		return INTERNAL_REALM_CONFIG;
	}

	private void setConnectors(Tomcat tomcat) {
		EmulationSection emulationSection = parameters.configuration.getEmulationSection();

		switch (emulationSection.getAppServerConnectors()) {
		case HTTP_HTTPS: {
			tomcat.setConnector(createHttpConnector(emulationSection));
			tomcat.setConnector(createHttpsConnector(emulationSection));
			break;
		}
		case HTTPS: {
			tomcat.setConnector(createHttpsConnector(emulationSection));
			break;
		}
		case HTTP:
		default: {
			tomcat.setConnector(createHttpConnector(emulationSection));
			break;
		}
		}
	}

	private Connector createHttpConnector(EmulationSection emulationSection) {
		Connector httpConnector = new Connector(Http11Nio2Protocol.class.getName());
		httpConnector.setURIEncoding(UTF_8.name());
		httpConnector.setScheme(Connectors.HTTP.getPreferredProtocol());
		httpConnector.setPort(emulationSection.getAppServerPort());

		Http11Nio2Protocol protocol = (Http11Nio2Protocol) httpConnector.getProtocolHandler();
		protocol.setConnectionTimeout(CONNECTION_TIMEOUT);
		protocol.setCompression(COMPRESSION);

		return httpConnector;
	}

	private Connector createHttpsConnector(EmulationSection emulationSection) {
		Connector httpsConnector = new Connector(Http11Nio2Protocol.class.getName());
		httpsConnector.setURIEncoding(UTF_8.name());
		httpsConnector.setScheme(Connectors.HTTPS.getPreferredProtocol());
		httpsConnector.setPort(emulationSection.getAppServerSecurePort());
		httpsConnector.setSecure(true);
		if (USE_HTTP2) {
			Http2Protocol h2 = new Http2Protocol();
			h2.setReadTimeout(HTTP2_READ_TIMEOUT);
			h2.setWriteTimeout(HTTP2_WRITE_TIMEOUT);
			h2.setKeepAliveTimeout(HTTP2_KEEP_ALIVE_TIMEOUT);
			h2.setUseSendfile(HTTP2_USE_SENDFILE);
			h2.setOverheadCountFactor(HTTP2_OVERHEAD_COUNT_FACTOR);
			h2.setOverheadDataThreshold(HTTP2_OVERHEAD_DATA_THRESHOLD);
			h2.setOverheadWindowUpdateThreshold(HTTP2_OVERHEAD_WINDOW_UPDATE_THRESHOLD);
			httpsConnector.addUpgradeProtocol(h2);
		}
		Http11Nio2Protocol protocol = (Http11Nio2Protocol) httpsConnector.getProtocolHandler();
		protocol.setConnectionTimeout(CONNECTION_TIMEOUT);
		protocol.setCompression(COMPRESSION);
		protocol.setSSLEnabled(true);

		SSLHostConfig sslHostConfig = new SSLHostConfig();

		SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.RSA);
		certificate.setCertificateKeystoreType(KeyStore.getDefaultType());
		certificate.setCertificateKeystoreFile(emulationSection.getAppServerKeystoreFile().getAbsolutePath());
		certificate.setCertificateKeystorePassword(emulationSection.getAppServerKeystorePassword());
		certificate.setCertificateKeyAlias(emulationSection.getAppServerKeyAlias());
		certificate.setCertificateKeyPassword(emulationSection.getAppServerKeyPassword());

		sslHostConfig.addCertificate(certificate);
		httpsConnector.addSslHostConfig(sslHostConfig);

		return httpsConnector;
	}

	private Context addContext(Tomcat tomcat) {
		return tomcat.addContext(tomcat.getHost(), CONTEXT_ROOT,
				tomcat.getServer().getCatalinaBase().getAbsolutePath());
	}

	private List<JSIDPlay2Servlet> addServlets(Context context) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		List<JSIDPlay2Servlet> result = new ArrayList<>();

		for (Class<? extends JSIDPlay2Servlet> servletCls : SERVLETS) {
			JSIDPlay2Servlet servlet = servletCls.getDeclaredConstructor().newInstance();
			servlet.setConfiguration(parameters.configuration);
			servlet.setDirectoryProperties(servletUtilProperties);
			servlet.setSidDatabase(sidDatabase);
			servlet.setStil(stil);

			WebServlet webServlet = servletCls.getAnnotation(WebServlet.class);
			Wrapper wrapper = addServlet(context, webServlet.name(), servlet);
			wrapper.setAsyncSupported(webServlet.asyncSupported());
			Stream.of(webServlet.urlPatterns()).forEach(wrapper::addMapping);

			result.add(servlet);
		}
		return result;
	}

	private void addServletFilters(Context context, List<JSIDPlay2Servlet> servlets) {
		servlets.forEach(servlet -> servlet.getServletFilters().forEach(servletFilter -> {
			WebServlet webServlet = servlet.getClass().getAnnotation(WebServlet.class);
			WebFilter webFilter = servletFilter.getClass().getAnnotation(WebFilter.class);

			String filterName = webServlet.name() + "_" + webFilter.filterName();

			FilterDef filterDefinition = new FilterDef();
			filterDefinition.setFilterName(filterName);
			filterDefinition.setFilter(servletFilter);
			filterDefinition.getParameterMap().putAll(servlet.getServletFiltersParameterMap());
			context.addFilterDef(filterDefinition);

			FilterMap filterMapping = new FilterMap();
			filterMapping.setFilterName(filterName);
			filterMapping.addServletName(webServlet.name());
			context.addFilterMap(filterMapping);
		}));
	}

	private void addSecurity(Context context, List<JSIDPlay2Servlet> servlets) {
		// roles must be defined before being used in a security constraint, therefore:
		context.addSecurityRole(ROLE_ADMIN);
		context.addSecurityRole(ROLE_USER);

		servlets.forEach(servlet -> {
			WebServlet webServlet = servlet.getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = servlet.getClass().getAnnotation(ServletSecurity.class);

			if (servlet.isSecured()) {
				SecurityCollection securityCollection = new SecurityCollection();
				Stream.of(webServlet.urlPatterns()).forEach(securityCollection::addPattern);

				SecurityConstraint securityConstraint = new SecurityConstraint();
				Stream.of(servletSecurity.value().rolesAllowed()).forEach(securityConstraint::addAuthRole);
				securityConstraint.setAuthConstraint(true);
				securityConstraint.addCollection(securityCollection);
				
				context.addConstraint(securityConstraint);
			}
		});
		context.getPipeline().addValve(new BasicAuthenticator());
		context.setLoginConfig(new LoginConfig(BASIC_AUTH, REALM_NAME, null, null));
	}

	private static void exit(int rc) {
		try {
			if (ENTITY_MANAGER_FACTORY != null && ENTITY_MANAGER_FACTORY.isOpen()) {
				ENTITY_MANAGER_FACTORY.close();
			}
			System.out.println("Press <enter> to exit the player!");
			System.in.read();
			System.exit(rc);
		} catch (IllegalStateException | IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		try {
			JSIDPlay2Server jsidplay2Server = getInstance(new ConfigService(CONFIGURATION_TYPE).load());
			JCommander commander = JCommander.newBuilder().addObject(jsidplay2Server.parameters)
					.programName(JSIDPlay2Server.class.getName()).build();
			commander.parse(args);
			if (jsidplay2Server.parameters.help) {
				commander.usage();
				exit(0);
			}
			if (jsidplay2Server.parameters.whatsSidDatabaseDriver != null && ENTITY_MANAGER_FACTORY == null) {
				ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory(PersistenceProperties.WHATSSID_DS,
						new PersistenceProperties(jsidplay2Server.parameters.whatsSidDatabaseDriver,
								jsidplay2Server.parameters.whatsSidDatabaseUrl,
								jsidplay2Server.parameters.whatsSidDatabaseUsername,
								jsidplay2Server.parameters.whatsSidDatabasePassword,
								jsidplay2Server.parameters.whatsSidDatabaseDialect));
			}
			jsidplay2Server.start();
		} catch (ParameterException | IOException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
				| LifecycleException e) {
			System.err.println(e.getMessage());
			exit(1);
		}
	}

	public static EntityManager getEntityManager() throws IOException {
		if (ENTITY_MANAGER_FACTORY == null) {
			throw new IOException("Database required, please specify command line parameters!");
		}
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em == null) {
			em = ENTITY_MANAGER_FACTORY.createEntityManager();
			THREAD_LOCAL_ENTITY_MANAGER.set(em);
		}
		return em;
	}

	public static void freeEntityManager() {
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em != null) {
			em.clear();
		}
	}
}
