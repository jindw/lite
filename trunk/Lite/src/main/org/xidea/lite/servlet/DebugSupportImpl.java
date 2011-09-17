package org.xidea.lite.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.jsi.JSIExportor;
import org.xidea.jsi.JSILoadContext;
import org.xidea.jsi.impl.ClasspathRoot;
import org.xidea.jsi.impl.DefaultExportorFactory;
import org.xidea.jsi.impl.JSIText;
import org.xidea.lite.Template;
import org.xidea.lite.impl.ParseUtil;

public class DebugSupportImpl implements DebugSupport {
	private TemplateServlet templateServlet;
	private JSONEncoder json;

	public DebugSupportImpl(TemplateServlet templateServlet) {
		this.templateServlet = templateServlet;
		json = new JSONEncoder(JSONEncoder.W3C_DATE_TIME_FORMAT, true, 32){
			Map<String, Method> fileGetterMap;
			{
				fileGetterMap = new HashMap<String, Method>( ReflectUtil.getGetterMap(File.class));
				fileGetterMap.keySet().retainAll(Arrays.asList("name","path","canonicalPath","absolutePath","absolute","hidden","file","directory"));
				
			}
			public Map<String, Method> getGetterMap(Class<?extends Object> clazz){
				if(clazz == File.class){
					return fileGetterMap;
				}
				return super.getGetterMap(clazz);
			}
		};
	}

	/**
	 * 
	 * @param servlet
	 * @param path
	 * @param request
	 * @param resp
	 * @return complete request
	 * @throws IOException
	 */
	public boolean debug(String path, HttpServletRequest request,
			HttpServletResponse resp) throws IOException {
		String debug = getCookie(request, LITE_DEBUG);
		if (debug != null) {
			return debugCookie(path, request, resp, debug);
		} else {
			return false;
		}
	}

