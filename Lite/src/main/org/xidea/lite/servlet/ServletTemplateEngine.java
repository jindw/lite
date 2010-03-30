package org.xidea.lite.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.json.JSONDecoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.impl.ParseConfigImpl;
import org.xidea.lite.parser.impl.HotTemplateEngine;

public class ServletTemplateEngine extends HotTemplateEngine {
	private static final Log log = LogFactory.getLog(ServletTemplateEngine.class);
	private ServletContext context;

	public ServletTemplateEngine(ServletConfig config) {
		super(new File(config.getServletContext().getRealPath("/")).toURI(),null);
		this.context = config.getServletContext();
		try {
			String configPath = config.getInitParameter("config");
			if(configPath==null){
				configPath = "/WEB-INF/lite.xml";
			}
			File file = new File(context.getRealPath(configPath));
			super.config = new ParseConfigImpl(file.toURI());
		} catch (Exception e) {
			log.error("装载页面装饰配置信息失败", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Template createTemplate(String path){
		if(config.isDebugModel()){
			return super.createTemplate(path);
		}else{
			try {
				File file = new File(context.getRealPath("/WEB-INF/litecached/"+path.replace('/', '^').replace('\\', '^')));
				List<Object> list = JSONDecoder.decode(loadText(file));
				return new Template((List<Object>)list.get(1));
			} catch (IOException e) {
				log.error(e);
				throw new RuntimeException(e);
			}
		}
	}

	private String loadText(File file) throws IOException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(file),"utf-8");
		StringBuilder buf = new StringBuilder();
		char[] cbuf = new char[1024];
		int c;
		while((c = in.read(cbuf))>=0){
			buf.append(cbuf, 0, c);
		}
		return buf.toString();
	}


	
}
