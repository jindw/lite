package org.xidea.lite.test.oldcases;

//import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


@SuppressWarnings("serial")
class TestServlet extends GenericServlet {

	/**
	 * type=java&code=[...]&model={..}
	 * type=php&code=[...]&model={..}
	 */
	@Override
	public void service(ServletRequest request, ServletResponse resp)
			throws ServletException, java.io.IOException {
		resp.setContentType("text/html;charset=utf-8");
		java.io.PrintWriter out = resp.getWriter();
		
		String code = request.getParameter("code");
		String model = request.getParameter("model");
		String callback = request.getParameter("callback");
		java.util.List<Object> litecode = org.xidea.el.json.JSONDecoder.decode(code);
		java.util.Map<String,Object> litemodel = org.xidea.el.json.JSONDecoder.decode(model);
		org.xidea.lite.Template tpl = new org.xidea.lite.Template(litecode,null);
		java.io.StringWriter out2 = new java.io.StringWriter();
		tpl.render(litemodel, out2);
		if(callback == null){
			out.print(out2.toString());
		}else{;
			out.print(callback);
			out.print('(');
			out.print(org.xidea.el.json.JSONEncoder.encode(out2.toString()));
			out.print(')');
		}
		
	}

}
