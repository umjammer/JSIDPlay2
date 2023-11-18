package server.restful.common;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

public class ServletUtil {

	public static boolean isSecured(ServletSecurity servletSecurity) {
		HttpConstraint httpConstraint = servletSecurity != null ? servletSecurity.value() : null;

		return httpConstraint != null
				&& (httpConstraint.rolesAllowed().length > 0 || EmptyRoleSemantic.DENY.equals(httpConstraint.value()));
	}

}
