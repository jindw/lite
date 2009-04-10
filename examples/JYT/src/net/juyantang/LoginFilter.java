package net.juyantang;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

public class LoginFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
        UserService userService = UserServiceFactory.getUserService();

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String thisURL = request.getRequestURI();
        if (request.getUserPrincipal() == null) {
        	response.sendRedirect(userService.createLoginURL(thisURL));
        } else {
        	request.setAttribute("username", request.getUserPrincipal());
        	request.setAttribute("logout", userService.createLogoutURL(thisURL));
        	chain.doFilter(req, resp);
        }

	}

	@Override
	public void init(FilterConfig config) throws ServletException {

	}

}
