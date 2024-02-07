package server.restful.common;

import static java.util.stream.Stream.of;
import static server.restful.common.IServletSystemProperties.UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

public class ServletUtil {

	public static boolean isSecured(ServletSecurity servletSecurity) {
		HttpConstraint httpConstraint = servletSecurity != null ? servletSecurity.value() : null;

		return httpConstraint != null
				&& (httpConstraint.rolesAllowed().length > 0 || EmptyRoleSemantic.DENY.equals(httpConstraint.value()));
	}

	public static void info(ServletContext servletContext, String msg, Thread... parentThreads) {
		servletContext.log(threads(parentThreads) + thread() + msg);
	}

	public static void warn(ServletContext servletContext, String msg, Thread... parentThreads) {
		servletContext.log(threads(parentThreads) + thread() + msg, null);
	}

	public static void error(ServletContext servletContext, Throwable t, Thread... parentThreads) {
		servletContext.log(threads(parentThreads) + thread() + t.getMessage(), t);
	}

	public static void uncaughtExceptionHandler(ServletContext servletContext, Throwable t, Thread thread,
			Thread... parentThreads) {
		servletContext.log(threads(parentThreads) + thread(thread) + t.getMessage(),
				UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS ? t : null);
	}

	public static StringBuilder threads(Thread... threads) {
		return of(threads).map(ServletUtil::thread).collect(StringBuilder::new, StringBuilder::append,
				StringBuilder::append);
	}

	public static String thread() {
		return thread(Thread.currentThread());
	}

	@SuppressWarnings("deprecation")
	private static String thread(Thread thread) {
		StringBuilder result = new StringBuilder();
		result.append(thread.getName());
		result.append(" (");
		result.append(thread.getId());
		result.append(")");
		result.append(": ");
		return result.toString();
	}

}
