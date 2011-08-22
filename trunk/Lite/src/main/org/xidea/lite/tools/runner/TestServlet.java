package org.xidea.lite.tools.runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jside.webserver.action.CGIAdaptor;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;

@SuppressWarnings("serial")
public class TestServlet extends GenericServlet {

	/**
	 * type=java&code=[...]&model={..}
	 * type=php&code=[...]&model={..}
	 */
	@Override
	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		String type = req.getParameter("type");
		String code = req.getParameter("code");
		String model = req.getParameter("model");
		resp.setContentType("text/html;charset=utf-8");
		PrintWriter out = resp.getWriter();
		if("java".equals(type)){
			List<Object> litecode = JSONDecoder.decode(code);
			Map<String,Object> litemodel = JSONDecoder.decode(model);
			Template tpl = new Template(litecode);
			tpl.render(litemodel, out);
		}else if("php".equals(type)){
		}else{
			//
		}
		
	}

}
