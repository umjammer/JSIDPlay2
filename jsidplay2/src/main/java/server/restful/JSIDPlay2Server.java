package server.restful;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletRequest.BASIC_AUTH;
import static org.apache.catalina.startup.Tomcat.addServlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.net.Constants;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.sidtune.SidTuneError;
import libsidutils.DebugUtil;
import server.restful.common.Connectors;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.servlets.ConvertServlet;
import server.restful.servlets.DirectoryServlet;
import server.restful.servlets.DownloadServlet;
import server.restful.servlets.FavoritesServlet;
import server.restful.servlets.FiltersServlet;
import server.restful.servlets.PhotoServlet;
import server.restful.servlets.StartPageServlet;
import server.restful.servlets.StaticServlet;
import server.restful.servlets.TuneInfoServlet;
import server.restful.servlets.whatssid.FindHashServlet;
import server.restful.servlets.whatssid.FindTuneServlet;
import server.restful.servlets.whatssid.InsertHashesServlet;
import server.restful.servlets.whatssid.InsertTuneServlet;
import server.restful.servlets.whatssid.TuneExistsServlet;
import server.restful.servlets.whatssid.WhatsSidServlet;
import sidplay.Player;
import ui.entities.PersistenceProperties;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;
import ui.entities.whatssid.service.WhatsSidService;

@Parameters(resourceBundle = "server.restful.JSIDPlay2Server")
public class JSIDPlay2Server {

	static {
		DebugUtil.init();
	}

	/**
	 * Context root of start page
	 */
	public static final String CONTEXT_ROOT = "";

	/**
	 * Context root of all servlets
	 */
	public static final String CONTEXT_ROOT_SERVLET = "/jsidplay2service/JSIDPlay2REST";

	/**
	 * Context root of static pages
	 */
	public static final String CONTEXT_ROOT_STATIC = "/static";