	private String getCookie(HttpServletRequest request, String key) {
		for (Cookie cookie : request.getCookies()) {
			if (key.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private boolean debugCookie(String path, HttpServletRequest request,
			HttpServletResponse response, String value) throws IOException,
			FileNotFoundException {
		if ("refresh".equals(value)) {
			templateServlet.templateEngine.clear(path);
		} else if ("source".equals(value)) {
			File f = new File(templateServlet.getServletContext().getRealPath(
					path));
			response.setContentType("text/plain");
			ServletOutputStream out = response.getOutputStream();
			FileInputStream in = new FileInputStream(f);
			byte[] buf = new byte[64];
			for (int c = in.read(buf); c >= 0;c=in.read(buf)) {
				out.write(buf, 0, c);
			}
			in.close();
			out.flush();
			return true;
		}else if("model".equals(value)) {
			// 只支持数据预览，不支持数据修改
			// log.warn("该版本尚不支持数据模型展现");
			// templateEngine.clear(path);
			Map<String, Object> model = templateServlet.createModel(request);
			String result = jsonStringify(model);
			String features = "{}";
			response.setContentType("text/html;charset=utf-8");
			PrintWriter out = response.getWriter();
			String serviceBase = getServiceBase(request);
			out.append("<!DOCTYPE html><html><body>\n");
			out.append("<style>body,html{width:98%;height:100%}</style>\n");
			out.append("<script>");
			out.append(";\nvar templatePath = " + jsonStringify(path));
			out.append(";\nvar templateModel = " + result);
			out.append(";\nvar templateFeatureMap = " + features);
			out.append(";\nvar serviceBase='" + serviceBase + "';</script>\n");
			out.append("<script src='" + serviceBase + '?' + PARAM_LITE_ACTION
					+ "=load&" + PARAM_LITE_PATH + '=' + DATA_VIEW_JS_PATH
					+ "'></script>\n");
			out
					.append("<script>if(!this.DataView && this.$import){$import('org.xidea.lite.web.DataView',true);}</script>\n");
			
			String model_view_impl = getCookie(request, "LITE_MODEL_VIEW_IMPL");
			if(model_view_impl !=null){
				out.append("<script src='"+model_view_impl+"'></script>");
			}
			out.append("<script>DataView.render(templatePath,templateModel,templateFeatureMap,serviceBase);</script>\n");
			if(model_view_impl!=null){
				out.append("<div><a href='#' onclick=\"document.cookie='LITE_MODEL_VIEW_IMPL=;expires='+new Date(0).toGMTString();alert('恢复成功')\">回复默认视图</a></div>");
			}
			out.append("\n<hr><pre>");
			out.print(result.replace("&", "&amp;").replace("<", "&lt;"));
			out.append("</pre>");
			out.append("</pre></body></html>");
			out.flush();
			return true;
		} else if (value.startsWith("model")) {
			String dataURL = value.substring(6);
			InputStream in = new URL(dataURL).openStream();
			String json = ParseUtil.loadTextAndClose(in, "UTF-8");
			Map<String, Object> context = templateServlet.createModel(request);
			Map<String, Object> mock = JSONDecoder.decode(json);
			for(String key : mock.keySet()){
				context.put(key, mock.get(key));
			}
			Template template = templateServlet.templateEngine.getTemplate(path);
			String contentType = template.getContentType();
			response.setContentType(contentType);
			PrintWriter out = response.getWriter();
			template.render(context, out);
			out.flush();
			return true;
		}
		return false;
	}

	private String jsonStringify(Object model) throws IOException {
		StringBuilder buf = new StringBuilder(); 
		json.encode(model,buf);
		return buf.toString();
	}

	private String getServiceBase(HttpServletRequest request) {
		String serviceBase = getCookie(request, LITE_SERVICE);
		if (serviceBase == null) {
			serviceBase = templateServlet.getServletContext()
					.getContextPath()
					+ templateServlet.serviceBase;
		}
		return serviceBase;
	}

	public boolean service(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		final String liteAction = request.getParameter(PARAM_LITE_ACTION);
		final String litePath = request.getParameter(PARAM_LITE_PATH);
		final File root = new File(templateServlet.getServletContext().getRealPath("/"));
		if (PARAM_LITE_ACTION_LOAD.equals(liteAction)) {
			if (DATA_VIEW_JS_PATH.equals(litePath)) {
				response.setContentType("text/javascript;charset=utf-8");
				PrintWriter out = response.getWriter();
				File file = new File(templateServlet.getServletContext()
						.getRealPath(DATA_VIEW_JS_PATH));
				if (file.exists()) {
					out.write(ParseUtil.loadTextAndClose(new FileInputStream(
							file), null));
				} else {
					// "alert('找不到文件:+"+file+",请手动设置')";
					@SuppressWarnings("unchecked")
					JSIExportor exporter = DefaultExportorFactory.getInstance()
							.createExplorter(
									DefaultExportorFactory.TYPE_SIMPLE,
									Collections.EMPTY_MAP);
					
					//FileRoot jsiRoot = new FileRoot(new File(root,"WEB-INF/classes").getAbsolutePath(),"UTF-8");
					ClasspathRoot jsiRoot = new ClasspathRoot();
					JSILoadContext context = jsiRoot
							.$import("org.xidea.lite.web:DataView");
					String result = exporter.export(context);
					out.write(result);
				}
				out.flush();
			} else if(litePath.startsWith("/WEB-INF/litecode/") && litePath.endsWith(".json") && litePath.indexOf("..")<0){
				response.setContentType("text/javascript;charset=utf-8");
				PrintWriter out = response.getWriter();
				out.write(ParseUtil.loadTextAndClose(new FileInputStream(
						new File(root,litePath)), "utf-8"));
				out.flush();
			}
		}else if (PARAM_LITE_ACTION_SAVE.equals(liteAction)) {
			if(litePath.endsWith(".json")){
				String path = "/WEB-INF/litecode/"+litePath.replace(':', '^').replace('/', '^');
				final File dest = new File(root, path);
				String liteData = request.getParameter(PARAM_LITE_DATA);
				FileOutputStream fout = new FileOutputStream(dest);
				JSIText.writeBase64(liteData, fout);
				fout.flush();
				fout.close();

				response.setContentType("text/javascript;charset=utf-8");
				PrintWriter out = response.getWriter();
				//String serviceBase = getServiceBase(request);
				String pathJSON = jsonStringify(path);
				String callback = request.getParameter(PARAM_LITE_CALLBACK);
				String data = "{\"path\":"+pathJSON+"}";
				if(callback!=null){
					out.print(callback+'('+ data+",true)");
				}else{
					out.print( data);
				}
				out.flush();
			}else{
				
			}
		}
		return true;
	}

}
