<%@ page contentType="text/html;charset=utf-8" pageEncoding="utf-8"
 isELIgnored="false"%><%
 
 		String code = new String(request.getParameter("code").getBytes("ISO-8859-1"), "utf-8");
		String model = new String(request.getParameter("model").getBytes("ISO-8859-1"), "utf-8");
		String callback = request.getParameter("callback");
		java.util.List<Object> litecode = org.xidea.el.json.JSONDecoder.decode(code);
		java.util.Map<String,Object> litemodel = org.xidea.el.json.JSONDecoder.decode(model);
		java.util.Map<String,String> featureMap = org.xidea.el.json.JSONDecoder.decode(
			"{'http://www.xidea.org/lite/features/content-type':'text/html;charset=utf-8'"
			+",'http://www.xidea.org/lite/features/encoding':'utf-8'}");
		org.xidea.lite.LiteTemplate tpl = new org.xidea.lite.LiteTemplate(litecode,featureMap);
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