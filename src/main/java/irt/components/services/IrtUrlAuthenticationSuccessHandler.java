package irt.components.services;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class IrtUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private final static Logger logger = LogManager.getLogger();

	 private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	 @Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

		 String redirectTo = request.getHeader("Referer");

		 Optional.ofNullable(redirectTo)
		 .filter(r->!r.endsWith("login"))
		 .map(
				 r->{
					 try {
//						 logger.error("redirectTo: {}", r);

						 redirectStrategy.sendRedirect(request, response, r);

						 return true;

					 } catch (IOException e) {
						 logger.catching(e);
					 }
					return false;
				 }).filter(b->b)
		 .orElseGet(
				()->{
					 try {

						 redirectStrategy.sendRedirect(request, response, "/");

					 } catch (IOException e) {
						 logger.catching(e);
						}
					return false;
				 });
	 }
}