	/**
	 * User role
	 */
	private static final String ROLE_USER = "user";

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
			DirectoryServlet.class, TuneInfoServlet.class, PhotoServlet.class, ConvertServlet.class,
			DownloadServlet.class, FavoritesServlet.class, StaticServlet.class, StartPageServlet.class,
			InsertTuneServlet.class, InsertHashesServlet.class, FindTuneServlet.class, FindHashServlet.class,
			WhatsSidServlet.class, TuneExistsServlet.class);

	public static WhatsSidService whatsSidService;

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--whatsSidDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", required = true)
	private String whatsSidDatabaseDriver;

	@Parameter(names = { "--whatsSidDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", required = true)
	private String whatsSidDatabaseUrl;

	@Parameter(names = { "--whatsSidDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", required = true)
	private String whatsSidDatabaseUsername;

	@Parameter(names = { "--whatsSidDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", required = true)
	private String whatsSidDatabasePassword;

	@Parameter(names = { "--whatsSidDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", required = true)
	private String whatsSidDatabaseDialect;

	@ParametersDelegate
	private Configuration configuration;

	private Tomcat tomcat;

	private Properties servletUtilProperties;

	private static JSIDPlay2Server instance;

	public static JSIDPlay2Server getInstance(Configuration configuration) {
		if (instance == null) {
			instance = new JSIDPlay2Server(configuration);
		}
		return instance;
	}

	private JSIDPlay2Server(Configuration configuration) {
		this.configuration = configuration;
		this.servletUtilProperties = getServletUtilProperties();
		Player.initializeTmpDir(configuration);
	}

	public synchronized void start() throws Exception {
		if (tomcat == null) {
			tomcat = createTomcat();
			tomcat.start();
		}
	}

	public synchronized void stop() throws Exception {
		if (tomcat != null && tomcat.getServer().getState() != LifecycleState.STOPPING_PREP
				&& tomcat.getServer().getState() != LifecycleState.STOPPING
				&& tomcat.getServer().getState() != LifecycleState.STOPPED
				&& tomcat.getServer().getState() != LifecycleState.DESTROYING
				&& tomcat.getServer().getState() != LifecycleState.DESTROYED) {
			try {
				try {
					tomcat.stop();
					tomcat.getServer().await();
				} catch (Throwable e) {
					// workaround java11 problem
				}
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

	private Tomcat createTomcat() throws Exception {
		SidPlay2Section sidplay2Section = configuration.getSidplay2Section();
		EmulationSection emulationSection = configuration.getEmulationSection();

		Tomcat tomcat = new Tomcat();
		tomcat.setAddDefaultWebXmlToWebapp(false);
		tomcat.setBaseDir(sidplay2Section.getTmpDir());

		setRealm(tomcat);
		setConnectors(tomcat, emulationSection);

		Context context = addWebApp(tomcat, sidplay2Section);

		addSecurityConstraints(context);
		addServlets(context);

		return tomcat;
	}

	private void setRealm(Tomcat tomcat) {
		MemoryRealm realm = new MemoryRealm();
		realm.setPathname(getRealmConfigURL().toExternalForm());
		tomcat.getEngine().setRealm(realm);
	}

	/**
	 * Search for user, password and role configuration file.<BR>
	 * <B>Note:</B>If no configuration file is found internal configuration is used
	 * 
	 * @return user, password and role configuration file
	 */
	private URL getRealmConfigURL() {
		for (String dir : new String[] { System.getProperty("user.dir"), System.getProperty("user.home") }) {
			File configPlace = new File(dir, REALM_CONFIG);
			if (configPlace.exists()) {
				try {
					return configPlace.toURI().toURL();
				} catch (MalformedURLException e) {
					throw new Error(e); // Can't happen
				}
			}
		}
		// built-in default configuration
		return INTERNAL_REALM_CONFIG;
	}

	private void setConnectors(Tomcat tomcat, EmulationSection emulationSection) {
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
		Connector httpConnector = new Connector(Http11NioProtocol.class.getName());
		httpConnector.setURIEncoding(UTF_8.name());
		httpConnector.setScheme(Connectors.HTTP.getPreferredProtocol());

		Http11NioProtocol protocol = (Http11NioProtocol) httpConnector.getProtocolHandler();
		protocol.setPort(emulationSection.getAppServerPort());
		return httpConnector;
	}

	private Connector createHttpsConnector(EmulationSection emulationSection) {
		Connector httpsConnector = new Connector(Http11NioProtocol.class.getName());
		httpsConnector.setURIEncoding(UTF_8.name());
		httpsConnector.setScheme(Connectors.HTTPS.getPreferredProtocol());

		Http11NioProtocol protocol = (Http11NioProtocol) httpsConnector.getProtocolHandler();
		protocol.setPort(emulationSection.getAppServerSecurePort());
		protocol.setSecure(true);
		protocol.setSSLEnabled(true);
		protocol.setSslProtocol(Constants.SSL_PROTO_TLSv1_2);

		asList(protocol.findSslHostConfigs()).stream().findFirst().ifPresent(sslHostConfig -> {
			SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.RSA);
			certificate.setCertificateKeystoreType(KeyStore.getDefaultType());
			certificate.setCertificateKeystoreFile(emulationSection.getAppServerKeystoreFile());
			certificate.setCertificateKeystorePassword(emulationSection.getAppServerKeystorePassword());
			certificate.setCertificateKeyAlias(emulationSection.getAppServerKeyAlias());
			certificate.setCertificateKeyPassword(emulationSection.getAppServerKeyPassword());
			sslHostConfig.addCertificate(certificate);
		});
		return httpsConnector;
	}

	/**
	 * <b>Note:</b> Base directory of the context root is .jsidplay2
	 */
	private Context addWebApp(Tomcat tomcat, SidPlay2Section sidplay2Section) {
		Context context = tomcat.addWebapp(tomcat.getHost(), CONTEXT_ROOT, sidplay2Section.getTmpDir());
		context.addSecurityRole(ROLE_ADMIN);
		context.addSecurityRole(ROLE_USER);

		StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
		jarScanner.setScanManifest(false);
		jarScanner.setScanClassPath(false);
		jarScanner.setScanAllDirectories(false);

		StandardJarScanFilter jarScanFilter = (StandardJarScanFilter) jarScanner.getJarScanFilter();
		jarScanFilter.setTldSkip("*");
		jarScanFilter.setDefaultTldScan(false);
		jarScanFilter.setDefaultPluggabilityScan(false);
		return context;
	}

	private void addSecurityConstraints(Context context) {
		context.setLoginConfig(new LoginConfig(BASIC_AUTH, null, null, null));

		SecurityConstraint constraint = new SecurityConstraint();
		constraint.addAuthRole(ROLE_ADMIN);
		constraint.addAuthRole(ROLE_USER);
		constraint.setAuthConstraint(true);

		SecurityCollection collection = new SecurityCollection();
		collection.addPattern(CONTEXT_ROOT_SERVLET + "/*");
		constraint.addCollection(collection);
		context.addConstraint(constraint);
	}

	private void addServlets(Context context) throws Exception {
		for (Class<? extends JSIDPlay2Servlet> servletCls : SERVLETS) {
			JSIDPlay2Servlet servlet = (JSIDPlay2Servlet) servletCls
					.getDeclaredConstructor(Configuration.class, Properties.class)
					.newInstance(configuration, servletUtilProperties);
			addServlet(context, servletCls.getSimpleName(), servlet).addMapping(servlet.getServletPath() + "/*");
		}
	}

	private HashMap<String, String> createDatabaseMap() {
		HashMap<String, String> result = new HashMap<>();
		result.put("hibernate.connection.driver_class", whatsSidDatabaseDriver);
		result.put("hibernate.connection.url", whatsSidDatabaseUrl);
		result.put("hibernate.connection.username", whatsSidDatabaseUsername);
		result.put("hibernate.connection.password", whatsSidDatabasePassword);
		result.put("hibernate.dialect", whatsSidDatabaseDialect);
		return result;
	}

	private static void exit(int rc) {
		if (whatsSidService != null) {
			whatsSidService.close();
		}
		try {
			System.out.println("Press <enter> to exit the player!");
			System.in.read();
			System.exit(rc);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			Configuration configuration = new ConfigService(ConfigurationType.XML).load();

			JSIDPlay2Server jsidplay2Server = getInstance(configuration);
			JCommander commander = JCommander.newBuilder().addObject(jsidplay2Server)
					.programName(JSIDPlay2Server.class.getName()).build();
			commander.parse(args);
			if (jsidplay2Server.help) {
				commander.usage();
				exit(0);
			}
			EntityManager em = Persistence
					.createEntityManagerFactory(PersistenceProperties.WHATSSID_DS, jsidplay2Server.createDatabaseMap())
					.createEntityManager();
			whatsSidService = new WhatsSidService(em);

			jsidplay2Server.start();
		} catch (ParameterException | IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			exit(1);
		}
	}

}
