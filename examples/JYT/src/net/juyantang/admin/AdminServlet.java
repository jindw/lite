package net.juyantang.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.*;

import net.juyantang.PersistenceFacade;
import net.juyantang.po.Message;
import net.juyantang.po.MessageGroup;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		String path = getPath(req);
		
		if (path == null) {
			resp.setContentType("text/js");
			out.println("//Hello, world");
		} else {
			Message pathInfo = PersistenceFacade.getManager().getObjectById(
					Message.class, path);
			if (pathInfo == null) {
				String template = getTemplate(pathInfo);
				out.print("document.write(");
				out.print(template);
				out.print("(");
				out.print(pathInfo.getData());
				out.print("))");
			}
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		String path = getPath(req);
		if (path == null) {
			resp.setContentType("text/js");
			out.println("Hello, world");
		} else {
			Message pathInfo = PersistenceFacade.getManager().getObjectById(
					Message.class, path);
			if (pathInfo == null) {
				String template = getTemplate(pathInfo);
				out.print("document.write(");
				out.print(template);
				out.print("(");
				out.print(pathInfo.getData());
				out.print("))");
			}
		}
	}

	private String getPath(HttpServletRequest req) {
		String path = req.getParameter("path");
		if (path == null) {
			path = req.getHeader("Referer");
		}
		return path;
	}

	private String getTemplate(Message pathInfo) {
		MessageGroup group = pathInfo.getGroup();
		return group.getTemplate();
	}
}
