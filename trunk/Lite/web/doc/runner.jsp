<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8"
 isELIgnored="false"%><%
 
 		String code = request.getParameter("code");
		String model = request.getParameter("model");
		String callback = request.getParameter("callback");
		java.util.List<Object> litecode = org.xidea.el.json.JSONDecoder.decode(code);
		java.util.Map<String,Object> litemodel = org.xidea.el.json.JSONDecoder.decode(model);
		org.xidea.lite.Template tpl = new org.xidea.lite.Template(litecode);
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
 
 %>